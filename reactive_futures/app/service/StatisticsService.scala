package service

import org.joda.time.{DateTime, Period}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * Created by brunow on 13/05/2017.
  */
trait StatisticsService {
  def createUserStatistics(userName: String)(implicit ec: ExecutionContext): Future[Unit]
}


class DefaultStatisticsService(statisticsRepository: StatisticsRepository, twitterService: TwitterService) extends StatisticsService {
  override def createUserStatistics(userName: String)(implicit ec: ExecutionContext): Future[Unit] = {

    def storeCounts(counts: (StoredCounts, TwitterCounts)): Future[Unit] = {
      counts match {
        case (previous, current) => statisticsRepository.storeCounts(
          StoredCounts(DateTime.now(), userName, current.followersCount, current.friendsCount))
      }
    }

    def publishMessage(counts: (StoredCounts, TwitterCounts)): Future[Unit] = counts match {
      case (prev, curr) => {
        val followersDiff = curr.followersCount - prev.followersCount
        val friendsDiff = curr.friendsCount - prev.friendsCount

        def phrasing(difference: Long) = if (difference > 0) "gained" else "lost"

        val durationInDays = new Period(prev.when, DateTime.now()).getDays

        twitterService.postTweet(s"@$userName in the past $durationInDays you have " +
          s"${phrasing(followersDiff)} $followersDiff " +
          s"followers and ${phrasing(friendsDiff)} " +
          s"$friendsDiff friends")
      }
    }

    val previousCounts = statisticsRepository.retrieveLatestCounts(userName)

    val currentCounts = twitterService.fetchRelationshipCounts(userName)

    val counts = for {
      previous <- previousCounts
      current <- currentCounts
    } yield (previous, current)

    val storedCounts = counts.flatMap(storeCounts)
    val publishedMessage = counts.flatMap(publishMessage)

    var result = for {
      _ <- storedCounts
      _ <- publishedMessage
    } yield {}

    result recoverWith {
      case CountStorageException(countsToStore) => retryStoring(countsToStore, attemptNumber = 0)
    } recover {
      case CountStorageException(countsToStore) => throw StatisticsServiceFailed("We couldn't save the statistics to our database." +
        "Next time it will work!")
      case CountRetrievalException(user, cause) => throw StatisticsServiceFailed("We have a problem with our database. Sorry", cause)
      case TwitterServiceException(message) => throw StatisticsServiceFailed(s"We have a problem contacting Twitter: $message")
      case NonFatal(t) => throw StatisticsServiceFailed("We have an unknown problem. Sorry!")
    }
  }

  private def retryStoring(counts: StoredCounts, attemptNumber: Int)
                          (implicit ec: ExecutionContext) : Future[Unit] = {
    if (attemptNumber < 3) {
      statisticsRepository.storeCounts(counts).recoverWith{
        case NonFatal(t) => retryStoring(counts, attemptNumber + 1)
      }
    } else {
      Future.failed(CountStorageException(counts))
    }
  }
}

class StatisticsServiceFailed(cause: Throwable) extends RuntimeException(cause) {
  def this(message: String) = this(new RuntimeException(message))

  def this (message: String, cause: Throwable) = this (new RuntimeException(message, cause))
}

object StatisticsServiceFailed {
  def apply(message: String): StatisticsServiceFailed =
    new StatisticsServiceFailed(message)

  def apply(message: String, cause: Throwable): StatisticsServiceFailed
  = new StatisticsServiceFailed(message, cause)
}
