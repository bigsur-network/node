package squeryl.tables

import org.squeryl.KeyedEntity

trait DbTable extends KeyedEntity[Long] {
  def name: String = this.getClass.getSimpleName.takeWhile(_ != '$').replace("Table", "")
}
