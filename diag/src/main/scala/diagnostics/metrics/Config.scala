package diagnostics.metrics

import sdk.reflect.Description
import scala.concurrent.duration.{Duration, DurationInt}

@Description("influxdb")
final case class Config(
  @Description("Interval to accumulate measurements before sending in a batch.")
  batchInterval: Duration = 1.second,
  @Description("Throttle measurements of the value to specified rate.")
  meter: Duration = 1.second,
  @Description("InfluxDb credential: connection string.")
  url: String = "https://us-east-1-1.aws.cloud2.influxdata.com",
  @Description("InfluxDb credential: organization.")
  org: String = "defaultOrg",
  @Description("InfluxDb credential: access token.")
  token: String = "defaultToken",
  @Description("InfluxDb credential: bucket.")
  bucket: String = "defaultBucket",
  @Description("InfluxDb http client: read timeout.")
  readTimeout: Duration = 10.seconds,
  @Description("InfluxDb http client: write timeout.")
  writeTimeout: Duration = 10.seconds,
  @Description("InfluxDb http client: connect timeout.")
  connectTimeout: Duration = 10.seconds,
)

object Config {
  val Default: Config = Config()
}
