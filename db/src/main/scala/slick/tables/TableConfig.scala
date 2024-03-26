package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableConfig(tag: Tag) extends Table[(String, String)](tag, "config") {
  def name: Rep[String]  = column[String]("name", O.PrimaryKey)
  def value: Rep[String] = column[String]("value")

  def * : ProvenShape[(String, String)] = (name, value)
}
