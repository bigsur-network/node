// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models

@SerialVersionUID(0L)
final case class GSysAuthToken(
    ) extends coop.rchain.models.StacksafeMessage[GSysAuthToken] with scalapb.lenses.Updatable[GSysAuthToken] {

    def mergeFromM[F[_]: cats.effect.Sync](`_input__`: _root_.com.google.protobuf.CodedInputStream): F[coop.rchain.models.GSysAuthToken] = {
      
      import cats.effect.Sync
      import cats.syntax.all.*
      
      Sync[F].defer {
        var _done__ = false
        
        Sync[F].whileM_ (Sync[F].delay { !_done__ }) {
          for {
            _tag__ <- Sync[F].delay { _input__.readTag() }
            _ <- _tag__ match {
              case 0 => Sync[F].delay { _done__ = true }
            case tag => Sync[F].delay { _input__.skipField(tag) }
            }
          } yield ()
        }
        .map { _ => coop.rchain.models.GSysAuthToken(
        )}
      }
    }
    
    final override def serializedSize: _root_.scala.Int = 0
    
    @transient var _serializedSizeM: coop.rchain.models.Memo[Int] = null
    
    def serializedSizeM: coop.rchain.models.Memo[Int] = synchronized {
      if(_serializedSizeM == null) {
        _serializedSizeM = new coop.rchain.models.Memo(coop.rchain.models.ProtoM.serializedSize(this))
        _serializedSizeM
      } else _serializedSizeM
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
    }
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = throw new MatchError(__fieldNumber)
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = throw new MatchError(__field)
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.GSysAuthToken.type = coop.rchain.models.GSysAuthToken
    // @@protoc_insertion_point(GeneratedMessage[GSysAuthToken])
}

object GSysAuthToken extends scalapb.GeneratedMessageCompanion[coop.rchain.models.GSysAuthToken] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.GSysAuthToken] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.GSysAuthToken = {
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.GSysAuthToken(
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.GSysAuthToken] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      coop.rchain.models.GSysAuthToken(
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = RhoTypesProto.javaDescriptor.getMessageTypes().get(53)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = RhoTypesProto.scalaDescriptor.messages(53)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = coop.rchain.models.GSysAuthToken(
  )
  implicit class GSysAuthTokenLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.GSysAuthToken]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.GSysAuthToken](_l) {
  }
  def of(
  ): _root_.coop.rchain.models.GSysAuthToken = _root_.coop.rchain.models.GSysAuthToken(
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[GSysAuthToken])
}
