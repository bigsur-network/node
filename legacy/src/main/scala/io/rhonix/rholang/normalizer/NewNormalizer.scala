package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.normalizer.GroundNormalizeMatcher
import coop.rchain.rholang.interpreter.compiler.{NameSort, SourcePosition, VarSort}
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.env.*
import io.rhonix.rholang.normalizer.syntax.all.*
import io.rhonix.rholang.types.{NewN, ParN}

import scala.jdk.CollectionConverters.*

object NewNormalizer {
  def normalizeNew[F[_]: Sync: NormalizerRec: BoundVarScope, T >: VarSort: BoundVarWriter](p: PNew): F[NewN] =
    Sync[F].defer {
      
      val (simpleBindings, urnData) = p.listnamedecl_.asScala.toSeq
        .foldRight(
          (Seq.empty[(String, VarSort, SourcePosition)], Seq.empty[(String, (String, VarSort, SourcePosition))]),
        ) {
          case (n: NameDeclSimpl, (simpleAcc, urnAcc)) =>
            ((n.var_, NameSort, SourcePosition(n.line_num, n.col_num)) +: simpleAcc, urnAcc)
          case (n: NameDeclUrn, (simpleAcc, urnAcc))   =>
            val urnData = (
              GroundNormalizeMatcher.stripUri(n.uriliteral_),
              (n.var_, NameSort, SourcePosition(n.line_num, n.col_num)),
            )
            (simpleAcc, urnData +: urnAcc)
          case (_, acc)                                => acc
        }

      val sortedUrnData = urnData.sortBy(_._1) // Sort by uris in lexicographical order

      val (uris, urnBindings) = sortedUrnData.unzip

      val boundVars = simpleBindings ++ urnBindings

      NormalizerRec[F]
        .normalize(p.proc_)
        .withAddedBoundVars[T](boundVars)
        .map { case (normalizedPar, indices) =>
          NewN(bindCount = indices.size, p = normalizedPar, uri = uris, injections = Map[String, ParN]())
        }
    }
}
