package io.rhonix.rholang.normalizer.envimpl

import coop.rchain.rholang.interpreter.compiler.SourcePosition
import io.rhonix.rholang.normalizer.env.VarContext

/** Map that associates variable names with their context, including de Bruijn index, type, and source position. */
final case class VarMap[T](data: Map[String, VarContext[T]], nextIndex: Int) {

  /**
   * Retrieve the variable context by its name.
   *
   * @return Some(varContext) if the variable is found, None otherwise.
   */
  def get(name: String): Option[VarContext[T]] = data.get(name).map(v => v.copy(indexRev = nextIndex - v.index - 1))

  /**
   * Adds a new variable to the map or update an existing one.
   *
   * @param name the name of the variable.
   * @param sort the type of the variable.
   * @param sourcePosition the source position of the variable.
   * @return a new VarMap with the updated data and next index.
   */
  def put(name: String, sort: T, sourcePosition: SourcePosition): (VarMap[T], VarContext[T]) = {
    // NOTE: Reverse index is not calculated here, it's temporary set to -1.
    val varData  = VarContext(nextIndex, -1, sort, sourcePosition)
    val newMap   = data.updated(name, varData)
    val newIndex = nextIndex + 1
    (VarMap(newMap, newIndex), varData)
  }

  /**
   * Creates new variables without names (internally increases next index).
   *
   * @param vars type of variables to add.
   * @return references of added variables.
   */
  def create(vars: Seq[(T, SourcePosition)]): (VarMap[T], Seq[VarContext[T]]) = {
    val newNextIdx = nextIndex + vars.size
    // NOTE: Reverse index is not calculated here, it's temporary set to -1.
    val addedVars  = vars.zipWithIndex.map { case ((t, pos), i) => VarContext(i, -1, t, pos) }
    (copy(nextIndex = newNextIdx), addedVars)
  }
}

object VarMap {

  /**
   * Creates a default instance of VarMap.
   *
   * @return a new VarMap with an empty data map and a next index of 0
   */
  def default[T]: VarMap[T] = VarMap(Map[String, VarContext[T]](), 0)

  /**
   * Create a VarMap with the given data.
   *
   * @param initData the data to initialize the VarMap with
   * @return a new VarMap with the given data and a next index of 0
   */
  def apply[T](initData: Seq[(String, T, SourcePosition)]): VarMap[T] = initData.foldLeft(default[T]) {
    case (varMap, data) => varMap.put(data._1, data._2, data._3)._1
  }
}
