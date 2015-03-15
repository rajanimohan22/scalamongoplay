package models

/**
 * Created by Mohan Rao on 08/02/2015.
 */
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.MongoCollection
object MongoDBConnection {
  def getConnection() : MongoCollection = {
    val mongoClient =  MongoClient("192.168.1.5", 27017)
    val db = mongoClient("discussionBoardDB")
    val collection = db("commentsData")
    collection
  }

}
