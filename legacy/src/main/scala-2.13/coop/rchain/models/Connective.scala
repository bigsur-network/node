// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models

@SerialVersionUID(0L)
final case class Connective(
    connectiveInstance: coop.rchain.models.Connective.ConnectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.Empty
    ) extends coop.rchain.models.StacksafeMessage[Connective] with scalapb.lenses.Updatable[Connective] {

    def mergeFromM[F[_]: cats.effect.Sync](`_input__`: _root_.com.google.protobuf.CodedInputStream): F[coop.rchain.models.Connective] = {
      
      import cats.effect.Sync
      import cats.syntax.all.*
      
      Sync[F].defer {
        var __connectiveInstance = this.connectiveInstance
        var _done__ = false
        
        Sync[F].whileM_ (Sync[F].delay { !_done__ }) {
          for {
            _tag__ <- Sync[F].delay { _input__.readTag() }
            _ <- _tag__ match {
              case 0 => Sync[F].delay { _done__ = true }
              case 10 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, connectiveInstance.connAndBody.getOrElse(coop.rchain.models.ConnectiveBody.defaultInstance))
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnAndBody(customTypeValue) }
                } yield ()
              case 18 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, connectiveInstance.connOrBody.getOrElse(coop.rchain.models.ConnectiveBody.defaultInstance))
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnOrBody(customTypeValue) }
                } yield ()
              case 26 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, connectiveInstance.connNotBody.getOrElse(coop.rchain.models.Par.defaultInstance))
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnNotBody(customTypeValue) }
                } yield ()
              case 34 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, connectiveInstance.varRefBody.getOrElse(coop.rchain.models.VarRef.defaultInstance))
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.VarRefBody(customTypeValue) }
                } yield ()
              case 40 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBool(customTypeValue) }
                } yield ()
              case 48 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnInt(customTypeValue) }
                } yield ()
              case 80 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBigInt(customTypeValue) }
                } yield ()
              case 56 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnString(customTypeValue) }
                } yield ()
              case 64 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnUri(customTypeValue) }
                } yield ()
              case 72 =>
                for {
                  readValue       <- Sync[F].delay { _input__.readBool() }
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnByteArray(customTypeValue) }
                } yield ()
            case tag => Sync[F].delay { _input__.skipField(tag) }
            }
          } yield ()
        }
        .map { _ => coop.rchain.models.Connective(
          connectiveInstance = __connectiveInstance
        )}
      }
    }
    
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      if (connectiveInstance.connAndBody.isDefined) {
        val __value = connectiveInstance.connAndBody.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      if (connectiveInstance.connOrBody.isDefined) {
        val __value = connectiveInstance.connOrBody.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      if (connectiveInstance.connNotBody.isDefined) {
        val __value = connectiveInstance.connNotBody.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      if (connectiveInstance.varRefBody.isDefined) {
        val __value = connectiveInstance.varRefBody.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      if (connectiveInstance.connBool.isDefined) {
        val __value = connectiveInstance.connBool.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(5, __value)
      };
      if (connectiveInstance.connInt.isDefined) {
        val __value = connectiveInstance.connInt.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(6, __value)
      };
      if (connectiveInstance.connBigInt.isDefined) {
        val __value = connectiveInstance.connBigInt.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(10, __value)
      };
      if (connectiveInstance.connString.isDefined) {
        val __value = connectiveInstance.connString.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(7, __value)
      };
      if (connectiveInstance.connUri.isDefined) {
        val __value = connectiveInstance.connUri.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(8, __value)
      };
      if (connectiveInstance.connByteArray.isDefined) {
        val __value = connectiveInstance.connByteArray.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(9, __value)
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
      connectiveInstance.connAndBody.foreach { __v =>
        val __m = __v
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      connectiveInstance.connOrBody.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      connectiveInstance.connNotBody.foreach { __v =>
        val __m = __v
        _output__.writeTag(3, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      connectiveInstance.varRefBody.foreach { __v =>
        val __m = __v
        _output__.writeTag(4, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      connectiveInstance.connBool.foreach { __v =>
        val __m = __v
        _output__.writeBool(5, __m)
      };
      connectiveInstance.connInt.foreach { __v =>
        val __m = __v
        _output__.writeBool(6, __m)
      };
      connectiveInstance.connString.foreach { __v =>
        val __m = __v
        _output__.writeBool(7, __m)
      };
      connectiveInstance.connUri.foreach { __v =>
        val __m = __v
        _output__.writeBool(8, __m)
      };
      connectiveInstance.connByteArray.foreach { __v =>
        val __m = __v
        _output__.writeBool(9, __m)
      };
      connectiveInstance.connBigInt.foreach { __v =>
        val __m = __v
        _output__.writeBool(10, __m)
      };
    }
    def getConnAndBody: coop.rchain.models.ConnectiveBody = connectiveInstance.connAndBody.getOrElse(coop.rchain.models.ConnectiveBody.defaultInstance)
    def withConnAndBody(__v: coop.rchain.models.ConnectiveBody): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnAndBody(__v))
    def getConnOrBody: coop.rchain.models.ConnectiveBody = connectiveInstance.connOrBody.getOrElse(coop.rchain.models.ConnectiveBody.defaultInstance)
    def withConnOrBody(__v: coop.rchain.models.ConnectiveBody): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnOrBody(__v))
    def getConnNotBody: coop.rchain.models.Par = connectiveInstance.connNotBody.getOrElse(coop.rchain.models.Par.defaultInstance)
    def withConnNotBody(__v: coop.rchain.models.Par): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnNotBody(__v))
    def getVarRefBody: coop.rchain.models.VarRef = connectiveInstance.varRefBody.getOrElse(coop.rchain.models.VarRef.defaultInstance)
    def withVarRefBody(__v: coop.rchain.models.VarRef): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.VarRefBody(__v))
    def getConnBool: _root_.scala.Boolean = connectiveInstance.connBool.getOrElse(false)
    def withConnBool(__v: _root_.scala.Boolean): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBool(__v))
    def getConnInt: _root_.scala.Boolean = connectiveInstance.connInt.getOrElse(false)
    def withConnInt(__v: _root_.scala.Boolean): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnInt(__v))
    def getConnBigInt: _root_.scala.Boolean = connectiveInstance.connBigInt.getOrElse(false)
    def withConnBigInt(__v: _root_.scala.Boolean): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBigInt(__v))
    def getConnString: _root_.scala.Boolean = connectiveInstance.connString.getOrElse(false)
    def withConnString(__v: _root_.scala.Boolean): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnString(__v))
    def getConnUri: _root_.scala.Boolean = connectiveInstance.connUri.getOrElse(false)
    def withConnUri(__v: _root_.scala.Boolean): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnUri(__v))
    def getConnByteArray: _root_.scala.Boolean = connectiveInstance.connByteArray.getOrElse(false)
    def withConnByteArray(__v: _root_.scala.Boolean): Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnByteArray(__v))
    def clearConnectiveInstance: Connective = copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.Empty)
    def withConnectiveInstance(__v: coop.rchain.models.Connective.ConnectiveInstance): Connective = copy(connectiveInstance = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => connectiveInstance.connAndBody.orNull
        case 2 => connectiveInstance.connOrBody.orNull
        case 3 => connectiveInstance.connNotBody.orNull
        case 4 => connectiveInstance.varRefBody.orNull
        case 5 => connectiveInstance.connBool.orNull
        case 6 => connectiveInstance.connInt.orNull
        case 10 => connectiveInstance.connBigInt.orNull
        case 7 => connectiveInstance.connString.orNull
        case 8 => connectiveInstance.connUri.orNull
        case 9 => connectiveInstance.connByteArray.orNull
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => connectiveInstance.connAndBody.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 2 => connectiveInstance.connOrBody.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 3 => connectiveInstance.connNotBody.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 4 => connectiveInstance.varRefBody.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 5 => connectiveInstance.connBool.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 6 => connectiveInstance.connInt.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 10 => connectiveInstance.connBigInt.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 7 => connectiveInstance.connString.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 8 => connectiveInstance.connUri.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 9 => connectiveInstance.connByteArray.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.Connective.type = coop.rchain.models.Connective
    // @@protoc_insertion_point(GeneratedMessage[Connective])
}

object Connective extends scalapb.GeneratedMessageCompanion[coop.rchain.models.Connective] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.Connective] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.Connective = {
    var __connectiveInstance: coop.rchain.models.Connective.ConnectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.Empty
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnAndBody(__connectiveInstance.connAndBody.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.ConnectiveBody](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 18 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnOrBody(__connectiveInstance.connOrBody.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.ConnectiveBody](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 26 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnNotBody(__connectiveInstance.connNotBody.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 34 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.VarRefBody(__connectiveInstance.varRefBody.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.VarRef](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 40 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBool(_input__.readBool())
        case 48 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnInt(_input__.readBool())
        case 80 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBigInt(_input__.readBool())
        case 56 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnString(_input__.readBool())
        case 64 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnUri(_input__.readBool())
        case 72 =>
          __connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnByteArray(_input__.readBool())
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.Connective(
        connectiveInstance = __connectiveInstance
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.Connective] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      coop.rchain.models.Connective(
        connectiveInstance = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[_root_.scala.Option[coop.rchain.models.ConnectiveBody]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnAndBody(_))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[coop.rchain.models.ConnectiveBody]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnOrBody(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[_root_.scala.Option[coop.rchain.models.Par]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnNotBody(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[_root_.scala.Option[coop.rchain.models.VarRef]]).map(coop.rchain.models.Connective.ConnectiveInstance.VarRefBody(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnBool(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnInt(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(10).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnBigInt(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnString(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnUri(_)))
            .orElse[coop.rchain.models.Connective.ConnectiveInstance](__fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(coop.rchain.models.Connective.ConnectiveInstance.ConnByteArray(_)))
            .getOrElse(coop.rchain.models.Connective.ConnectiveInstance.Empty)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = RhoTypesProto.javaDescriptor.getMessageTypes().get(44)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = RhoTypesProto.scalaDescriptor.messages(44)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = coop.rchain.models.ConnectiveBody
      case 2 => __out = coop.rchain.models.ConnectiveBody
      case 3 => __out = coop.rchain.models.Par
      case 4 => __out = coop.rchain.models.VarRef
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = coop.rchain.models.Connective(
    connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.Empty
  )
  sealed trait ConnectiveInstance extends _root_.scalapb.GeneratedOneof {
    def isEmpty: _root_.scala.Boolean = false
    def isDefined: _root_.scala.Boolean = true
    def isConnAndBody: _root_.scala.Boolean = false
    def isConnOrBody: _root_.scala.Boolean = false
    def isConnNotBody: _root_.scala.Boolean = false
    def isVarRefBody: _root_.scala.Boolean = false
    def isConnBool: _root_.scala.Boolean = false
    def isConnInt: _root_.scala.Boolean = false
    def isConnBigInt: _root_.scala.Boolean = false
    def isConnString: _root_.scala.Boolean = false
    def isConnUri: _root_.scala.Boolean = false
    def isConnByteArray: _root_.scala.Boolean = false
    def connAndBody: _root_.scala.Option[coop.rchain.models.ConnectiveBody] = _root_.scala.None
    def connOrBody: _root_.scala.Option[coop.rchain.models.ConnectiveBody] = _root_.scala.None
    def connNotBody: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None
    def varRefBody: _root_.scala.Option[coop.rchain.models.VarRef] = _root_.scala.None
    def connBool: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
    def connInt: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
    def connBigInt: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
    def connString: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
    def connUri: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
    def connByteArray: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
  }
  object ConnectiveInstance {
    @SerialVersionUID(0L)
    case object Empty extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Nothing
      override def isEmpty: _root_.scala.Boolean = true
      override def isDefined: _root_.scala.Boolean = false
      override def number: _root_.scala.Int = 0
      override def value: _root_.scala.Nothing = throw new java.util.NoSuchElementException("Empty.value")
    }
  
    @SerialVersionUID(0L)
    final case class ConnAndBody(value: coop.rchain.models.ConnectiveBody) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = coop.rchain.models.ConnectiveBody
      override def isConnAndBody: _root_.scala.Boolean = true
      override def connAndBody: _root_.scala.Option[coop.rchain.models.ConnectiveBody] = Some(value)
      override def number: _root_.scala.Int = 1
    }
    @SerialVersionUID(0L)
    final case class ConnOrBody(value: coop.rchain.models.ConnectiveBody) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = coop.rchain.models.ConnectiveBody
      override def isConnOrBody: _root_.scala.Boolean = true
      override def connOrBody: _root_.scala.Option[coop.rchain.models.ConnectiveBody] = Some(value)
      override def number: _root_.scala.Int = 2
    }
    @SerialVersionUID(0L)
    final case class ConnNotBody(value: coop.rchain.models.Par) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = coop.rchain.models.Par
      override def isConnNotBody: _root_.scala.Boolean = true
      override def connNotBody: _root_.scala.Option[coop.rchain.models.Par] = Some(value)
      override def number: _root_.scala.Int = 3
    }
    @SerialVersionUID(0L)
    final case class VarRefBody(value: coop.rchain.models.VarRef) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = coop.rchain.models.VarRef
      override def isVarRefBody: _root_.scala.Boolean = true
      override def varRefBody: _root_.scala.Option[coop.rchain.models.VarRef] = Some(value)
      override def number: _root_.scala.Int = 4
    }
    @SerialVersionUID(0L)
    final case class ConnBool(value: _root_.scala.Boolean) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Boolean
      override def isConnBool: _root_.scala.Boolean = true
      override def connBool: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
      override def number: _root_.scala.Int = 5
    }
    @SerialVersionUID(0L)
    final case class ConnInt(value: _root_.scala.Boolean) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Boolean
      override def isConnInt: _root_.scala.Boolean = true
      override def connInt: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
      override def number: _root_.scala.Int = 6
    }
    @SerialVersionUID(0L)
    final case class ConnBigInt(value: _root_.scala.Boolean) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Boolean
      override def isConnBigInt: _root_.scala.Boolean = true
      override def connBigInt: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
      override def number: _root_.scala.Int = 10
    }
    @SerialVersionUID(0L)
    final case class ConnString(value: _root_.scala.Boolean) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Boolean
      override def isConnString: _root_.scala.Boolean = true
      override def connString: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
      override def number: _root_.scala.Int = 7
    }
    @SerialVersionUID(0L)
    final case class ConnUri(value: _root_.scala.Boolean) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Boolean
      override def isConnUri: _root_.scala.Boolean = true
      override def connUri: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
      override def number: _root_.scala.Int = 8
    }
    @SerialVersionUID(0L)
    final case class ConnByteArray(value: _root_.scala.Boolean) extends coop.rchain.models.Connective.ConnectiveInstance {
      type ValueType = _root_.scala.Boolean
      override def isConnByteArray: _root_.scala.Boolean = true
      override def connByteArray: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
      override def number: _root_.scala.Int = 9
    }
  }
  implicit class ConnectiveLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Connective]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.Connective](_l) {
    def connAndBody: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.ConnectiveBody] = field(_.getConnAndBody)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnAndBody(f_)))
    def connOrBody: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.ConnectiveBody] = field(_.getConnOrBody)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnOrBody(f_)))
    def connNotBody: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par] = field(_.getConnNotBody)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnNotBody(f_)))
    def varRefBody: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.VarRef] = field(_.getVarRefBody)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.VarRefBody(f_)))
    def connBool: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getConnBool)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBool(f_)))
    def connInt: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getConnInt)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnInt(f_)))
    def connBigInt: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getConnBigInt)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnBigInt(f_)))
    def connString: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getConnString)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnString(f_)))
    def connUri: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getConnUri)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnUri(f_)))
    def connByteArray: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getConnByteArray)((c_, f_) => c_.copy(connectiveInstance = coop.rchain.models.Connective.ConnectiveInstance.ConnByteArray(f_)))
    def connectiveInstance: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Connective.ConnectiveInstance] = field(_.connectiveInstance)((c_, f_) => c_.copy(connectiveInstance = f_))
  }
  final val CONN_AND_BODY_FIELD_NUMBER = 1
  final val CONN_OR_BODY_FIELD_NUMBER = 2
  final val CONN_NOT_BODY_FIELD_NUMBER = 3
  final val VAR_REF_BODY_FIELD_NUMBER = 4
  final val CONN_BOOL_FIELD_NUMBER = 5
  final val CONN_INT_FIELD_NUMBER = 6
  final val CONN_BIG_INT_FIELD_NUMBER = 10
  final val CONN_STRING_FIELD_NUMBER = 7
  final val CONN_URI_FIELD_NUMBER = 8
  final val CONN_BYTE_ARRAY_FIELD_NUMBER = 9
  def of(
    connectiveInstance: coop.rchain.models.Connective.ConnectiveInstance
  ): _root_.coop.rchain.models.Connective = _root_.coop.rchain.models.Connective(
    connectiveInstance
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[Connective])
}
