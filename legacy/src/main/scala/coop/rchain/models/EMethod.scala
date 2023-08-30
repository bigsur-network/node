// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models
import coop.rchain.models.BitSetBytesMapper.bitSetBytesMapper
import coop.rchain.models.ParSetTypeMapper.parSetESetTypeMapper
import coop.rchain.models.ParMapTypeMapper.parMapEMapTypeMapper
import coop.rchain.models.BigIntTypeMapper.bigIntBytesTypeMapper
import coop.rchain.models.EqualMDerivation.gen
import coop.rchain.models.EqualMImplicits._
import coop.rchain.models.HashMDerivation.gen
import coop.rchain.models.HashMImplicits._

/** *
  * `target.method(arguments)`
  */
@SerialVersionUID(0L)
final case class EMethod(
    methodName: _root_.scala.Predef.String = "",
    target: coop.rchain.models.Par = coop.rchain.models.Par.defaultInstance,
    arguments: _root_.scala.Seq[coop.rchain.models.Par] = _root_.scala.Seq.empty,
    locallyFree: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet] = coop.rchain.models.EMethod._typemapper_locallyFree.toCustom(_root_.com.google.protobuf.ByteString.EMPTY),
    connectiveUsed: _root_.scala.Boolean = false
    ) extends coop.rchain.models.StacksafeMessage[EMethod] with scalapb.lenses.Updatable[EMethod] {
    
    override def equals(x: Any): Boolean = {
    
      import coop.rchain.catscontrib.effect.implicits.sEval
    
     coop.rchain.models.EqualM[coop.rchain.models.EMethod].equals[cats.Eval](this, x).value
    
    }
    
    override def hashCode(): Int = {
    
      import coop.rchain.catscontrib.effect.implicits.sEval
    
     coop.rchain.models.HashM[coop.rchain.models.EMethod].hash[cats.Eval](this).value
    
    }
    
    
    def mergeFromM[F[_]: cats.effect.Sync](`_input__`: _root_.com.google.protobuf.CodedInputStream): F[coop.rchain.models.EMethod] = {
      
      import cats.effect.Sync
      import cats.syntax.all._
      
      Sync[F].defer {
        var __methodName = this.methodName
        var __target = this.target
        val __arguments = (new _root_.scala.collection.immutable.VectorBuilder[coop.rchain.models.Par] ++= this.arguments)
        var __locallyFree = this.locallyFree
        var __connectiveUsed = this.connectiveUsed
        var _done__ = false
        
        Sync[F].whileM_ (Sync[F].delay { !_done__ }) {
          for {
            _tag__ <- Sync[F].delay { _input__.readTag() }
            _ <- _tag__ match {
              case 0 => Sync[F].delay { _done__ = true }
              case 10 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readStringRequireUtf8() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __methodName = customTypeValue }
                } yield ()
              case 18 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, __target)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __target = customTypeValue }
                } yield ()
              case 26 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, coop.rchain.models.Par.defaultInstance)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __arguments += customTypeValue }
                } yield ()
              case 42 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBytes() }
                  customTypeValue =  coop.rchain.models.EMethod._typemapper_locallyFree.toCustom(readValue)
                  _               <- Sync[F].delay { __locallyFree = customTypeValue }
                } yield ()
              case 48 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveUsed = customTypeValue }
                } yield ()
            case tag => Sync[F].delay { _input__.skipField(tag) }
            }
          } yield ()
        }
        .map { _ => coop.rchain.models.EMethod(
          methodName = __methodName,
          target = __target,
          arguments = __arguments.result(),
          locallyFree = __locallyFree,
          connectiveUsed = __connectiveUsed
        )}
      }
    }
    
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = methodName
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = target
        if (__value.serializedSize != 0) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
        }
      };
      arguments.foreach { __item =>
        val __value = __item
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      
      {
        val __value = coop.rchain.models.EMethod._typemapper_locallyFree.toBase(locallyFree)
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBytesSize(5, __value)
        }
      };
      
      {
        val __value = connectiveUsed
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(6, __value)
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
        val __v = methodName
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = target
        if (__v.serializedSize != 0) {
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      };
      arguments.foreach { __v =>
        val __m = __v
        _output__.writeTag(3, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = coop.rchain.models.EMethod._typemapper_locallyFree.toBase(locallyFree)
        if (!__v.isEmpty) {
          _output__.writeBytes(5, __v)
        }
      };
      {
        val __v = connectiveUsed
        if (__v != false) {
          _output__.writeBool(6, __v)
        }
      };
    }
    def withMethodName(__v: _root_.scala.Predef.String): EMethod = copy(methodName = __v)
    def withTarget(__v: coop.rchain.models.Par): EMethod = copy(target = __v)
    def clearArguments = copy(arguments = _root_.scala.Seq.empty)
    def addArguments(__vs: coop.rchain.models.Par *): EMethod = addAllArguments(__vs)
    def addAllArguments(__vs: Iterable[coop.rchain.models.Par]): EMethod = copy(arguments = arguments ++ __vs)
    def withArguments(__v: _root_.scala.Seq[coop.rchain.models.Par]): EMethod = copy(arguments = __v)
    def withLocallyFree(__v: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]): EMethod = copy(locallyFree = __v)
    def withConnectiveUsed(__v: _root_.scala.Boolean): EMethod = copy(connectiveUsed = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = methodName
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = target
          if (__t != coop.rchain.models.Par.defaultInstance) __t else null
        }
        case 3 => arguments
        case 5 => {
          val __t = coop.rchain.models.EMethod._typemapper_locallyFree.toBase(locallyFree)
          if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
        }
        case 6 => {
          val __t = connectiveUsed
          if (__t != false) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(methodName)
        case 2 => target.toPMessage
        case 3 => _root_.scalapb.descriptors.PRepeated(arguments.iterator.map(_.toPMessage).toVector)
        case 5 => _root_.scalapb.descriptors.PByteString(coop.rchain.models.EMethod._typemapper_locallyFree.toBase(locallyFree))
        case 6 => _root_.scalapb.descriptors.PBoolean(connectiveUsed)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.EMethod.type = coop.rchain.models.EMethod
    // @@protoc_insertion_point(GeneratedMessage[EMethod])
}

object EMethod extends scalapb.GeneratedMessageCompanion[coop.rchain.models.EMethod] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.EMethod] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.EMethod = {
    var __methodName: _root_.scala.Predef.String = ""
    var __target: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None
    val __arguments: _root_.scala.collection.immutable.VectorBuilder[coop.rchain.models.Par] = new _root_.scala.collection.immutable.VectorBuilder[coop.rchain.models.Par]
    var __locallyFree: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
    var __connectiveUsed: _root_.scala.Boolean = false
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __methodName = _input__.readStringRequireUtf8()
        case 18 =>
          __target = _root_.scala.Some(__target.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 26 =>
          __arguments += _root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__)
        case 42 =>
          __locallyFree = _input__.readBytes()
        case 48 =>
          __connectiveUsed = _input__.readBool()
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.EMethod(
        methodName = __methodName,
        target = __target.getOrElse(coop.rchain.models.Par.defaultInstance),
        arguments = __arguments.result(),
        locallyFree = coop.rchain.models.EMethod._typemapper_locallyFree.toCustom(__locallyFree),
        connectiveUsed = __connectiveUsed
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.EMethod] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      coop.rchain.models.EMethod(
        methodName = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        target = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[coop.rchain.models.Par]).getOrElse(coop.rchain.models.Par.defaultInstance),
        arguments = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Seq[coop.rchain.models.Par]]).getOrElse(_root_.scala.Seq.empty),
        locallyFree = coop.rchain.models.EMethod._typemapper_locallyFree.toCustom(__fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)),
        connectiveUsed = __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[_root_.scala.Boolean]).getOrElse(false)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = RhoTypesProto.javaDescriptor.getMessageTypes().get(20)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = RhoTypesProto.scalaDescriptor.messages(20)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 2 => __out = coop.rchain.models.Par
      case 3 => __out = coop.rchain.models.Par
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = coop.rchain.models.EMethod(
    methodName = "",
    target = coop.rchain.models.Par.defaultInstance,
    arguments = _root_.scala.Seq.empty,
    locallyFree = coop.rchain.models.EMethod._typemapper_locallyFree.toCustom(_root_.com.google.protobuf.ByteString.EMPTY),
    connectiveUsed = false
  )
  implicit class EMethodLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.EMethod]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.EMethod](_l) {
    def methodName: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.methodName)((c_, f_) => c_.copy(methodName = f_))
    def target: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par] = field(_.target)((c_, f_) => c_.copy(target = f_))
    def arguments: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[coop.rchain.models.Par]] = field(_.arguments)((c_, f_) => c_.copy(arguments = f_))
    def locallyFree: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]] = field(_.locallyFree)((c_, f_) => c_.copy(locallyFree = f_))
    def connectiveUsed: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.connectiveUsed)((c_, f_) => c_.copy(connectiveUsed = f_))
  }
  final val METHODNAME_FIELD_NUMBER = 1
  final val TARGET_FIELD_NUMBER = 2
  final val ARGUMENTS_FIELD_NUMBER = 3
  final val LOCALLYFREE_FIELD_NUMBER = 5
  final val CONNECTIVE_USED_FIELD_NUMBER = 6
  @transient
  private[models] val _typemapper_locallyFree: _root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]] = implicitly[_root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]]]
  def of(
    methodName: _root_.scala.Predef.String,
    target: coop.rchain.models.Par,
    arguments: _root_.scala.Seq[coop.rchain.models.Par],
    locallyFree: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet],
    connectiveUsed: _root_.scala.Boolean
  ): _root_.coop.rchain.models.EMethod = _root_.coop.rchain.models.EMethod(
    methodName,
    target,
    arguments,
    locallyFree,
    connectiveUsed
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[EMethod])
}
