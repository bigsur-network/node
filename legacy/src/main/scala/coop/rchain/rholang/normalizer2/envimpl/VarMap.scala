package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.normalizer2.env.VarContext

/** Map that associates variable names with their context, including de Bruijn index, type, and source position. */
final class VarMap[T](private val data: Map[String, VarContext[T]], private val nextIndex: Int) {

  /**
   * Retrieve the variable context by its name.
   * @return Some(varContext) if the variable is found, None otherwise.
   */
  def get(name: String): Option[VarContext[T]] = data.get(name)

  /**
   *  Retrieve all variables and their contexts.
   * @return a sequence of tuples, each containing a variable name and its context
   */
  def getAll: Seq[(String, VarContext[T])] = data.toSeq

  /**
   * Add a new variable to the map or update an existing one.
   * @param name the name of the variable
   * @param sort the type of the variable
   * @param sourcePosition the source position of the variable
   * @return a new VarMap with the updated data and next index
   */
  def put(name: String, sort: T, sourcePosition: SourcePosition): VarMap[T] = {
    val newData  = data.updated(name, VarContext(nextIndex, sort, sourcePosition))
    val newIndex = nextIndex + 1
    new VarMap(newData, newIndex)
  }
}

object VarMap {

  /**
   * Create an empty VarMap.
   * @return a new VarMap with an empty data map and a next index of 0
   */
  def empty[T]: VarMap[T] = new VarMap(Map[String, VarContext[T]](), 0)
}
