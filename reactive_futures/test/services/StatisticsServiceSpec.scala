package services



import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.mutable.ExecutionEnvironment
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.WithApplication
import play.modules.reactivemongo._
import service.{DefaultStatisticsService, MongoStatisticsRepository, WSTwitterService}

import scala.concurrent.duration._

/**
  * Created by brunow on 13/05/2017.
  */
class StatisticsServiceSpec() extends Specification with ExecutionEnvironment {

  def is(implicit ee: ExecutionEnv) = {
    "The statistics service" should {

      "Compute and publish statistics" in new WithApplication() {
        val repository = new MongoStatisticsRepository(configuredAppBuilder.injector.instanceOf[ReactiveMongoApi])
        val wSTwitterService = new WSTwitterService(configuredAppBuilder.injector.instanceOf[Configuration],
            configuredAppBuilder.injector.instanceOf[WSClient])
        val service = new DefaultStatisticsService(repository, wSTwitterService)

        val f = service.createUserStatistics("bweninger")

        f must beEqualTo(()).await(0, 5.seconds)
      }
    }

    def configuredAppBuilder = {

      import scala.collection.JavaConversions.iterableAsScalaIterable


      val env = play.api.Environment.simple(mode = play.api.Mode.Test)

      val config = play.api.Configuration.load(env)

      val modules = config.getStringList("play.modules.enabled").fold(
        List.empty[String])(l => iterableAsScalaIterable(l).toList)

      new GuiceApplicationBuilder().configure("play.modules.enabled" -> (modules :+
        "play.modules.reactivemongo.ReactiveMongoModule")).build
    }
  }
}
