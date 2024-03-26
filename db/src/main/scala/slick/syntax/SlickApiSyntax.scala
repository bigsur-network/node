package slick.syntax

import sdk.codecs.Digest
import sdk.primitive.ByteArray
import sdk.syntax.all.digestSyntax
import slick.api.SlickApi

trait SlickApiSyntax {
  implicit final def slickApiSyntax[F[_]](
    store: SlickApi[F],
  ): SlickApiOps[F] = new SlickApiOps[F](store)
}

final class SlickApiOps[F[_]](private val api: SlickApi[F]) extends AnyVal {
  def blockInsertHashed(
    block: sdk.data.Block,
  )(implicit setDigest: Digest[Set[ByteArray]], bmDigest: Digest[Map[ByteArray, Long]]): F[Unit] =
    api.blockInsert(block)(
      justificationSetHash = block.justificationSet.digest,
      offencesSetHash = block.offencesSet.digest,
      bondsMapHash = block.bondsMap.digest,
      finalFringeHash = block.finalFringe.digest,
      execDeploySetHash = block.execDeploySet.digest,
      mergeDeploySetHash = block.mergeDeploySet.digest,
      dropDeploySetHash = ByteArray.Default,
      mergeDeploySetFinalHash = block.mergeDeploySetFinal.digest,
      dropDeploySetFinalHash = block.dropDeploySetFinal.digest,
    )
}
