package models

import play.api.libs.functional.syntax._
import play.api.libs.json.Format
import play.api.libs.json._
import Json._


/**
 * Created by Mohan Rao on 08/02/2015.
 */

case class Comment (
     _id : String,
     commentData : String,
     commentId : String,
     postedBy : String,
     createdTime : java.util.Date,
     replies : List[Comment]
  )
object Comment {
  import play.api.data.validation.ValidationError
  import play.api.libs.json.Reads._
  def notEqualReads[T](v: T)(implicit r: Reads[T]): Reads[T] = Reads.filterNot(ValidationError("validate.error.unexpected.value", v))( _ == v )
  def skipReads(implicit r: Reads[String]): Reads[String] = r.map( _.substring(2) )

  val commentReads: Reads[Comment] = (
    (__ \ "_id").read[String] and
      (__ \ "commentData").read[String] and
      (__ \ "commentId").read[String] and
      (__ \ "postedBy").read[String] and
      (__ \ "createdTime").read[java.util.Date] and
      (__ \ "replies").lazyRead( list[Comment](commentReads) )
    )(Comment.apply _)

  import play.api.libs.json.Writes._
  val commentWrites: Writes[Comment] = (
    (__ \ "_id").write[String] and
      (__ \ "commentData").write[String] and
      (__ \ "commentId").write[String] and
      (__ \ "postedBy").write[String] and
      (__ \ "createdTime").write[java.util.Date] and
      (__ \ "replies").lazyWrite(Writes.traversableWrites[Comment](commentWrites))
    )(unlift(Comment.unapply))

  implicit val commentFormat: Format[Comment] = Format(commentReads, commentWrites)
}
