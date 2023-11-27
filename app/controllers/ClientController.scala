package controllers

import models.{Client, Clients}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
//import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/*la clase ClientController es un controlador de Play Framework que se encarga de manejar las solicitudes
relacionadas con la entidad Client. En este caso, el método getAllClients devuelve todos los clientes almacenados
en la base de datos en formato JSON. La inyección de dependencias y el uso de un singleton aseguran una gestión
eficiente de los recursos y la coherencia de las instancias del controlador en la aplicación.*/

@Singleton
class ClientController @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider,
 cc:ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile]
{
  import profile.api._
  implicit val clientFormat: Writes[Client] = Json.writes[Client]
  implicit val clientFormatR: Reads[Client] = Json.reads[Client]
  def getAllClients: Action[AnyContent] = Action.async {
    db.run(Clients.table.result).map { clientsList =>
      Ok(Json.toJson(clientsList))
    }
  }

  /*este método busca un cliente en la base de datos según su código, y la respuesta de la
  acción dependerá de si se encuentra o no el cliente. Si se encuentra, se devuelve el cliente en formato
  JSON con un código de estado Ok. Si no se encuentra, se devuelve un mensaje indicando que no se
  encontró el cliente con el código especificado y un código de estado NotFound.*/

  def getClientByCode(code:Int): Action[AnyContent] = Action.async{ implicit request=>
    val query = Clients.table.filter(_.codigo===code)
    val result:Future[Option[Client]] = db.run(query.result.headOption)
    result.map {
      case Some(cliente) => Ok(Json.toJson(cliente))
      case None => NotFound(s"No se encontró el cliente con el código $code")
    }
  }

  /*este método maneja la creación de un nuevo cliente. Valida el objeto JSON de la solicitud como un objeto Client,
  y si la validación tiene éxito, inserta el cliente en la base de datos y responde con un mensaje indicando que el
  cliente se ha creado con éxito. Si la validación falla, responde con un mensaje de error indicando que hay
  un problema en el formato JSON.*/

  def createClient(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val clientResult = request.body.validate[Client]

    clientResult.fold(
      errors => {
        println(s"Error de validación JSON: $errors")
        Future.successful(BadRequest(Json.obj("message" -> "Error en el formato JSON")))
      },
      client => {
        val insertQuery = Clients.table += client
        val result: Future[Int] = db.run(insertQuery)

        result.map { _ =>
          Ok(Json.obj("message" -> "Cliente creado con éxito"))
        }
      }
    )
  }

  /*este método maneja la actualización de un cliente existente en la base de datos. Valida el objeto JSON de la
  solicitud como un objeto Client, y si la validación tiene éxito, actualiza el cliente en la base de datos y responde
  con un mensaje indicando que el cliente se ha actualizado con éxito. Si la validación falla, responde con un mensaje
  de error indicando que hay un problema en el formato JSON. Si no se encuentra un cliente con el código proporcionado,
  responde con un mensaje indicando que no se encontró un cliente con ese código.*/


  def updateClient(code: Int): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val clientResult = request.body.validate[Client]

    clientResult.fold(
      errors => {
        println(s"Error de validación JSON: $errors")
        Future.successful(BadRequest(Json.obj("message" -> "Error en el formato JSON")))
      },
      updatedClient => {
        val updateQuery = Clients.table.filter(_.codigo === code).update(updatedClient)
        val result: Future[Int] = db.run(updateQuery)

        result.map {
          case 0 =>
            NotFound(Json.obj("message" -> s"No se encontró un cliente con código $code"))
          case _ =>
            Ok(Json.obj("codigo" -> code, "message" -> "Cliente actualizado con éxito"))
        }
      }
    )
  }

/*este método maneja la eliminación de un cliente de la base de datos. Construye y ejecuta una consulta
Slick para eliminar el cliente según su código, y responde con un mensaje indicando que el cliente se ha
eliminado con éxito. La respuesta será Ok siempre y cuando la eliminación sea exitosa, independientemente
de si el cliente con el código proporcionado realmente existía en la base de datos o no.*/

  def deleteClient(code: Int): Action[AnyContent] = Action.async { implicit request =>
    val deleteQuery = Clients.table.filter(_.codigo === code).delete
    val result: Future[Int] = db.run(deleteQuery)

    result.map { _ =>
      Ok(Json.obj("message" -> "Cliente eliminado con éxito"))
    }
  }
}
