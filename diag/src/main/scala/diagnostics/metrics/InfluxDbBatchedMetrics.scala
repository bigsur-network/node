package diagnostics.metrics

import cats.effect.kernel.{Async, Clock}
import cats.effect.{Ref, Resource, Sync}
import cats.syntax.all.*
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory, InfluxDBClientOptions, WriteApi}
import okhttp3.OkHttpClient
import sdk.diag.Metrics
import sdk.diag.Metrics.{Field, Tag}
import sdk.log.Logger.*

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*

object InfluxDbBatchedMetrics {

  // key for tag identifying the process reporting metrics
  private val SOURCE_TAG_KEY: String = "sourceTag"

  private def clientResource[F[_]: Sync](config: Config): Resource[F, InfluxDBClient] =
    Resource.make(Sync[F].delay {
      import config.*

      // Set http client
      val httpClient: OkHttpClient.Builder = new OkHttpClient.Builder()
        .readTimeout(readTimeout._1, readTimeout._2)
        .writeTimeout(writeTimeout._1, writeTimeout._2)
        .connectTimeout(connectTimeout._1, connectTimeout._2)

      // Set client options
      val options: InfluxDBClientOptions = InfluxDBClientOptions
        .builder()
        .url(url)
        .authenticateToken(token.toCharArray)
        .bucket(config.bucket)
        .org(org)
        .okHttpClient(httpClient)
        .build();

      // Build, enable compression
      val c = InfluxDBClientFactory.create(options).enableGzip()

      logInfo(s"InfluxDB client created.")
      c
    })(c => Sync[F].delay(c.close()))

  private def writerResource[F[_]: Sync](client: InfluxDBClient): Resource[F, WriteApi] =
    Resource.make(Sync[F].delay(client.makeWriteApi()))(w => Sync[F].delay { w.flush(); w.close() })

  private def metricsResource[F[_]: Async](
    writer: WriteApi,
    batchInterval: Duration,
    meter: Duration,
    sourceTag: String,
  ): Resource[F, Metrics[F]] = {
    val initPoints = Ref.of(List.empty[Point])
    val initTimes  = Ref.of(Map.empty[String, Long])

    Resource.eval(initPoints.product(initTimes)).flatMap { case (pointsRef, timesRef) =>
      val report = pointsRef.getAndSet(List()).map { points =>
        writer.writePoints(points.asJava)
        logDebug(s"Sent ${points.size} points.")
      }

      val startFiber = Async[F]
        .background((report *> Async[F].sleep(batchInterval)).foreverM)
        .as(logInfo(s"InfluxDb reporter started."))

      val metrics = new Metrics[F] {
        override def measurement(name: String, fields: List[Metrics.Field], tags: List[Metrics.Tag]): F[Unit] =
          Clock[F].realTimeInstant.flatMap { now =>
            lazy val p = Point
              .measurement(name)
              .addTag(SOURCE_TAG_KEY, sourceTag)
              .time(now, WritePrecision.MS)
              .addTags(tags.map { case Tag(k, v) => k -> v }.toMap.asJava)
              .addFields(fields.map { case Field(k, v) => k -> v }.toMap.asJava)

            timesRef
              .modify { s =>
                val nowMillis    = now.toEpochMilli
                val shouldUpdate = s.get(name).forall(x => (nowMillis - x) >= meter.toMillis)
                val newS         = if (shouldUpdate) s + (name -> nowMillis) else s
                newS -> shouldUpdate
              }
              .flatMap(pointsRef.update(_ :+ p).whenA(_))
          }
      }

      startFiber.as(metrics)
    }
  }

  /**
   * Report metrics to InfluxDB with batching.
   * @param config configuration object
   * @param sourceTag tag of a source. Useful to show many instantiations of the same metric on the same chart
   */
  def apply[F[_]: Async](config: Config, sourceTag: String): Resource[F, Metrics[F]] =
    for {
      client <- clientResource(config)
      writer <- writerResource(client)
      r      <- metricsResource(writer, config.batchInterval, config.meter, sourceTag)
    } yield r
}
