package benchmarks

import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit

@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@OperationsPerInvocation(value = 20)
@State(Scope.Benchmark)
class Benchmarks {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def flatMapBench(): Unit = {
    val map = (1 to 10000).map(_ -> Set(1, 4, 2, 5)).toMap
    val _   = (1 to 2000).flatMap(map)
  }
}
