package service

import javax.inject.Inject

import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by brunow on 13/05/2017.
  */
trait TwitterService {

  def fetchRelationshipCounts(userName: String)(implicit ec: ExecutionContext): Future[TwitterCounts]

  def postTweet(message: String)(implicit ec: ExecutionContext): Future[Unit]

}

case class TwitterCounts(followersCount: Long, friendsCount: Long)

class WSTwitterService @Inject()(conf: play.api.Configuration, ws: WSClient) extends TwitterService {
  override def fetchRelationshipCounts(userName: String)(implicit ec: ExecutionContext): Future[TwitterCounts] = {
    credentials.map {
      case (consumerKey, requestToken) => ws.url("https://api.twitter.com/1.1/users/show.json").
        sign(OAuthCalculator(consumerKey, requestToken)).withQueryString("screen_name" -> userName).get().map {
        response =>
          if (response.status == 200) {
            TwitterCounts(
              (response.json \ "followers_count").as[Long],
              (response.json \ "friends_count").as[Long]
            )
          } else {
            throw new TwitterServiceException(s"Could not retrieve counts for Twitter user $userName")
          }
      }
    }.getOrElse {
      Future.failed(new TwitterServiceException("You did not correctly configure the Twitter credentials"))
    }
  }

  override def postTweet(message: String)(implicit ec: ExecutionContext): Future[Unit] = Future.successful {
    println("TWITTER: " + message)
  }

  private def credentials = for {
    apiKey <- conf.getString("twitter.apiKey")

    apiSecret <- conf.getString("twitter.apiSecret")

    token <- conf.getString("twitter.accessToken")

    tokenSecret <- conf.getString("twitter.accessTokenSecret")

  } yield (ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))
}

case class TwitterServiceException(message: String) extends RuntimeException(message)