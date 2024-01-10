package node.codec

import cats.Monad
import cats.syntax.all.*
import dproc.data.Block
import sdk.api.data.{Balance, TokenTransferRequest}
import sdk.codecs.protobuf.ProtoCodec
import sdk.codecs.{Codec, PrimitiveReader, PrimitiveWriter, Serialize}
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.primitive.ByteArray
import weaver.data.ConflictResolution

import scala.math.Numeric.LongIsIntegral

// TODO should we use Magnolia or other derivation tool to not write binary protocols manually? Or macros?
object Serialization {

  implicit val balanceCodec: Codec[Balance, ByteArray] = ProtoCodec.apply[Balance](
    b => r => r.write(b.x),
    r => r.readLong.map(new Balance(_)),
  )

  implicit def balanceSerialize[F[_]: Monad]: Serialize[F, Balance] = new Serialize[F, Balance] {
    override def write(x: Balance): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) => w.write(x.x)
    override def read: PrimitiveReader[F] => F[Balance]           = (r: PrimitiveReader[F]) => r.readLong.map(new Balance(_))
  }

  implicit def balancesStateSerialize[F[_]: Monad]: Serialize[F, BalancesState] =
    new Serialize[F, BalancesState] {
      override def write(x: BalancesState): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesState(diffs) =>
            w.write(diffs.size) *>
              diffs.toList.sorted.traverse_ { case (k, v) => w.write(k.bytes) *> w.write(v) }
        }

      override def read: PrimitiveReader[F] => F[BalancesState] = (r: PrimitiveReader[F]) =>
        for {
          size  <- r.readInt
          diffs <- (0 until size).toList.traverse { _ =>
                     for {
                       k <- r.readBytes.map(ByteArray(_))
                       v <- r.readLong
                     } yield k -> v
                   }
        } yield BalancesState(diffs.toMap)
    }

  implicit def balancesDeployBodySerialize[F[_]: Monad]: Serialize[F, BalancesDeployBody] =
    new Serialize[F, BalancesDeployBody] {
      override def write(x: BalancesDeployBody): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesDeployBody(balanceState, vabn) =>
            balancesStateSerialize[F].write(balanceState)(w) *> w.write(vabn)
        }

      override def read: PrimitiveReader[F] => F[BalancesDeployBody] = (r: PrimitiveReader[F]) =>
        for {
          balanceState <- balancesStateSerialize[F].read(r)
          vabn         <- r.readLong
        } yield BalancesDeployBody(balanceState, vabn)
    }

  implicit def balancesDeploySerialize[F[_]: Monad]: Serialize[F, BalancesDeploy] =
    new Serialize[F, BalancesDeploy] {
      override def write(x: BalancesDeploy): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesDeploy(id, body) => w.write(id.bytes) *> balancesDeployBodySerialize[F].write(body)(w)
        }

      override def read: PrimitiveReader[F] => F[BalancesDeploy] = (r: PrimitiveReader[F]) =>
        for {
          id   <- r.readBytes
          body <- balancesDeployBodySerialize[F].read(r)
        } yield BalancesDeploy(ByteArray(id), body)
    }

  implicit def blockSerialize[F[_]: Monad]: Serialize[F, Block[ByteArray, ByteArray, BalancesDeploy]] =
    new Serialize[F, Block[ByteArray, ByteArray, BalancesDeploy]] {
      override def write(x: Block[ByteArray, ByteArray, BalancesDeploy]): PrimitiveWriter[F] => F[Unit] =
        (w: PrimitiveWriter[F]) =>
          x match {
            case Block(
                  sender,
                  minGenJs,
                  offences,
                  txs,
                  finalFringe,
                  finalized,
                  merge,
                  bonds,
                  lazTol,
                  expThresh,
                  finalStateHash,
                  postStateHash,
                ) =>
              w.write(sender.bytes) *>
                minGenJs.toList.sorted.traverse_(x => w.write(x.bytes)) *>
                offences.toList.sorted.traverse_(x => w.write(x.bytes)) *>
                txs.sorted.traverse_(x => balancesDeploySerialize.write(x)(w)) *>
                finalFringe.toList.sorted.traverse_(x => w.write(x.bytes)) *>
                finalized.traverse_ { case ConflictResolution(accepted, rejected) =>
                  accepted.toList.sorted.traverse_(x => balancesDeploySerialize.write(x)(w)) *>
                    rejected.toList.sorted.traverse_(x => balancesDeploySerialize.write(x)(w))
                } *>
                merge.toList.sorted.traverse_(x => balancesDeploySerialize.write(x)(w)) *>
                bonds.bonds.toList.sorted.traverse_ { case (k, v) => w.write(k.bytes) *> w.write(v) } *>
                w.write(lazTol) *>
                w.write(expThresh) *>
                w.write(finalStateHash) *>
                w.write(postStateHash)
          }

      override def read: PrimitiveReader[F] => F[Block[ByteArray, ByteArray, BalancesDeploy]] =
        ??? // not required for now
    }

  implicit def tokenTransferRequestBodySerialize[F[_]: Monad]: Serialize[F, TokenTransferRequest.Body] =
    new Serialize[F, TokenTransferRequest.Body] {
      override def write(x: TokenTransferRequest.Body): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case TokenTransferRequest.Body(sender, recipient, tokenId, amount, vafn) =>
            w.write(sender) *>
              w.write(recipient) *>
              w.write(tokenId) *>
              w.write(amount) *>
              w.write(vafn)
        }

      override def read: PrimitiveReader[F] => F[TokenTransferRequest.Body] = (r: PrimitiveReader[F]) =>
        for {
          sender    <- r.readBytes
          recipient <- r.readBytes
          tokenId   <- r.readLong
          amount    <- r.readLong
          vafn      <- r.readLong
        } yield TokenTransferRequest.Body(sender, recipient, tokenId, amount, vafn)
    }

  implicit def tokenTransferRequestSerialize[F[_]: Monad]: Serialize[F, TokenTransferRequest] =
    new Serialize[F, TokenTransferRequest] {
      override def write(x: TokenTransferRequest): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case TokenTransferRequest(pubKey, digest, signature, signatureAlg, body) =>
            w.write(pubKey) *>
              w.write(digest) *>
              w.write(signature) *>
              w.write(signatureAlg) *>
              tokenTransferRequestBodySerialize[F].write(body)(w)
        }

      override def read: PrimitiveReader[F] => F[TokenTransferRequest] = (r: PrimitiveReader[F]) =>
        for {
          pubKey       <- r.readBytes
          digest       <- r.readBytes
          signature    <- r.readBytes
          signatureAlg <- r.readString
          body         <- tokenTransferRequestBodySerialize[F].read(r)
        } yield TokenTransferRequest(pubKey, digest, signature, signatureAlg, body)
    }
}
