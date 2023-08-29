// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package coop.rchain.models
import coop.rchain.models.BitSetBytesMapper.bitSetBytesMapper
import coop.rchain.models.ParSetTypeMapper.parSetESetTypeMapper
import coop.rchain.models.ParMapTypeMapper.parMapEMapTypeMapper
import coop.rchain.models.BigIntTypeMapper.bigIntBytesTypeMapper

/** A variable used as a var should be bound in a process context, not a name
  * context. For example:
  * `for (&#64;x &lt;- c1; &#64;y &lt;- c2) { z!(x + y) }` is fine, but
  * `for (x &lt;- c1; y &lt;- c2) { z!(x + y) }` should raise an error.
  */
@SerialVersionUID(0L)
final case class EVar(
    v: coop.rchain.models.Var = coop.rchain.models.Var.defaultInstance
    ) extends coop.rchain.models.StacksafeMessage[EVar] with scalapb.lenses.Updatable[EVar] {
    
    override def equals(x: Any): Boolean = {
    
      import coop.rchain.catscontrib.effect.implicits.sEval
    
     coop.rchain.models.EqualM[coop.rchain.models.EVar].equals[cats.Eval](this, x).value
    
    }
    
    override def hashCode(): Int = {
    
      import coop.rchain.catscontrib.effect.implicits.sEval
    
     coop.rchain.models.HashM[coop.rchain.models.EVar].hash[cats.Eval](this).value
    
    }
    
    
    def mergeFromM[F[_]: cats.effect.Sync](`_input__`: _root_.com.google.protobuf.CodedInputStream): F[coop.rchain.models.EVar] = {
      
      import cats.effect.Sync
      import cats.syntax.all._
      
      Sync[F].defer {
        var __v = this.v
        var _done__ = false
        
        Sync[F].whileM_ (Sync[F].delay { !_done__ }) {
          for {
            _tag__ <- Sync[F].delay { _input__.readTag() }
            _ <- _tag__ match {
              case 0 => Sync[F].delay { _done__ = true }
              case 10 =>
                for {
                  readValue       <- coop.rchain.models.SafeParser.readMessage(_input__, __v)
                  customTypeValue =  readValue
                  _               <- Sync[F].delay { __v = customTypeValue }
                } yield ()
            case tag => Sync[F].delay { _input__.skipField(tag) }
            }
          } yield ()
        }
        .map { _ => coop.rchain.models.EVar(
          v = __v
        )}
      }
    }
    
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = v
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
        val __v = v
        if (__v.serializedSize != 0) {
          _output__.writeTag(1, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      };
    }
    def withV(__v: coop.rchain.models.Var): EVar = copy(v = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = v
          if (__t != coop.rchain.models.Var.defaultInstance) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => v.toPMessage
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: coop.rchain.models.EVar.type = coop.rchain.models.EVar
    // @@protoc_insertion_point(GeneratedMessage[EVar])
}

object EVar extends scalapb.GeneratedMessageCompanion[coop.rchain.models.EVar] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[coop.rchain.models.EVar] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): coop.rchain.models.EVar = {
    var __v: _root_.scala.Option[coop.rchain.models.Var] = _root_.scala.None
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __v = _root_.scala.Some(__v.fold(_root_.scalapb.LiteParser.readMessage[coop.rchain.models.Var](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case tag => _input__.skipField(tag)
      }
    }
    coop.rchain.models.EVar(
        v = __v.getOrElse(coop.rchain.models.Var.defaultInstance)
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[coop.rchain.models.EVar] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      coop.rchain.models.EVar(
        v = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[coop.rchain.models.Var]).getOrElse(coop.rchain.models.Var.defaultInstance)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = RhoTypesProto.javaDescriptor.getMessageTypes().get(22)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = RhoTypesProto.scalaDescriptor.messages(22)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = coop.rchain.models.Var
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = coop.rchain.models.EVar(
    v = coop.rchain.models.Var.defaultInstance
  )
  implicit class EVarLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.EVar]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, coop.rchain.models.EVar](_l) {
    def v: _root_.scalapb.lenses.Lens[UpperPB, coop.rchain.models.Var] = field(_.v)((c_, f_) => c_.copy(v = f_))
  }
  final val V_FIELD_NUMBER = 1
  def of(
    v: coop.rchain.models.Var
  ): _root_.coop.rchain.models.EVar = _root_.coop.rchain.models.EVar(
    v
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[EVar])
}
