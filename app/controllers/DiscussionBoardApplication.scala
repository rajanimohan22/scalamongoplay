package controllers

/**
 * Created by Mohan Rao on 25/01/2015.
 */
import models.{MongoDBConnection, Comment, Discussion, DiscussionForm}
import play.api.mvc.Controller
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.data.validation.Constraints._
import play.api.mvc._
import collection.mutable.HashMap
import play.api.db._

import play.core.Router.JavascriptReverseRoute
import play.core.Router._

import play.api.libs.functional.syntax._
import play.api.libs.json.Format
import play.api.libs.json._
import Json._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.Imports.DBObject
import org.bson.types.ObjectId
import scala.collection.mutable.ListBuffer

object DiscussionBoardApplication extends Controller {
  /**
   * This action is used to serve Home page of the application
   *
   * @return
   */
  val discussions: scala.collection.mutable.Map[String, Discussion] = new HashMap
  val discussionForm = Form(
    mapping(
      "postedBy" -> text(minLength = 5, maxLength = 5), //can use customized error reporting by adding -- .verifying(error = "User Name must be exactly 5 chars Length",x => yourCustomValidationFunctionThatReturnsBoolean(x)),
      "title" -> text(minLength = 10, maxLength = 20),
      "commentData" -> text(minLength = 10,  maxLength = 50)
    ) (DiscussionForm.apply _)(DiscussionForm.unapply _) )

  def newDiscussion = Action { implicit request =>
    Ok(views.html.createDiscussionForm(discussionForm))
  }

  def createDiscussion = Action{ implicit request =>
    discussionForm.bindFromRequest.fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:s
        // in your form, test .hasErrors
        println("Form contains errors......most probably Validation errors.")
        BadRequest(views.html.createDiscussionForm(formWithErrors))
      },
      userData => {
        /* binding success, you get the value. */
        createDiscussionInMongoDB(userData.postedBy, userData.title, userData.commentData)
        Redirect(routes.DiscussionBoardApplication.discussionsList)
      }
    )
  }

  def discussionsList = Action { implicit request =>
    val allDiscussionsList = getAllDiscussions()
    Ok(views.html.discussions(allDiscussionsList))
  }

  def showDiscussion(idAndTitle : String) = Action {
    implicit request =>
      Ok(views.html.index(idAndTitle))
  }

  /**
   * This action is used to handle Ajax request
   *
   * @return
   */
  def ajaxCall(discussionThreadId : String) = Action { implicit request =>
    import play.api.libs.json.Json
    import play.api.libs.json.{JsNull,Json,JsString,JsValue}

    val collection = MongoDBConnection.getConnection()
    val commentsTree = getAllCommentsForDiscussion(discussionThreadId);
    val retVal = Json.toJson(commentsTree)
    Ok(retVal)

  }

  def addReplyCommentAjaxCall(replyAjaxData : String) = Action { implicit request =>
    var dataArray = replyAjaxData.split("::::")
    var parentId = dataArray(0)
    var postedBy = dataArray(1)
    var commentData = dataArray(2)
    var dateAndTimePosted = new java.util.Date()
    var newComment = new Comment("NODATA",
      commentData,
      parentId + "-" + postedBy,
      postedBy,
      dateAndTimePosted,
      Nil
    )
    addReplyComment(parentId, postedBy, commentData, dateAndTimePosted)
    var repliesScalaList : ListBuffer[Comment] = new ListBuffer[Comment]();
    repliesScalaList.append(newComment)
    val retVal = Json.toJson(repliesScalaList)
    Ok(retVal)
  }

  def getAllCommentsForDiscussion(parentId : String) : Comment = {
    val rootQuery =  MongoDBObject("commentId" -> parentId)
    val collectionRef = MongoDBConnection.getConnection()
    val allComments = collectionRef.findOne(rootQuery);
    var replies: List[DBObject] = null
    var newComment : Comment = null
    var repliesScalaList : ListBuffer[Comment] = new ListBuffer[Comment]();
    allComments match {
      case Some(x) =>
        newComment = new Comment(
          x.getAs[ObjectId]("_id").toString ,
          x.getAs[String]("commentData").get,
          x.getAs[String]("commentId").get,
          x.getAs[String]("postedBy").get,
          x.getAs[java.util.Date]("createdTime").get,
          convertToCommentsList(x.getAs[List[DBObject]]("replies").getOrElse(Nil))
        )
      case None => Nil
    }
    newComment
  }

  def convertToCommentsList(replies: List[DBObject]) : List[Comment] = {
    var repliesScalaList : ListBuffer[Comment] = new ListBuffer[Comment]();
    replies match {
      case x: List[DBObject] =>
        for(yy <- x) {
          repliesScalaList.append(
            new Comment((yy.getAs[ObjectId]("_id").getOrElse("NODATA")).toString,
              yy.getAs[String]("commentData").get,
              yy.getAs[String]("commentId").get,
              yy.getAs[String]("postedBy").get,
              yy.getAs[java.util.Date]("createdTime").get,
              Nil
            )
          )
        }
    }
    repliesScalaList.toList
  }

  def getAllDiscussions() : List[Discussion] = {
    val collectionRef = MongoDBConnection.getConnection()
    val rootQuery = MongoDBObject("category" -> "discussionTopic")
    val allDiscussions  = collectionRef.find(rootQuery);
    var discussionsList : ListBuffer[Discussion] = new ListBuffer[Discussion]()
    for(yy <- allDiscussions) {
      discussionsList.append(
        new Discussion((yy.getAs[ObjectId]("_id").getOrElse("NODATA")).toString,
          yy.getAs[String]("category").get,
          yy.getAs[String]("postedBy").get,
          yy.getAs[java.util.Date]("createdTime").get,
          yy.getAs[Integer]("lastReplyCount").get,
          yy.getAs[String]("title").get,
          yy.getAs[String]("commentData").getOrElse("NODATA")
        )
      )
    }

    discussionsList.toList
  }

  def createDiscussionInMongoDB(postedBy : String, title: String, commentData : String) = {
    val discussionObject : MongoDBObject = MongoDBObject(
      "category" -> "discussionTopic",
      "postedBy" -> postedBy,
      "createdTime" ->  new java.util.Date,
      "lastReplyCount" -> 0,
      "title" -> title);
    val collectionRef = MongoDBConnection.getConnection()
    collectionRef.save(discussionObject)
    val idInserted  =  discussionObject.getAs[ObjectId]( "_id" ).get

    //Now create a discussion thread that contains all the comments including the parent comment
    createComment( idInserted, 0, postedBy, commentData)
  }

  def createComment( parentId : ObjectId, replyIndex : Integer,
                     postedBy : String, commentData : String) {
    val collectionRef = MongoDBConnection.getConnection()
    val commentId : String = parentId.toString
    val commentObject : MongoDBObject = MongoDBObject(   "commentId" -> commentId,
      "postedBy" -> postedBy,
      "createdTime" ->  new java.util.Date,
      "commentData" -> commentData
    );
    collectionRef.save(commentObject)
  }

  def addReplyComment(parentId: String, postedBy : String, commentData : String, dateAndTimeAdded : java.util.Date) {
    val collectionRef = MongoDBConnection.getConnection()
    val commentId : String = parentId + "-" + postedBy
    val commentObject : MongoDBObject = MongoDBObject(
      "commentId" -> commentId,
      "postedBy" -> postedBy,
      "createdTime" ->  dateAndTimeAdded,
      "commentData" -> commentData
    );
    val replyIndex = getTotalRepliesCount(parentId) + 1;
    val query = MongoDBObject("commentId" -> getRootParentId(parentId))
    val pushingNewInnerComment = $addToSet( "replies"  ->  commentObject )
    collectionRef.update(query, pushingNewInnerComment, true)

    val rootQuery =  MongoDBObject("_id" -> new ObjectId(getRootParentId(parentId)))
    val rootUpdateQuery = $set("lastReplyCount" -> replyIndex.toInt)
    collectionRef.update(rootQuery, rootUpdateQuery, true)

  }

  def getTotalRepliesCount(parentId : String) : Integer = {
    var rootParentId = getRootParentId(parentId);
    val collectionRef = MongoDBConnection.getConnection()
    val query = MongoDBObject("_id" -> new ObjectId(rootParentId))
    val rootParentObject  = collectionRef.findOne(query)
    var replyIndex = 0;
    rootParentObject match {
      case Some(x) => replyIndex = x.getAs[Integer]("lastReplyCount").get
      case None => Nil
    }
    replyIndex
  }

  def getRootParentId(commentId : String) : String = {
    var rootParentId = "";
    if(commentId.contains("-"))
      rootParentId = commentId.split("-").toList.head
    else
      rootParentId = commentId
    rootParentId
  }
}
