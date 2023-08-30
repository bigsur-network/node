// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models
import coop.rchain.models.BitSetBytesMapper.bitSetBytesMapper
import coop.rchain.models.ParSetTypeMapper.parSetESetTypeMapper
import coop.rchain.models.ParMapTypeMapper.parMapEMapTypeMapper
import coop.rchain.models.BigIntTypeMapper.bigIntBytesTypeMapper
import coop.rchain.models.EqualMDerivation.eqMGen
import coop.rchain.models.EqualMImplicits._
import coop.rchain.models.HashMDerivation.hashMGen
import coop.rchain.models.HashMImplicits._

object RhoTypesProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq(
    scalapb.options.ScalapbProto
  )
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      coop.rchain.models.Par,
      coop.rchain.models.TaggedContinuation,
      coop.rchain.models.ParWithRandom,
      coop.rchain.models.PCost,
      coop.rchain.models.ListParWithRandom,
      coop.rchain.models.Var,
      coop.rchain.models.Bundle,
      coop.rchain.models.Send,
      coop.rchain.models.ReceiveBind,
      coop.rchain.models.BindPattern,
      coop.rchain.models.ListBindPatterns,
      coop.rchain.models.Receive,
      coop.rchain.models.New,
      coop.rchain.models.MatchCase,
      coop.rchain.models.Match,
      coop.rchain.models.Expr,
      coop.rchain.models.EList,
      coop.rchain.models.ETuple,
      coop.rchain.models.ESet,
      coop.rchain.models.EMap,
      coop.rchain.models.EMethod,
      coop.rchain.models.KeyValuePair,
      coop.rchain.models.EVar,
      coop.rchain.models.ENot,
      coop.rchain.models.ENeg,
      coop.rchain.models.EMult,
      coop.rchain.models.EDiv,
      coop.rchain.models.EMod,
      coop.rchain.models.EPlus,
      coop.rchain.models.EMinus,
      coop.rchain.models.ELt,
      coop.rchain.models.ELte,
      coop.rchain.models.EGt,
      coop.rchain.models.EGte,
      coop.rchain.models.EEq,
      coop.rchain.models.ENeq,
      coop.rchain.models.EAnd,
      coop.rchain.models.EOr,
      coop.rchain.models.EShortAnd,
      coop.rchain.models.EShortOr,
      coop.rchain.models.EMatches,
      coop.rchain.models.EPercentPercent,
      coop.rchain.models.EPlusPlus,
      coop.rchain.models.EMinusMinus,
      coop.rchain.models.Connective,
      coop.rchain.models.VarRef,
      coop.rchain.models.ConnectiveBody,
      coop.rchain.models.DeployId,
      coop.rchain.models.DeployerId,
      coop.rchain.models.GUnforgeable,
      coop.rchain.models.GPrivate,
      coop.rchain.models.GDeployId,
      coop.rchain.models.GDeployerId,
      coop.rchain.models.GSysAuthToken
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """Cg5SaG9UeXBlcy5wcm90bxoVc2NhbGFwYi9zY2FsYXBiLnByb3RvIs4ECgNQYXISJwoFc2VuZHMYASADKAsyBS5TZW5kQgriP
  wcSBXNlbmRzUgVzZW5kcxIzCghyZWNlaXZlcxgCIAMoCzIILlJlY2VpdmVCDeI/ChIIcmVjZWl2ZXNSCHJlY2VpdmVzEiMKBG5ld
  3MYBCADKAsyBC5OZXdCCeI/BhIEbmV3c1IEbmV3cxInCgVleHBycxgFIAMoCzIFLkV4cHJCCuI/BxIFZXhwcnNSBWV4cHJzEi4KB
  21hdGNoZXMYBiADKAsyBi5NYXRjaEIM4j8JEgdtYXRjaGVzUgdtYXRjaGVzEkQKDHVuZm9yZ2VhYmxlcxgHIAMoCzINLkdVbmZvc
  mdlYWJsZUIR4j8OEgx1bmZvcmdlYWJsZXNSDHVuZm9yZ2VhYmxlcxIvCgdidW5kbGVzGAsgAygLMgcuQnVuZGxlQgziPwkSB2J1b
  mRsZXNSB2J1bmRsZXMSPwoLY29ubmVjdGl2ZXMYCCADKAsyCy5Db25uZWN0aXZlQhDiPw0SC2Nvbm5lY3RpdmVzUgtjb25uZWN0a
  XZlcxJ1Cgtsb2NhbGx5RnJlZRgJIAEoDEJT4j9QCkFjb29wLnJjaGFpbi5tb2RlbHMuQWx3YXlzRXF1YWxbc2NhbGEuY29sbGVjd
  Glvbi5pbW11dGFibGUuQml0U2V0XRILbG9jYWxseUZyZWVSC2xvY2FsbHlGcmVlEjwKD2Nvbm5lY3RpdmVfdXNlZBgKIAEoCEIT4
  j8QEg5jb25uZWN0aXZlVXNlZFIOY29ubmVjdGl2ZVVzZWQimQEKElRhZ2dlZENvbnRpbnVhdGlvbhI5CghwYXJfYm9keRgBIAEoC
  zIOLlBhcldpdGhSYW5kb21CDOI/CRIHcGFyQm9keUgAUgdwYXJCb2R5EjkKDnNjYWxhX2JvZHlfcmVmGAIgASgDQhHiPw4SDHNjY
  WxhQm9keVJlZkgAUgxzY2FsYUJvZHlSZWZCDQoLdGFnZ2VkX2NvbnQilQEKDVBhcldpdGhSYW5kb20SJgoEYm9keRgBIAEoCzIEL
  lBhckIM4j8JEgRib2R58AEBUgRib2R5ElwKC3JhbmRvbVN0YXRlGAIgASgMQjriPzcKKGNvb3AucmNoYWluLmNyeXB0by5oYXNoL
  kJsYWtlMmI1MTJSYW5kb20SC3JhbmRvbVN0YXRlUgtyYW5kb21TdGF0ZSImCgVQQ29zdBIdCgRjb3N0GAEgASgEQgniPwYSBGNvc
  3RSBGNvc3QilgEKEUxpc3RQYXJXaXRoUmFuZG9tEiMKBHBhcnMYASADKAsyBC5QYXJCCeI/BhIEcGFyc1IEcGFycxJcCgtyYW5kb
  21TdGF0ZRgCIAEoDEI64j83Cihjb29wLnJjaGFpbi5jcnlwdG8uaGFzaC5CbGFrZTJiNTEyUmFuZG9tEgtyYW5kb21TdGF0ZVILc
  mFuZG9tU3RhdGUivAEKA1ZhchIsCglib3VuZF92YXIYASABKBFCDeI/ChIIYm91bmRWYXJIAFIIYm91bmRWYXISKQoIZnJlZV92Y
  XIYAiABKBFCDOI/CRIHZnJlZVZhckgAUgdmcmVlVmFyEj0KCHdpbGRjYXJkGAMgASgLMhAuVmFyLldpbGRjYXJkTXNnQg3iPwoSC
  HdpbGRjYXJkSABSCHdpbGRjYXJkGg0KC1dpbGRjYXJkTXNnQg4KDHZhcl9pbnN0YW5jZSKJAQoGQnVuZGxlEiYKBGJvZHkYASABK
  AsyBC5QYXJCDOI/CRIEYm9kefABAVIEYm9keRIsCgl3cml0ZUZsYWcYAiABKAhCDuI/CxIJd3JpdGVGbGFnUgl3cml0ZUZsYWcSK
  QoIcmVhZEZsYWcYAyABKAhCDeI/ChIIcmVhZEZsYWdSCHJlYWRGbGFnIrkCCgRTZW5kEiYKBGNoYW4YASABKAsyBC5QYXJCDOI/C
  RIEY2hhbvABAVIEY2hhbhIjCgRkYXRhGAIgAygLMgQuUGFyQgniPwYSBGRhdGFSBGRhdGESLwoKcGVyc2lzdGVudBgDIAEoCEIP4
  j8MEgpwZXJzaXN0ZW50UgpwZXJzaXN0ZW50EnUKC2xvY2FsbHlGcmVlGAUgASgMQlPiP1AKQWNvb3AucmNoYWluLm1vZGVscy5Bb
  HdheXNFcXVhbFtzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5CaXRTZXRdEgtsb2NhbGx5RnJlZVILbG9jYWxseUZyZWUSPAoPY
  29ubmVjdGl2ZV91c2VkGAYgASgIQhPiPxASDmNvbm5lY3RpdmVVc2VkUg5jb25uZWN0aXZlVXNlZCLOAQoLUmVjZWl2ZUJpbmQSL
  woIcGF0dGVybnMYASADKAsyBC5QYXJCDeI/ChIIcGF0dGVybnNSCHBhdHRlcm5zEiwKBnNvdXJjZRgCIAEoCzIELlBhckIO4j8LE
  gZzb3VyY2XwAQFSBnNvdXJjZRIyCglyZW1haW5kZXIYAyABKAsyBC5WYXJCDuI/CxIJcmVtYWluZGVyUglyZW1haW5kZXISLAoJZ
  nJlZUNvdW50GAQgASgFQg7iPwsSCWZyZWVDb3VudFIJZnJlZUNvdW50IqABCgtCaW5kUGF0dGVybhIvCghwYXR0ZXJucxgBIAMoC
  zIELlBhckIN4j8KEghwYXR0ZXJuc1IIcGF0dGVybnMSMgoJcmVtYWluZGVyGAIgASgLMgQuVmFyQg7iPwsSCXJlbWFpbmRlclIJc
  mVtYWluZGVyEiwKCWZyZWVDb3VudBgDIAEoBUIO4j8LEglmcmVlQ291bnRSCWZyZWVDb3VudCJLChBMaXN0QmluZFBhdHRlcm5zE
  jcKCHBhdHRlcm5zGAEgAygLMgwuQmluZFBhdHRlcm5CDeI/ChIIcGF0dGVybnNSCHBhdHRlcm5zIpQDCgdSZWNlaXZlEi4KBWJpb
  mRzGAEgAygLMgwuUmVjZWl2ZUJpbmRCCuI/BxIFYmluZHNSBWJpbmRzEiYKBGJvZHkYAiABKAsyBC5QYXJCDOI/CRIEYm9kefABA
  VIEYm9keRIvCgpwZXJzaXN0ZW50GAMgASgIQg/iPwwSCnBlcnNpc3RlbnRSCnBlcnNpc3RlbnQSHQoEcGVlaxgEIAEoCEIJ4j8GE
  gRwZWVrUgRwZWVrEiwKCWJpbmRDb3VudBgFIAEoBUIO4j8LEgliaW5kQ291bnRSCWJpbmRDb3VudBJ1Cgtsb2NhbGx5RnJlZRgGI
  AEoDEJT4j9QCkFjb29wLnJjaGFpbi5tb2RlbHMuQWx3YXlzRXF1YWxbc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuQml0U2V0X
  RILbG9jYWxseUZyZWVSC2xvY2FsbHlGcmVlEjwKD2Nvbm5lY3RpdmVfdXNlZBgHIAEoCEIT4j8QEg5jb25uZWN0aXZlVXNlZFIOY
  29ubmVjdGl2ZVVzZWQihwMKA05ldxIsCgliaW5kQ291bnQYASABKBFCDuI/CxIJYmluZENvdW50UgliaW5kQ291bnQSHQoBcBgCI
  AEoCzIELlBhckIJ4j8GEgFw8AEBUgFwEhoKA3VyaRgDIAMoCUII4j8FEgN1cmlSA3VyaRJFCgppbmplY3Rpb25zGAQgAygLMhQuT
  mV3LkluamVjdGlvbnNFbnRyeUIP4j8MEgppbmplY3Rpb25zUgppbmplY3Rpb25zEnUKC2xvY2FsbHlGcmVlGAUgASgMQlPiP1AKQ
  WNvb3AucmNoYWluLm1vZGVscy5BbHdheXNFcXVhbFtzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5CaXRTZXRdEgtsb2NhbGx5R
  nJlZVILbG9jYWxseUZyZWUaWQoPSW5qZWN0aW9uc0VudHJ5EhoKA2tleRgBIAEoCUII4j8FEgNrZXlSA2tleRImCgV2YWx1ZRgCI
  AEoCzIELlBhckIK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBIpgBCglNYXRjaENhc2USLwoHcGF0dGVybhgBIAEoCzIELlBhckIP4j8ME
  gdwYXR0ZXJu8AEBUgdwYXR0ZXJuEiwKBnNvdXJjZRgCIAEoCzIELlBhckIO4j8LEgZzb3VyY2XwAQFSBnNvdXJjZRIsCglmcmVlQ
  291bnQYAyABKAVCDuI/CxIJZnJlZUNvdW50UglmcmVlQ291bnQimAIKBU1hdGNoEiwKBnRhcmdldBgBIAEoCzIELlBhckIO4j8LE
  gZ0YXJnZXTwAQFSBnRhcmdldBIsCgVjYXNlcxgCIAMoCzIKLk1hdGNoQ2FzZUIK4j8HEgVjYXNlc1IFY2FzZXMSdQoLbG9jYWxse
  UZyZWUYBCABKAxCU+I/UApBY29vcC5yY2hhaW4ubW9kZWxzLkFsd2F5c0VxdWFsW3NjYWxhLmNvbGxlY3Rpb24uaW1tdXRhYmxlL
  kJpdFNldF0SC2xvY2FsbHlGcmVlUgtsb2NhbGx5RnJlZRI8Cg9jb25uZWN0aXZlX3VzZWQYBSABKAhCE+I/EBIOY29ubmVjdGl2Z
  VVzZWRSDmNvbm5lY3RpdmVVc2VkIrQPCgRFeHByEiMKBmdfYm9vbBgBIAEoCEIK4j8HEgVnQm9vbEgAUgVnQm9vbBIgCgVnX2lud
  BgCIAEoEkIJ4j8GEgRnSW50SABSBGdJbnQSPQoJZ19iaWdfaW50GCIgASgMQh/iPxwKEXNjYWxhLm1hdGguQmlnSW50EgdnQmlnS
  W50SABSB2dCaWdJbnQSKQoIZ19zdHJpbmcYAyABKAlCDOI/CRIHZ1N0cmluZ0gAUgdnU3RyaW5nEiAKBWdfdXJpGAQgASgJQgniP
  wYSBGdVcmlIAFIEZ1VyaRIzCgxnX2J5dGVfYXJyYXkYGSABKAxCD+I/DBIKZ0J5dGVBcnJheUgAUgpnQnl0ZUFycmF5EjQKCmVfb
  m90X2JvZHkYBSABKAsyBS5FTm90Qg3iPwoSCGVOb3RCb2R5SABSCGVOb3RCb2R5EjQKCmVfbmVnX2JvZHkYBiABKAsyBS5FTmVnQ
  g3iPwoSCGVOZWdCb2R5SABSCGVOZWdCb2R5EjgKC2VfbXVsdF9ib2R5GAcgASgLMgYuRU11bHRCDuI/CxIJZU11bHRCb2R5SABSC
  WVNdWx0Qm9keRI0CgplX2Rpdl9ib2R5GAggASgLMgUuRURpdkIN4j8KEghlRGl2Qm9keUgAUghlRGl2Qm9keRI4CgtlX3BsdXNfY
  m9keRgJIAEoCzIGLkVQbHVzQg7iPwsSCWVQbHVzQm9keUgAUgllUGx1c0JvZHkSPAoMZV9taW51c19ib2R5GAogASgLMgcuRU1pb
  nVzQg/iPwwSCmVNaW51c0JvZHlIAFIKZU1pbnVzQm9keRIwCgllX2x0X2JvZHkYCyABKAsyBC5FTHRCDOI/CRIHZUx0Qm9keUgAU
  gdlTHRCb2R5EjQKCmVfbHRlX2JvZHkYDCABKAsyBS5FTHRlQg3iPwoSCGVMdGVCb2R5SABSCGVMdGVCb2R5EjAKCWVfZ3RfYm9ke
  RgNIAEoCzIELkVHdEIM4j8JEgdlR3RCb2R5SABSB2VHdEJvZHkSNAoKZV9ndGVfYm9keRgOIAEoCzIFLkVHdGVCDeI/ChIIZUd0Z
  UJvZHlIAFIIZUd0ZUJvZHkSMAoJZV9lcV9ib2R5GA8gASgLMgQuRUVxQgziPwkSB2VFcUJvZHlIAFIHZUVxQm9keRI0CgplX25lc
  V9ib2R5GBAgASgLMgUuRU5lcUIN4j8KEghlTmVxQm9keUgAUghlTmVxQm9keRI0CgplX2FuZF9ib2R5GBEgASgLMgUuRUFuZEIN4
  j8KEghlQW5kQm9keUgAUghlQW5kQm9keRIwCgllX29yX2JvZHkYEiABKAsyBC5FT3JCDOI/CRIHZU9yQm9keUgAUgdlT3JCb2R5E
  jQKCmVfdmFyX2JvZHkYEyABKAsyBS5FVmFyQg3iPwoSCGVWYXJCb2R5SABSCGVWYXJCb2R5EjgKC2VfbGlzdF9ib2R5GBQgASgLM
  gYuRUxpc3RCDuI/CxIJZUxpc3RCb2R5SABSCWVMaXN0Qm9keRI8CgxlX3R1cGxlX2JvZHkYFSABKAsyBy5FVHVwbGVCD+I/DBIKZ
  VR1cGxlQm9keUgAUgplVHVwbGVCb2R5Ek8KCmVfc2V0X2JvZHkYFiABKAsyBS5FU2V0QijiPyUKGWNvb3AucmNoYWluLm1vZGVsc
  y5QYXJTZXQSCGVTZXRCb2R5SABSCGVTZXRCb2R5Ek8KCmVfbWFwX2JvZHkYFyABKAsyBS5FTWFwQijiPyUKGWNvb3AucmNoYWluL
  m1vZGVscy5QYXJNYXASCGVNYXBCb2R5SABSCGVNYXBCb2R5EkAKDWVfbWV0aG9kX2JvZHkYGCABKAsyCC5FTWV0aG9kQhDiPw0SC
  2VNZXRob2RCb2R5SABSC2VNZXRob2RCb2R5EkQKDmVfbWF0Y2hlc19ib2R5GBsgASgLMgkuRU1hdGNoZXNCEeI/DhIMZU1hdGNoZ
  XNCb2R5SABSDGVNYXRjaGVzQm9keRJhChZlX3BlcmNlbnRfcGVyY2VudF9ib2R5GBwgASgLMhAuRVBlcmNlbnRQZXJjZW50QhjiP
  xUSE2VQZXJjZW50UGVyY2VudEJvZHlIAFITZVBlcmNlbnRQZXJjZW50Qm9keRJJChBlX3BsdXNfcGx1c19ib2R5GB0gASgLMgouR
  VBsdXNQbHVzQhLiPw8SDWVQbHVzUGx1c0JvZHlIAFINZVBsdXNQbHVzQm9keRJRChJlX21pbnVzX21pbnVzX2JvZHkYHiABKAsyD
  C5FTWludXNNaW51c0IU4j8REg9lTWludXNNaW51c0JvZHlIAFIPZU1pbnVzTWludXNCb2R5EjQKCmVfbW9kX2JvZHkYHyABKAsyB
  S5FTW9kQg3iPwoSCGVNb2RCb2R5SABSCGVNb2RCb2R5EkkKEGVfc2hvcnRfYW5kX2JvZHkYICABKAsyCi5FU2hvcnRBbmRCEuI/D
  xINZVNob3J0QW5kQm9keUgAUg1lU2hvcnRBbmRCb2R5EkUKD2Vfc2hvcnRfb3JfYm9keRghIAEoCzIJLkVTaG9ydE9yQhHiPw4SD
  GVTaG9ydE9yQm9keUgAUgxlU2hvcnRPckJvZHlCDwoNZXhwcl9pbnN0YW5jZSKPAgoFRUxpc3QSHQoCcHMYASADKAsyBC5QYXJCB
  +I/BBICcHNSAnBzEnUKC2xvY2FsbHlGcmVlGAMgASgMQlPiP1AKQWNvb3AucmNoYWluLm1vZGVscy5BbHdheXNFcXVhbFtzY2FsY
  S5jb2xsZWN0aW9uLmltbXV0YWJsZS5CaXRTZXRdEgtsb2NhbGx5RnJlZVILbG9jYWxseUZyZWUSPAoPY29ubmVjdGl2ZV91c2VkG
  AQgASgIQhPiPxASDmNvbm5lY3RpdmVVc2VkUg5jb25uZWN0aXZlVXNlZBIyCglyZW1haW5kZXIYBSABKAsyBC5WYXJCDuI/CxIJc
  mVtYWluZGVyUglyZW1haW5kZXIi3AEKBkVUdXBsZRIdCgJwcxgBIAMoCzIELlBhckIH4j8EEgJwc1ICcHMSdQoLbG9jYWxseUZyZ
  WUYAyABKAxCU+I/UApBY29vcC5yY2hhaW4ubW9kZWxzLkFsd2F5c0VxdWFsW3NjYWxhLmNvbGxlY3Rpb24uaW1tdXRhYmxlLkJpd
  FNldF0SC2xvY2FsbHlGcmVlUgtsb2NhbGx5RnJlZRI8Cg9jb25uZWN0aXZlX3VzZWQYBCABKAhCE+I/EBIOY29ubmVjdGl2ZVVzZ
  WRSDmNvbm5lY3RpdmVVc2VkIo4CCgRFU2V0Eh0KAnBzGAEgAygLMgQuUGFyQgfiPwQSAnBzUgJwcxJ1Cgtsb2NhbGx5RnJlZRgDI
  AEoDEJT4j9QCkFjb29wLnJjaGFpbi5tb2RlbHMuQWx3YXlzRXF1YWxbc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuQml0U2V0X
  RILbG9jYWxseUZyZWVSC2xvY2FsbHlGcmVlEjwKD2Nvbm5lY3RpdmVfdXNlZBgEIAEoCEIT4j8QEg5jb25uZWN0aXZlVXNlZFIOY
  29ubmVjdGl2ZVVzZWQSMgoJcmVtYWluZGVyGAUgASgLMgQuVmFyQg7iPwsSCXJlbWFpbmRlclIJcmVtYWluZGVyIpoCCgRFTWFwE
  ikKA2t2cxgBIAMoCzINLktleVZhbHVlUGFpckII4j8FEgNrdnNSA2t2cxJ1Cgtsb2NhbGx5RnJlZRgDIAEoDEJT4j9QCkFjb29wL
  nJjaGFpbi5tb2RlbHMuQWx3YXlzRXF1YWxbc2NhbGEuY29sbGVjdGlvbi5pbW11dGFibGUuQml0U2V0XRILbG9jYWxseUZyZWVSC
  2xvY2FsbHlGcmVlEjwKD2Nvbm5lY3RpdmVfdXNlZBgEIAEoCEIT4j8QEg5jb25uZWN0aXZlVXNlZFIOY29ubmVjdGl2ZVVzZWQSM
  goJcmVtYWluZGVyGAUgASgLMgQuVmFyQg7iPwsSCXJlbWFpbmRlclIJcmVtYWluZGVyItECCgdFTWV0aG9kEi8KCm1ldGhvZE5hb
  WUYASABKAlCD+I/DBIKbWV0aG9kTmFtZVIKbWV0aG9kTmFtZRIsCgZ0YXJnZXQYAiABKAsyBC5QYXJCDuI/CxIGdGFyZ2V08AEBU
  gZ0YXJnZXQSMgoJYXJndW1lbnRzGAMgAygLMgQuUGFyQg7iPwsSCWFyZ3VtZW50c1IJYXJndW1lbnRzEnUKC2xvY2FsbHlGcmVlG
  AUgASgMQlPiP1AKQWNvb3AucmNoYWluLm1vZGVscy5BbHdheXNFcXVhbFtzY2FsYS5jb2xsZWN0aW9uLmltbXV0YWJsZS5CaXRTZ
  XRdEgtsb2NhbGx5RnJlZVILbG9jYWxseUZyZWUSPAoPY29ubmVjdGl2ZV91c2VkGAYgASgIQhPiPxASDmNvbm5lY3RpdmVVc2VkU
  g5jb25uZWN0aXZlVXNlZCJeCgxLZXlWYWx1ZVBhaXISIwoDa2V5GAEgASgLMgQuUGFyQgviPwgSA2tlefABAVIDa2V5EikKBXZhb
  HVlGAIgASgLMgQuUGFyQg3iPwoSBXZhbHVl8AEBUgV2YWx1ZSIlCgRFVmFyEh0KAXYYASABKAsyBC5WYXJCCeI/BhIBdvABAVIBd
  iIlCgRFTm90Eh0KAXAYASABKAsyBC5QYXJCCeI/BhIBcPABAVIBcCIlCgRFTmVnEh0KAXAYASABKAsyBC5QYXJCCeI/BhIBcPABA
  VIBcCJLCgVFTXVsdBIgCgJwMRgBIAEoCzIELlBhckIK4j8HEgJwMfABAVICcDESIAoCcDIYAiABKAsyBC5QYXJCCuI/BxICcDLwA
  QFSAnAyIkoKBEVEaXYSIAoCcDEYASABKAsyBC5QYXJCCuI/BxICcDHwAQFSAnAxEiAKAnAyGAIgASgLMgQuUGFyQgriPwcSAnAy8
  AEBUgJwMiJKCgRFTW9kEiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoCzIELlBhckIK4j8HEgJwM
  vABAVICcDIiSwoFRVBsdXMSIAoCcDEYASABKAsyBC5QYXJCCuI/BxICcDHwAQFSAnAxEiAKAnAyGAIgASgLMgQuUGFyQgriPwcSA
  nAy8AEBUgJwMiJMCgZFTWludXMSIAoCcDEYASABKAsyBC5QYXJCCuI/BxICcDHwAQFSAnAxEiAKAnAyGAIgASgLMgQuUGFyQgriP
  wcSAnAy8AEBUgJwMiJJCgNFTHQSIAoCcDEYASABKAsyBC5QYXJCCuI/BxICcDHwAQFSAnAxEiAKAnAyGAIgASgLMgQuUGFyQgriP
  wcSAnAy8AEBUgJwMiJKCgRFTHRlEiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoCzIELlBhckIK4
  j8HEgJwMvABAVICcDIiSQoDRUd0EiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoCzIELlBhckIK4
  j8HEgJwMvABAVICcDIiSgoERUd0ZRIgCgJwMRgBIAEoCzIELlBhckIK4j8HEgJwMfABAVICcDESIAoCcDIYAiABKAsyBC5QYXJCC
  uI/BxICcDLwAQFSAnAyIkkKA0VFcRIgCgJwMRgBIAEoCzIELlBhckIK4j8HEgJwMfABAVICcDESIAoCcDIYAiABKAsyBC5QYXJCC
  uI/BxICcDLwAQFSAnAyIkoKBEVOZXESIAoCcDEYASABKAsyBC5QYXJCCuI/BxICcDHwAQFSAnAxEiAKAnAyGAIgASgLMgQuUGFyQ
  griPwcSAnAy8AEBUgJwMiJKCgRFQW5kEiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoCzIELlBhc
  kIK4j8HEgJwMvABAVICcDIiSQoDRU9yEiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoCzIELlBhc
  kIK4j8HEgJwMvABAVICcDIiTwoJRVNob3J0QW5kEiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoC
  zIELlBhckIK4j8HEgJwMvABAVICcDIiTgoIRVNob3J0T3ISIAoCcDEYASABKAsyBC5QYXJCCuI/BxICcDHwAQFSAnAxEiAKAnAyG
  AIgASgLMgQuUGFyQgriPwcSAnAy8AEBUgJwMiJpCghFTWF0Y2hlcxIsCgZ0YXJnZXQYASABKAsyBC5QYXJCDuI/CxIGdGFyZ2V08
  AEBUgZ0YXJnZXQSLwoHcGF0dGVybhgCIAEoCzIELlBhckIP4j8MEgdwYXR0ZXJu8AEBUgdwYXR0ZXJuIlUKD0VQZXJjZW50UGVyY
  2VudBIgCgJwMRgBIAEoCzIELlBhckIK4j8HEgJwMfABAVICcDESIAoCcDIYAiABKAsyBC5QYXJCCuI/BxICcDLwAQFSAnAyIk8KC
  UVQbHVzUGx1cxIgCgJwMRgBIAEoCzIELlBhckIK4j8HEgJwMfABAVICcDESIAoCcDIYAiABKAsyBC5QYXJCCuI/BxICcDLwAQFSA
  nAyIlEKC0VNaW51c01pbnVzEiAKAnAxGAEgASgLMgQuUGFyQgriPwcSAnAx8AEBUgJwMRIgCgJwMhgCIAEoCzIELlBhckIK4j8HE
  gJwMvABAVICcDIi2QQKCkNvbm5lY3RpdmUSRwoNY29ubl9hbmRfYm9keRgBIAEoCzIPLkNvbm5lY3RpdmVCb2R5QhDiPw0SC2Nvb
  m5BbmRCb2R5SABSC2Nvbm5BbmRCb2R5EkQKDGNvbm5fb3JfYm9keRgCIAEoCzIPLkNvbm5lY3RpdmVCb2R5Qg/iPwwSCmNvbm5Pc
  kJvZHlIAFIKY29ubk9yQm9keRI8Cg1jb25uX25vdF9ib2R5GAMgASgLMgQuUGFyQhDiPw0SC2Nvbm5Ob3RCb2R5SABSC2Nvbm5Ob
  3RCb2R5EjwKDHZhcl9yZWZfYm9keRgEIAEoCzIHLlZhclJlZkIP4j8MEgp2YXJSZWZCb2R5SABSCnZhclJlZkJvZHkSLAoJY29ub
  l9ib29sGAUgASgIQg3iPwoSCGNvbm5Cb29sSABSCGNvbm5Cb29sEikKCGNvbm5faW50GAYgASgIQgziPwkSB2Nvbm5JbnRIAFIHY
  29ubkludBIzCgxjb25uX2JpZ19pbnQYCiABKAhCD+I/DBIKY29ubkJpZ0ludEgAUgpjb25uQmlnSW50EjIKC2Nvbm5fc3RyaW5nG
  AcgASgIQg/iPwwSCmNvbm5TdHJpbmdIAFIKY29ublN0cmluZxIpCghjb25uX3VyaRgIIAEoCEIM4j8JEgdjb25uVXJpSABSB2Nvb
  m5VcmkSPAoPY29ubl9ieXRlX2FycmF5GAkgASgIQhLiPw8SDWNvbm5CeXRlQXJyYXlIAFINY29ubkJ5dGVBcnJheUIVChNjb25uZ
  WN0aXZlX2luc3RhbmNlIkwKBlZhclJlZhIgCgVpbmRleBgBIAEoEUIK4j8HEgVpbmRleFIFaW5kZXgSIAoFZGVwdGgYAiABKBFCC
  uI/BxIFZGVwdGhSBWRlcHRoIi8KDkNvbm5lY3RpdmVCb2R5Eh0KAnBzGAEgAygLMgQuUGFyQgfiPwQSAnBzUgJwcyImCghEZXBsb
  3lJZBIaCgNzaWcYASABKAxCCOI/BRIDc2lnUgNzaWciOgoKRGVwbG95ZXJJZBIsCglwdWJsaWNLZXkYASABKAxCDuI/CxIJcHVib
  GljS2V5UglwdWJsaWNLZXki3gIKDEdVbmZvcmdlYWJsZRJECg5nX3ByaXZhdGVfYm9keRgBIAEoCzIJLkdQcml2YXRlQhHiPw4SD
  GdQcml2YXRlQm9keUgAUgxnUHJpdmF0ZUJvZHkSSQoQZ19kZXBsb3lfaWRfYm9keRgCIAEoCzIKLkdEZXBsb3lJZEIS4j8PEg1nR
  GVwbG95SWRCb2R5SABSDWdEZXBsb3lJZEJvZHkSUQoSZ19kZXBsb3llcl9pZF9ib2R5GAMgASgLMgwuR0RlcGxveWVySWRCFOI/E
  RIPZ0RlcGxveWVySWRCb2R5SABSD2dEZXBsb3llcklkQm9keRJaChVnX3N5c19hdXRoX3Rva2VuX2JvZHkYBCABKAsyDi5HU3lzQ
  XV0aFRva2VuQhbiPxMSEWdTeXNBdXRoVG9rZW5Cb2R5SABSEWdTeXNBdXRoVG9rZW5Cb2R5Qg4KDHVuZl9pbnN0YW5jZSIjCghHU
  HJpdmF0ZRIXCgJpZBgBIAEoDEIH4j8EEgJpZFICaWQiJwoJR0RlcGxveUlkEhoKA3NpZxgBIAEoDEII4j8FEgNzaWdSA3NpZyI7C
  gtHRGVwbG95ZXJJZBIsCglwdWJsaWNLZXkYASABKAxCDuI/CxIJcHVibGljS2V5UglwdWJsaWNLZXkiDwoNR1N5c0F1dGhUb2tlb
  kKBAuI//QEKEmNvb3AucmNoYWluLm1vZGVscxo2Y29vcC5yY2hhaW4ubW9kZWxzLkJpdFNldEJ5dGVzTWFwcGVyLmJpdFNldEJ5d
  GVzTWFwcGVyGjhjb29wLnJjaGFpbi5tb2RlbHMuUGFyU2V0VHlwZU1hcHBlci5wYXJTZXRFU2V0VHlwZU1hcHBlcho4Y29vcC5yY
  2hhaW4ubW9kZWxzLlBhck1hcFR5cGVNYXBwZXIucGFyTWFwRU1hcFR5cGVNYXBwZXIaOWNvb3AucmNoYWluLm1vZGVscy5CaWdJb
  nRUeXBlTWFwcGVyLmJpZ0ludEJ5dGVzVHlwZU1hcHBlckgAYgZwcm90bzM="""
      ).mkString)
  lazy val scalaDescriptor: _root_.scalapb.descriptors.FileDescriptor = {
    val scalaProto = com.google.protobuf.descriptor.FileDescriptorProto.parseFrom(ProtoBytes)
    _root_.scalapb.descriptors.FileDescriptor.buildFrom(scalaProto, dependencies.map(_.scalaDescriptor))
  }
  lazy val javaDescriptor: com.google.protobuf.Descriptors.FileDescriptor = {
    val javaProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.parseFrom(ProtoBytes)
    com.google.protobuf.Descriptors.FileDescriptor.buildFrom(javaProto, _root_.scala.Array(
      scalapb.options.ScalapbProto.javaDescriptor
    ))
  }
  @deprecated("Use javaDescriptor instead. In a future version this will refer to scalaDescriptor.", "ScalaPB 0.5.47")
  def descriptor: com.google.protobuf.Descriptors.FileDescriptor = javaDescriptor
}