package models

import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
case class Client(
                 codigo:Int,
                 apellidos: String,
                 nombres: String,
                 ruc:String,
                 lineaCredito:Double
                 )

class Clients(tag:Tag) extends Table[Client](tag, "cliente"){
  def codigo = column[Int]("codigo", O.PrimaryKey)
  def apellidos = column[String]("apellidos")
  def nombres = column[String]("nombres")
  def ruc = column[String]("ruc")
  def lineaCredito = column[Double]("linea_de_credito")
  def * = (codigo,apellidos,nombres,ruc,lineaCredito) <> (Client.tupled, Client.unapply)
}

object Clients{
  val table = TableQuery[Clients]
}
