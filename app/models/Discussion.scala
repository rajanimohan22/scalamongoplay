package models

/**
 * Created by Mohan Rao on 08/02/2015.
 */
case class Discussion (
      _id : String,
      category : String,
      postedBy : String,
      createdTime : java.util.Date,
      lastReplyCount : Int,
      title : String,
      commentData : String
)
object Discussion {

}
