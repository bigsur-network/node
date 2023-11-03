// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models

@SerialVersionUID(0L)
final case class EMatches(
    target: coop.rchain.models.Par = coop.rchain.models.Par.defaultInstance,
    pattern: coop.rchain.models.Par = coop.rchain.models.Par.defaultInstance
    ) extends coop.rchain.models.StacksafeMessage[EMatches] with scalapb.lenses.Updatable[EMatches] {

    def mergeFromM[F[_]: cats.effect.Sync](`_input__`: _root_.com.google.protobuf.CodedInputStream): F[coop.rchain.models.EMatches] = {
      
      import cats.effect.Sync
      import cats.syntax.all.*
      
      Sync[F].defer {
        var __target = this.target
        var __pattern = this.pattern
        var _done__ = false
        
        Sync[F].whileM_ (Sync[F].delay { !_done__ }) {
          for {
            _tag__ <- Sync[F].delay { _input__.readTag() }
            _ <- _tag__ match {
              case 0 => Sync[F].delay { _done__ = true }
              case 10 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, __target)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __target = customTypeValue }
                } yield ()
              case 18 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, __pattern)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __pattern = customTypeValue }
                } yield ()
            case tag => Sync[F].delay { _input__.skipField(tag) }
            }
          } yield ()
        }
        .map { _ => coop.rchain.models.EMatches(
          target = __target,
          pattern = __pattern
        )}
      }
    }
    
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = target
        if (__value.serializedSize != 0) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
        }
      };
      
      {
        val __value = pattern
        if (__value.serializedSize != 0) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
        }
      };
      __size
    }
    override def serializedSize: _root_.scala.Int = {
      var __size = __serializedSizeMemoized
      if (__size == 0) {
        __size = __computeSerializedSize() + 1
        __serializedSizeMemoized = __size
      }
      __size - 1
      
    }
    
    @transient var _serializedSizeM: coop.rchain.models.Memo[Int] = null
    
    def serializedSizeM: coop.rchain.models.Memo[Int] = synchronized {
      if(_serializedSizeM == null) {
        _serializedSizeM = new coop.rchain.models.Memo(coop.rchain.models.ProtoM.serializedSize(this))
        _serializedSizeM
      } else _serializedSizeM
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
      {
        val __v = target
        if (__v.serializedSize != 0) {
          _output__.writeTag(1, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      };
      {
        val __v = pattern
        if (__v.serializedSize != 0) {
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      };
    }
    def withTarget(__v: coop.rchain.models.Par): EMatches = copy(target = __v)
    def withPattern(__v: coop.rchain.models.Par): EMatches = copy(pattern = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = target
          if (__t != coop.rchain.models.Par.defaultInstance) __t else null
        }
        case 2 => {
          val __t = pattern
          if (__t != coop.rchain.models.Par.defaultInstance) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => target.toPMessage
        case 2 => pattern.toPMessage
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.EMatches.type = coop.rchain.models.EMatches
    // @@protoc_insertion_point(GeneratedMessage[EMatches])
}

object EMatches extends scalapb.GeneratedMessageCompanion[coop.rchain.models.EMatches] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.EMatches] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.EMatches = {
    var __target: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None
    var __pattern: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __target = _root_.scala.Some(__target.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 18 =>
          __pattern = _root_.scala.Some(__pattern.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.EMatches(
        target = __target.getOrElse(coop.rchain.models.Par.defaultInstance),
        pattern = __pattern.getOrElse(coop.rchain.models.Par.defaultInstance)
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.EMatches] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      coop.rchain.models.EMatches(
        target = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[coop.rchain.models.Par]).getOrElse(coop.rchain.models.Par.defaultInstance),
        pattern = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[coop.rchain.models.Par]).getOrElse(coop.rchain.models.Par.defaultInstance)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = RhoTypesProto.javaDescriptor.getMessageTypes().get(40)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = RhoTypesProto.scalaDescriptor.messages(40)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = coop.rchain.models.Par
      case 2 => __out = coop.rchain.models.Par
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = coop.rchain.models.EMatches(
    target = coop.rchain.models.Par.defaultInstance,
    pattern = coop.rchain.models.Par.defaultInstance
  )
  implicit class EMatchesLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.EMatches]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.EMatches](_l) {
    def target: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par] = field(_.target)((c_, f_) => c_.copy(target = f_))
    def pattern: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par] = field(_.pattern)((c_, f_) => c_.copy(pattern = f_))
  }
  final val TARGET_FIELD_NUMBER = 1
  final val PATTERN_FIELD_NUMBER = 2
  def of(
    target: coop.rchain.models.Par,
    pattern: coop.rchain.models.Par
  ): _root_.coop.rchain.models.EMatches = _root_.coop.rchain.models.EMatches(
    target,
    pattern
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[EMatches])
}
