// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models
import coop.rchain.models.BitSetBytesMapper.bitSetBytesMapper

/** Number of variables bound in the new statement.
  * For normalized form, p should not contain solely another new.
  * Also for normalized form, the first use should be level+0, next use level+1
  * up to level+count for the last used variable.
  *
  * @param bindCount
  *   Includes any uris listed below. This makes it easier to substitute or walk a term.
  * @param uri
  *   For normalization, uri-referenced variables come at the end, and in lexicographical order.
  */
@SerialVersionUID(0L)
final case class New(
  bindCount: _root_.scala.Int = 0,
  p: coop.rchain.models.Par = coop.rchain.models.Par.defaultInstance,
  uri: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
  injections: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, coop.rchain.models.Par] =
    _root_.scala.collection.immutable.Map.empty,
  locallyFree: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet] =
    coop.rchain.models.New._typemapper_locallyFree.toCustom(_root_.com.google.protobuf.ByteString.EMPTY),
) extends coop.rchain.models.StacksafeMessage[New]
    with scalapb.lenses.Updatable[New] {

  def mergeFromM[F[_]: cats.effect.Sync](
    `_input__`: _root_.com.google.protobuf.CodedInputStream,
  ): F[coop.rchain.models.New] = {

    import cats.effect.Sync
    import cats.syntax.all.*

    Sync[F].defer {
      var __bindCount   = this.bindCount
      var __p           = this.p
      val __uri         = new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] ++= this.uri
      val __injections  = scala.collection.immutable.Map
        .newBuilder[_root_.scala.Predef.String, coop.rchain.models.Par] ++= this.injections
      var __locallyFree = this.locallyFree
      var _done__       = false

      Sync[F]
        .whileM_(Sync[F].delay(!_done__)) {
          for {
            _tag__ <- Sync[F].delay(_input__.readTag())
            _      <- _tag__ match {
                        case 0   => Sync[F].delay { _done__ = true }
                        case 8   =>
                          for {
                            readValue      <- Sync[F].delay(_input__.readSInt32())
                            customTypeValue = readValue
                            _              <- Sync[F].delay { __bindCount = customTypeValue }
                          } yield ()
                        case 18  =>
                          for {
                            readValue      <- coop.rchain.models.SafeParser.readMessage(_input__, __p)
                            customTypeValue = readValue
                            _              <- Sync[F].delay { __p = customTypeValue }
                          } yield ()
                        case 26  =>
                          for {
                            readValue      <- Sync[F].delay(_input__.readStringRequireUtf8())
                            customTypeValue = readValue
                            _              <- Sync[F].delay(__uri += customTypeValue)
                          } yield ()
                        case 34  =>
                          for {
                            readValue      <- coop.rchain.models.SafeParser
                                                .readMessage(_input__, coop.rchain.models.New.InjectionsEntry.defaultInstance)
                            customTypeValue = coop.rchain.models.New._typemapper_injections.toCustom(readValue)
                            _              <- Sync[F].delay(__injections += customTypeValue)
                          } yield ()
                        case 42  =>
                          for {
                            readValue      <- Sync[F].delay(_input__.readBytes())
                            customTypeValue = coop.rchain.models.New._typemapper_locallyFree.toCustom(readValue)
                            _              <- Sync[F].delay { __locallyFree = customTypeValue }
                          } yield ()
                        case tag => Sync[F].delay(_input__.skipField(tag))
                      }
          } yield ()
        }
        .map { _ =>
          coop.rchain.models.New(
            bindCount = __bindCount,
            p = __p,
            uri = __uri.result(),
            injections = __injections.result(),
            locallyFree = __locallyFree,
          )
        }
    }
  }

  @transient
  private[this] var __serializedSizeMemoized: _root_.scala.Int  = 0
  private[this] def __computeSerializedSize(): _root_.scala.Int = {
    var __size = 0

    {
      val __value = bindCount
      if (__value != 0) {
        __size += _root_.com.google.protobuf.CodedOutputStream.computeSInt32Size(1, __value)
      }
    };

    {
      val __value = p
      if (__value.serializedSize != 0) {
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream
          .computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
    };
    uri.foreach { __item =>
      val __value = __item
      __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, __value)
    }
    injections.foreach { __item =>
      val __value = coop.rchain.models.New._typemapper_injections.toBase(__item)
      __size += 1 + _root_.com.google.protobuf.CodedOutputStream
        .computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
    }

    {
      val __value = coop.rchain.models.New._typemapper_locallyFree.toBase(locallyFree)
      if (!__value.isEmpty) {
        __size += _root_.com.google.protobuf.CodedOutputStream.computeBytesSize(5, __value)
      }
    };
    __size
  }
  override def serializedSize: _root_.scala.Int                 = {
    var __size = __serializedSizeMemoized
    if (__size == 0) {
      __size = __computeSerializedSize() + 1
      __serializedSizeMemoized = __size
    }
    __size - 1

  }

  @transient var _serializedSizeM: coop.rchain.models.Memo[Int] = null

  def serializedSizeM: coop.rchain.models.Memo[Int]                                                    = synchronized {
    if (_serializedSizeM == null) {
      _serializedSizeM = new coop.rchain.models.Memo(coop.rchain.models.ProtoM.serializedSize(this))
      _serializedSizeM
    } else _serializedSizeM
  }
  def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit            = {
    {
      val __v = bindCount
      if (__v != 0) {
        _output__.writeSInt32(1, __v)
      }
    };
    {
      val __v = p
      if (__v.serializedSize != 0) {
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__v.serializedSize)
        __v.writeTo(_output__)
      }
    };
    uri.foreach { __v =>
      val __m = __v
      _output__.writeString(3, __m)
    };
    injections.foreach { __v =>
      val __m = coop.rchain.models.New._typemapper_injections.toBase(__v)
      _output__.writeTag(4, 2)
      _output__.writeUInt32NoTag(__m.serializedSize)
      __m.writeTo(_output__)
    };
    {
      val __v = coop.rchain.models.New._typemapper_locallyFree.toBase(locallyFree)
      if (!__v.isEmpty) {
        _output__.writeBytes(5, __v)
      }
    };
  }
  def withBindCount(__v: _root_.scala.Int): New                                                        = copy(bindCount = __v)
  def withP(__v: coop.rchain.models.Par): New                                                          = copy(p = __v)
  def clearUri                                                                                         = copy(uri = _root_.scala.Seq.empty)
  def addUri(__vs: _root_.scala.Predef.String*): New                                                   = addAllUri(__vs)
  def addAllUri(__vs: Iterable[_root_.scala.Predef.String]): New                                       = copy(uri = uri ++ __vs)
  def withUri(__v: _root_.scala.Seq[_root_.scala.Predef.String]): New                                  = copy(uri = __v)
  def clearInjections                                                                                  = copy(injections = _root_.scala.collection.immutable.Map.empty)
  def addInjections(__vs: (_root_.scala.Predef.String, coop.rchain.models.Par)*): New                  = addAllInjections(__vs)
  def addAllInjections(__vs: Iterable[(_root_.scala.Predef.String, coop.rchain.models.Par)]): New      =
    copy(injections = injections ++ __vs)
  def withInjections(
    __v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, coop.rchain.models.Par],
  ): New = copy(injections = __v)
  def withLocallyFree(__v: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]): New     =
    copy(locallyFree = __v)
  def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any                              =
    (__fieldNumber: @ _root_.scala.unchecked) match {
      case 1 =>
        val __t = bindCount
        if (__t != 0) __t else null
      case 2 =>
        val __t = p
        if (__t != coop.rchain.models.Par.defaultInstance) __t else null
      case 3 => uri
      case 4 => injections.iterator.map(coop.rchain.models.New._typemapper_injections.toBase(_)).toSeq
      case 5 =>
        val __t = coop.rchain.models.New._typemapper_locallyFree.toBase(locallyFree)
        if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
    }
  def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
    _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
    (__field.number: @ _root_.scala.unchecked) match {
      case 1 => _root_.scalapb.descriptors.PInt(bindCount)
      case 2 => p.toPMessage
      case 3 => _root_.scalapb.descriptors.PRepeated(uri.iterator.map(_root_.scalapb.descriptors.PString(_)).toVector)
      case 4 =>
        _root_.scalapb.descriptors.PRepeated(
          injections.iterator.map(coop.rchain.models.New._typemapper_injections.toBase(_).toPMessage).toVector,
        )
      case 5 =>
        _root_.scalapb.descriptors.PByteString(coop.rchain.models.New._typemapper_locallyFree.toBase(locallyFree))
    }
  }
  def toProtoString: _root_.scala.Predef.String                                                        = _root_.scalapb.TextFormat.printToUnicodeString(this)
  def companion: coop.rchain.models.New.type                                                           = coop.rchain.models.New
  // @@protoc_insertion_point(GeneratedMessage[New])
}

object New extends scalapb.GeneratedMessageCompanion[coop.rchain.models.New] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.New]                    = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.New              = {
    var __bindCount: _root_.scala.Int                                                      = 0
    var __p: _root_.scala.Option[coop.rchain.models.Par]                                   = _root_.scala.None
    val __uri: _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] =
      new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String]
    val __injections: _root_.scala.collection.mutable.Builder[
      (_root_.scala.Predef.String, coop.rchain.models.Par),
      _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, coop.rchain.models.Par],
    ] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, coop.rchain.models.Par]
    var __locallyFree: _root_.com.google.protobuf.ByteString                               = _root_.com.google.protobuf.ByteString.EMPTY
    var _done__                                                                            = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0   => _done__ = true
        case 8   =>
          __bindCount = _input__.readSInt32()
        case 18  =>
          __p = _root_.scala.Some(
            __p.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(
              _root_.scalapb.LiteParser.readMessage(_input__, _),
            ),
          )
        case 26  =>
          __uri += _input__.readStringRequireUtf8()
        case 34  =>
          __injections += coop.rchain.models.New._typemapper_injections
            .toCustom(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.New.InjectionsEntry](_input__))
        case 42  =>
          __locallyFree = _input__.readBytes()
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.New(
      bindCount = __bindCount,
      p = __p.getOrElse(coop.rchain.models.Par.defaultInstance),
      uri = __uri.result(),
      injections = __injections.result(),
      locallyFree = coop.rchain.models.New._typemapper_locallyFree.toCustom(__locallyFree),
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.New]                         =
    _root_.scalapb.descriptors.Reads {
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(
          __fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor),
          "FieldDescriptor does not match message type.",
        )
        coop.rchain.models.New(
          bindCount =
            __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Int]).getOrElse(0),
          p = __fieldsMap
            .get(scalaDescriptor.findFieldByNumber(2).get)
            .map(_.as[coop.rchain.models.Par])
            .getOrElse(coop.rchain.models.Par.defaultInstance),
          uri = __fieldsMap
            .get(scalaDescriptor.findFieldByNumber(3).get)
            .map(_.as[_root_.scala.Seq[_root_.scala.Predef.String]])
            .getOrElse(_root_.scala.Seq.empty),
          injections = __fieldsMap
            .get(scalaDescriptor.findFieldByNumber(4).get)
            .map(_.as[_root_.scala.Seq[coop.rchain.models.New.InjectionsEntry]])
            .getOrElse(_root_.scala.Seq.empty)
            .iterator
            .map(coop.rchain.models.New._typemapper_injections.toCustom(_))
            .toMap,
          locallyFree = coop.rchain.models.New._typemapper_locallyFree.toCustom(
            __fieldsMap
              .get(scalaDescriptor.findFieldByNumber(5).get)
              .map(_.as[_root_.com.google.protobuf.ByteString])
              .getOrElse(_root_.com.google.protobuf.ByteString.EMPTY),
          ),
        )
      case _                                                => throw new RuntimeException("Expected PMessage")
    }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor                                       =
    RhoTypesProto.javaDescriptor.getMessageTypes().get(12)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor                                                  = RhoTypesProto.scalaDescriptor.messages(12)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @ _root_.scala.unchecked) match {
      case 2 => __out = coop.rchain.models.Par
      case 4 => __out = coop.rchain.models.New.InjectionsEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions
    : Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      _root_.coop.rchain.models.New.InjectionsEntry,
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_]  =
    throw new MatchError(__fieldNumber)
  lazy val defaultInstance                                                                                    = coop.rchain.models.New(
    bindCount = 0,
    p = coop.rchain.models.Par.defaultInstance,
    uri = _root_.scala.Seq.empty,
    injections = _root_.scala.collection.immutable.Map.empty,
    locallyFree = coop.rchain.models.New._typemapper_locallyFree.toCustom(_root_.com.google.protobuf.ByteString.EMPTY),
  )
  @SerialVersionUID(0L)
  final case class InjectionsEntry(
    key: _root_.scala.Predef.String = "",
    value: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None,
  ) extends coop.rchain.models.StacksafeMessage[InjectionsEntry]
      with scalapb.lenses.Updatable[InjectionsEntry] {

    def mergeFromM[F[_]: cats.effect.Sync](
      `_input__`: _root_.com.google.protobuf.CodedInputStream,
    ): F[coop.rchain.models.New.InjectionsEntry] = {

      import cats.effect.Sync
      import cats.syntax.all.*

      Sync[F].defer {
        var __key   = this.key
        var __value = this.value
        var _done__ = false

        Sync[F]
          .whileM_(Sync[F].delay(!_done__)) {
            for {
              _tag__ <- Sync[F].delay(_input__.readTag())
              _      <- _tag__ match {
                          case 0   => Sync[F].delay { _done__ = true }
                          case 10  =>
                            for {
                              readValue      <- Sync[F].delay(_input__.readStringRequireUtf8())
                              customTypeValue = readValue
                              _              <- Sync[F].delay { __key = customTypeValue }
                            } yield ()
                          case 18  =>
                            for {
                              readValue      <-
                                coop.rchain.models.SafeParser
                                  .readMessage(_input__, __value.getOrElse(coop.rchain.models.Par.defaultInstance))
                              customTypeValue = readValue
                              _              <- Sync[F].delay { __value = Option(customTypeValue) }
                            } yield ()
                          case tag => Sync[F].delay(_input__.skipField(tag))
                        }
            } yield ()
          }
          .map { _ =>
            coop.rchain.models.New.InjectionsEntry(
              key = __key,
              value = __value,
            )
          }
      }
    }

    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int  = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0

      {
        val __value = key
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      if (value.isDefined) {
        val __value = value.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream
          .computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      __size
    }
    override def serializedSize: _root_.scala.Int                 = {
      var __size = __serializedSizeMemoized
      if (__size == 0) {
        __size = __computeSerializedSize() + 1
        __serializedSizeMemoized = __size
      }
      __size - 1

    }

    @transient var _serializedSizeM: coop.rchain.models.Memo[Int] = null

    def serializedSizeM: coop.rchain.models.Memo[Int]                                                    = synchronized {
      if (_serializedSizeM == null) {
        _serializedSizeM = new coop.rchain.models.Memo(coop.rchain.models.ProtoM.serializedSize(this))
        _serializedSizeM
      } else _serializedSizeM
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit            = {
      {
        val __v = key
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      value.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
    }
    def withKey(__v: _root_.scala.Predef.String): InjectionsEntry                                        = copy(key = __v)
    def getValue: coop.rchain.models.Par                                                                 = value.getOrElse(coop.rchain.models.Par.defaultInstance)
    def clearValue: InjectionsEntry                                                                      = copy(value = _root_.scala.None)
    def withValue(__v: coop.rchain.models.Par): InjectionsEntry                                          = copy(value = Option(__v))
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any                              =
      (__fieldNumber: @ _root_.scala.unchecked) match {
        case 1 =>
          val __t = key
          if (__t != "") __t else null
        case 2 => value.orNull
      }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @ _root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(key)
        case 2 => value.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
      }
    }
    def toProtoString: _root_.scala.Predef.String                                                        = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.New.InjectionsEntry.type                                           = coop.rchain.models.New.InjectionsEntry
    // @@protoc_insertion_point(GeneratedMessage[New.InjectionsEntry])
  }

  object InjectionsEntry extends scalapb.GeneratedMessageCompanion[coop.rchain.models.New.InjectionsEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.New.InjectionsEntry]       = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.New.InjectionsEntry = {
      var __key: _root_.scala.Predef.String                    = ""
      var __value: _root_.scala.Option[coop.rchain.models.Par] = _root_.scala.None
      var _done__                                              = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0   => _done__ = true
          case 10  =>
            __key = _input__.readStringRequireUtf8()
          case 18  =>
            __value = Option(
              __value.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Par](_input__))(
                _root_.scalapb.LiteParser.readMessage(_input__, _),
              ),
            )
          case tag => _input__.skipField(tag)
        }
      }
      coop.rchain.models.New.InjectionsEntry(
        key = __key,
        value = __value,
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.New.InjectionsEntry]            =
      _root_.scalapb.descriptors.Reads {
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          _root_.scala.Predef.require(
            __fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor),
            "FieldDescriptor does not match message type.",
          )
          coop.rchain.models.New.InjectionsEntry(
            key = __fieldsMap
              .get(scalaDescriptor.findFieldByNumber(1).get)
              .map(_.as[_root_.scala.Predef.String])
              .getOrElse(""),
            value = __fieldsMap
              .get(scalaDescriptor.findFieldByNumber(2).get)
              .flatMap(_.as[_root_.scala.Option[coop.rchain.models.Par]]),
          )
        case _                                                => throw new RuntimeException("Expected PMessage")
      }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor                                          =
      coop.rchain.models.New.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor                                                     =
      coop.rchain.models.New.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_]    = {
      var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
      (__number: @ _root_.scala.unchecked) match {
        case 2 => __out = coop.rchain.models.Par
      }
      __out
    }
    lazy val nestedMessagesCompanions
      : Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_]     =
      throw new MatchError(__fieldNumber)
    lazy val defaultInstance                                                                                       = coop.rchain.models.New.InjectionsEntry(
      key = "",
      value = _root_.scala.None,
    )
    implicit class InjectionsEntryLens[UpperPB](
      _l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.New.InjectionsEntry],
    ) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.New.InjectionsEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String]                            =
        field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par]                              =
        field(_.getValue)((c_, f_) => c_.copy(value = Option(f_)))
      def optionalValue: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[coop.rchain.models.Par]] =
        field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER                                                                                     = 1
    final val VALUE_FIELD_NUMBER                                                                                   = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[
      coop.rchain.models.New.InjectionsEntry,
      (_root_.scala.Predef.String, coop.rchain.models.Par),
    ] =
      _root_.scalapb
        .TypeMapper[coop.rchain.models.New.InjectionsEntry, (_root_.scala.Predef.String, coop.rchain.models.Par)](__m =>
          (__m.key, __m.getValue),
        )(__p => coop.rchain.models.New.InjectionsEntry(__p._1, Some(__p._2)))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.scala.Option[coop.rchain.models.Par],
    ): _root_.coop.rchain.models.New.InjectionsEntry = _root_.coop.rchain.models.New.InjectionsEntry(
      key,
      value,
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[New.InjectionsEntry])
  }

  implicit class NewLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.New])
      extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.New](_l) {
    def bindCount: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Int]                       =
      field(_.bindCount)((c_, f_) => c_.copy(bindCount = f_))
    def p: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Par]                         = field(_.p)((c_, f_) => c_.copy(p = f_))
    def uri: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[_root_.scala.Predef.String]] =
      field(_.uri)((c_, f_) => c_.copy(uri = f_))
    def injections: _root_.scalapb.lenses.Lens[
      UpperPB,
      _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, coop.rchain.models.Par],
    ] = field(_.injections)((c_, f_) => c_.copy(injections = f_))
    def locallyFree
      : _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet]] =
      field(_.locallyFree)((c_, f_) => c_.copy(locallyFree = f_))
  }
  final val BINDCOUNT_FIELD_NUMBER = 1
  final val P_FIELD_NUMBER           = 2
  final val URI_FIELD_NUMBER         = 3
  final val INJECTIONS_FIELD_NUMBER  = 4
  final val LOCALLYFREE_FIELD_NUMBER = 5
  @transient
  private[models] val _typemapper_injections: _root_.scalapb.TypeMapper[
    coop.rchain.models.New.InjectionsEntry,
    (_root_.scala.Predef.String, coop.rchain.models.Par),
  ] = implicitly[_root_.scalapb.TypeMapper[
    coop.rchain.models.New.InjectionsEntry,
    (_root_.scala.Predef.String, coop.rchain.models.Par),
  ]]
  @transient
  private[models] val _typemapper_locallyFree: _root_.scalapb.TypeMapper[
    _root_.com.google.protobuf.ByteString,
    coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet],
  ] = implicitly[_root_.scalapb.TypeMapper[_root_.com.google.protobuf.ByteString, coop.rchain.models.AlwaysEqual[
    scala.collection.immutable.BitSet,
  ]]]
  def of(
    bindCount: _root_.scala.Int,
    p: coop.rchain.models.Par,
    uri: _root_.scala.Seq[_root_.scala.Predef.String],
    injections: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, coop.rchain.models.Par],
    locallyFree: coop.rchain.models.AlwaysEqual[scala.collection.immutable.BitSet],
  ): _root_.coop.rchain.models.New = _root_.coop.rchain.models.New(
    bindCount,
    p,
    uri,
    injections,
    locallyFree,
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[New])
}
