package zio.aws.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.github.matsluni.akkahttpspi.{AkkaHttpClient => SPI}
import zio.aws.core.BuilderHelper
import zio.aws.core.httpclient.{HttpClient, ServiceHttpCapabilities}
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import zio.{Task, ZIO, ZLayer, ZManaged}

import scala.concurrent.ExecutionContext

object AkkaHttpClient {
  val builderHelper: BuilderHelper[SdkAsyncHttpClient] = BuilderHelper.apply

  import builderHelper._

  def client(
      connectionPoolSettings: Option[ConnectionPoolSettings] = None,
      executionContext: Option[ExecutionContext] = None
  ): ZLayer[ActorSystem, Throwable, HttpClient] =
    (for {
      actorSystem <- ZManaged.service[ActorSystem]
      akkaClient <- ZManaged
        .fromAutoCloseable(
          ZIO(
            SPI
              .builder()
              .withActorSystem(actorSystem)
              .optionallyWith(connectionPoolSettings)(
                _.withConnectionPoolSettings
              )
              .optionallyWith(executionContext)(_.withExecutionContext)
              .build()
          )
        )
    } yield new HttpClient {
      override def clientFor(
          serviceCaps: ServiceHttpCapabilities
      ): Task[SdkAsyncHttpClient] = Task.succeed(akkaClient)
    }).toLayer
}
