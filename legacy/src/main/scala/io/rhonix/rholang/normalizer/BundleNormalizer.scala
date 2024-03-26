package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.interpreter.errors.{InterpreterError, UnexpectedBundleContent}
import io.rhonix.rholang.normalizer.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.env.NestingWriter
import io.rhonix.rholang.types.{BundleN, ConnectiveN, ParN, ParProcN}

object BundleNormalizer {
  def normalizeBundle[F[_]: Sync: NormalizerRec: NestingWriter](p: PBundle): F[BundleN] = {
    def connectivesExistOnTop(p: ParN): F[Boolean] =
      p match {
        case _: ConnectiveN  => true.pure
        case pProc: ParProcN => pProc.ps.existsM(connectivesExistOnTop)
        case _               => false.pure
      }

    def raiseError: F[Unit] = UnexpectedBundleContent(
      s"Illegal top level connective in bundle at: ${SourcePosition(p.line_num, p.col_num)}.",
    ).raiseError

    for {
      // Normalize the target while marking that this process is inside a bundle.
      target <- NormalizerRec[F].normalize(p.proc_).withinBundle()
      // Inside bundle target it is prohibited to have connectives on top level.
      _      <- connectivesExistOnTop(target).ifM(raiseError, Sync[F].unit)

      outermostBundle = p.bundle_ match {
                          case _: BundleReadWrite => BundleN(target, writeFlag = true, readFlag = true)
                          case _: BundleRead      => BundleN(target, writeFlag = false, readFlag = true)
                          case _: BundleWrite     => BundleN(target, writeFlag = true, readFlag = false)
                          case _: BundleEquiv     => BundleN(target, writeFlag = false, readFlag = false)
                        }

    } yield target match {
      case b: BundleN => outermostBundle.merge(b) // When there are nested bundles, these bundles is merged into one.
      case _          => outermostBundle
    }
  }
}
