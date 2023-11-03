// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models
import coop.rchain.models.BitSetBytesMapper.bitSetBytesMapper

/** *
  * A receive is written `for(binds) { body }`
  * i.e. `for(patterns &lt;- source) { body }`
  * or for a persistent recieve: `for(patterns &lt;= source) { body }`.
  *
  * It's an error for free Variable to occur more than once in a pattern.
  */
@SerialVersionUID(0L)
final case class Receive(
    binds: _root_.scala.Seq[coop.rchain.models.ReceiveBind] = _root_.scala.Seq.empty,
    body: coop.rchain.models.Par = coop.rchain.models.Par.defaultInstance,
    persistent: _root_.scala.Boolean = false,
    peek: _root_.scala.Boolean = false,
    bindCount: _root_.scala.Int = 0,
    locallyFree: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet] = coop.rchain.models.Receive._typemapper_locallyFree.toCustom(_root_.com.google.protobuf.ByteString.EMPTY),
    connectiveUsed: _root_.scala.Boolean = false
    ) extends coop.rchain.models.StacksafeMessage[Receive] with scalapb.lenses.Updatable[Receive] {

    def mergeFromM[F[_]: cats.effect.Sync](`_input__`: _root_.com.google.protobuf.CodedInputStream): F[coop.rchain.models.Receive] = {
      
      import cats.effect.Sync
      import cats.syntax.all.*
      
      Sync[F].defer {
        val __binds = (new _root_.scala.collection.immutable.VectorBuilder[coop.rchain.models.ReceiveBind] ++= this.binds)
        var __body = this.body
        var __persistent = this.persistent
        var __peek = this.peek
        var __bindCount = this.bindCount
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
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, coop.rchain.models.ReceiveBind.defaultInstance)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __binds += customTypeValue }
                } yield ()
              case 18 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, __body)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __body = customTypeValue }
                } yield ()
              case 24 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __persistent = customTypeValue }
                } yield ()
              case 32 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __peek = customTypeValue }
                } yield ()
              case 40 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readInt32() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __bindCount = customTypeValue }
                } yield ()
              case 50 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBytes() }
                  customTypeValue =  coop.rchain.models.Receive._typemapper_locallyFree.toCustom(readValue)
                  _               <- Sync[F].delay { __locallyFree = customTypeValue }
                } yield ()
              case 56 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveUsed = customTypeValue }
                } yield ()
            case tag => Sync[F].delay { _input__.skipField(tag) }
            }
          } yield ()
        }
        .map { _ => coop.rchain.models.Receive(
          binds = __binds.result(),
          body = __body,
          persistent = __persistent,
          peek = __peek,
          bindCount = __bindCount,
          locallyFree = __locallyFree,
          connectiveUsed = __connectiveUsed
        )}
      }
    }
    
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      binds.foreach { __item =>
        val __value = __item
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      
      {
        val __value = body
        if (__value.serializedSize != 0) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
        }
      };
      
      {
        val __value = persistent
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(3, __value)
        }
      };
      
      {
        val __value = peek
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(4, __value)
        }
      };
      
      {
        val __value = bindCount
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(5, __value)
        }
      };
      
      {
        val __value = coop.rchain.models.Receive._typemapper_locallyFree.toBase(locallyFree)
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBytesSize(6, __value)
        }
      };
      
      {
        val __value = connectiveUsed
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(7, __value)
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
      binds.foreach { __v =>
        val __m = __v
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = body
        if (__v.serializedSize != 0) {
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      };
      {
        val __v = persistent
        if (__v != false) {
          _output__.writeBool(3, __v)
        }
      };
      {
        val __v = peek
        if (__v != false) {
          _output__.writeBool(4, __v)
        }
      };
      {
        val __v = bindCount
        if (__v != 0) {
          _output__.writeInt32(5, __v)
        }
      };
      {
        val __v = coop.rchain.models.Receive._typemapper_locallyFree.toBase(locallyFree)
        if (!__v.isEmpty) {
          _output__.writeBytes(6, __v)
        }
      };
      {
        val __v = connectiveUsed
        if (__v != false) {
          _output__.writeBool(7, __v)
        }
      };
    }
    def clearBinds = copy(binds = _root_.scala.Seq.empty)
    def addBinds(__vs: coop.rchain.models.ReceiveBind *): Receive = addAllBinds(__vs)
    def addAllBinds(__vs: Iterable[coop.rchain.models.ReceiveBind]): Receive = copy(binds = binds ++ __vs)
    def withBinds(__v: _root_.scala.Seq[coop.rchain.models.ReceiveBind]): Receive = copy(binds = __v)
    def withBody(__v: coop.rchain.models.Par): Receive = copy(body = __v)
    def withPersistent(__v: _root_.scala.Boolean): Receive = copy(persistent = __v)
    def withPeek(__v: _root_.scala.Boolean): Receive = copy(peek = __v)
    def withBindCount(__v: _root_.scala.Int): Receive = copy(bindCount = __v)
    def withLocallyFree(__v: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]): Receive = copy(locallyFree = __v)
    def withConnectiveUsed(__v: _root_.scala.Boolean): Receive = copy(connectiveUsed = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => binds
        case 2 => {
          val __t = body
          if (__t != coop.rchain.models.Par.defaultInstance) __t else null
        }
        case 3 => {
          val __t = persistent
          if (__t != false) __t else null
        }
        case 4 => {
          val __t = peek
          if (__t != false) __t else null
        }
        case 5 => {
          val __t = bindCount
          if (__t != 0) __t else null
        }
        case 6 => {
          val __t = coop.rchain.models.Receive._typemapper_locallyFree.toBase(locallyFree)
          if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
        }
        case 7 => {
          val __t = connectiveUsed
          if (__t != false) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(binds.iterator.map(_.toPMessage).toVector)
        case 2 => body.toPMessage
        case 3 => _root_.scalapb.descriptors.PBoolean(persistent)
        case 4 => _root_.scalapb.descriptors.PBoolean(peek)
        case 5 => _root_.scalapb.descriptors.PInt(bindCount)
        case 6 => _root_.scalapb.descriptors.PByteString(coop.rchain.models.Receive._typemapper_locallyFree.toBase(locallyFree))
        case 7 => _root_.scalapb.descriptors.PBoolean(connectiveUsed)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.Receive.type = coop.rchain.models.Receive
    // @@protoc_insertion_point(GeneratedMessage[Receive])
}

object Receive extends scalapb.GeneratedMessageCompanion[coop.rchain.models.Receive] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.Receive] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.Receive = {
    val __binds: _root_.scala.collection.immutable.VectorBuilder[coop.rchain.models.ReceiveBind] = new _root_.scala.collection.immutable.VectorBuilder[coop.rchain.models.ReceiveBind]
    var __body: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None
    var __persistent: _root_.scala.Boolean = false
    var __peek: _root_.scala.Boolean = false
    var __bindCount: _root_.scala.Int = 0
    var __locallyFree: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
    var __connectiveUsed: _root_.scala.Boolean = false
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __binds += _root_.scalapb.LiteParser.readMessage[coop.rchain.models.ReceiveBind](_input__)
        case 18 =>
          __body = _root_.scala.Some(__body.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 24 =>
          __persistent = _input__.readBool()
        case 32 =>
          __peek = _input__.readBool()
        case 40 =>
          __bindCount = _input__.readInt32()
        case 50 =>
          __locallyFree = _input__.readBytes()
        case 56 =>
          __connectiveUsed = _input__.readBool()
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.Receive(
        binds = __binds.result(),
        body = __body.getOrElse(coop.rchain.models.Par.defaultInstance),
        persistent = __persistent,
        peek = __peek,
        bindCount = __bindCount,
        locallyFree = coop.rchain.models.Receive._typemapper_locallyFree.toCustom(__locallyFree),
        connectiveUsed = __connectiveUsed
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.Receive] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      coop.rchain.models.Receive(
        binds = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Seq[coop.rchain.models.ReceiveBind]]).getOrElse(_root_.scala.Seq.empty),
        body = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[coop.rchain.models.Par]).getOrElse(coop.rchain.models.Par.defaultInstance),
        persistent = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        peek = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        bindCount = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Int]).getOrElse(0),
        locallyFree = coop.rchain.models.Receive._typemapper_locallyFree.toCustom(__fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)),
        connectiveUsed = __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).map(_.as[_root_.scala.Boolean]).getOrElse(false)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = RhoTypesProto.javaDescriptor.getMessageTypes().get(11)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = RhoTypesProto.scalaDescriptor.messages(11)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = coop.rchain.models.ReceiveBind
      case 2 => __out = coop.rchain.models.Par
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = coop.rchain.models.Receive(
    binds = _root_.scala.Seq.empty,
    body = coop.rchain.models.Par.defaultInstance,
    persistent = false,
    peek = false,
    bindCount = 0,
    locallyFree = coop.rchain.models.Receive._typemapper_locallyFree.toCustom(_root_.com.google.protobuf.ByteString.EMPTY),
    connectiveUsed = false
  )
  implicit class ReceiveLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Receive]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.Receive](_l) {
    def binds: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[coop.rchain.models.ReceiveBind]] = field(_.binds)((c_, f_) => c_.copy(binds = f_))
    def body: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par] = field(_.body)((c_, f_) => c_.copy(body = f_))
    def persistent: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.persistent)((c_, f_) => c_.copy(persistent = f_))
    def peek: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.peek)((c_, f_) => c_.copy(peek = f_))
    def bindCount: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Int] = field(_.bindCount)((c_, f_) => c_.copy(bindCount = f_))
    def locallyFree: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]] = field(_.locallyFree)((c_, f_) => c_.copy(locallyFree = f_))
    def connectiveUsed: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.connectiveUsed)((c_, f_) => c_.copy(connectiveUsed = f_))
  }
  final val BINDS_FIELD_NUMBER = 1
  final val BODY_FIELD_NUMBER = 2
  final val PERSISTENT_FIELD_NUMBER = 3
  final val PEEK_FIELD_NUMBER = 4
  final val BINDCOUNT_FIELD_NUMBER = 5
  final val LOCALLYFREE_FIELD_NUMBER = 6
  final val CONNECTIVE_USED_FIELD_NUMBER = 7
  @transient
  private[models] val _typemapper_locallyFree: _root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]] = implicitly[_root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]]]
  def of(
    binds: _root_.scala.Seq[coop.rchain.models.ReceiveBind],
    body: coop.rchain.models.Par,
    persistent: _root_.scala.Boolean,
    peek: _root_.scala.Boolean,
    bindCount: _root_.scala.Int,
    locallyFree: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet],
    connectiveUsed: _root_.scala.Boolean
  ): _root_.coop.rchain.models.Receive = _root_.coop.rchain.models.Receive(
    binds,
    body,
    persistent,
    peek,
    bindCount,
    locallyFree,
    connectiveUsed
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[Receive])
}
