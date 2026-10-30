/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.server.netty;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import infra.context.ApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.web.DispatcherHandler;
import infra.web.multipart.parsing.DefaultMultipartParser;
import infra.web.server.HttpRequestExecutor;
import infra.web.server.SimpleHttpRequestExecutor;
import infra.web.server.WebServer;

/**
 * HTTP/1.1 request/response benchmark for the Netty server, kept structurally
 * parallel to {@link Http2ServerBenchmark} so the two protocols can be compared.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@State(Scope.Benchmark)
@BenchmarkMode({ Mode.Throughput, Mode.SampleTime })
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class Http1ServerBenchmark {

  private static final int WARMUP_REQUESTS = 2000;

  private static final int MEASURE_REQUESTS = Integer.getInteger("bench.requests", 20000);

  private static final int CONCURRENCY = Integer.getInteger("bench.concurrency", 8);

  @Param({ "platform", "virtual" })
  public String executorType;

  private WebServer server;

  private ExecutorService executor;

  private HttpClient httpClient;

  private Http1BenchmarkClient client;

  @Setup(Level.Trial)
  public void setup() throws Exception {
    HttpRequestExecutor requestExecutor;
    if ("virtual".equals(executorType)) {
      requestExecutor = newVirtualThreadExecutor();
    }
    else {
      executor = Executors.newFixedThreadPool(4);
      requestExecutor = new SimpleHttpRequestExecutor(executor);
    }

    ApplicationContext applicationContext = new GenericApplicationContext();
    DispatcherHandler dispatcher = new BenchmarkHandler(applicationContext);

    NettyRequestConfig requestConfig = NettyRequestConfig.forBuilder(false)
            .multipartParser(new DefaultMultipartParser())
            .sendErrorHandler((request, message) -> {
            })
            .build();

    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(requestConfig, applicationContext,
            dispatcher, requestExecutor);

    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setPort(0);
    factory.setHttpTrafficHandler(trafficHandler);

    server = factory.createWebServer();
    server.start();
    httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    client = new Http1BenchmarkClient(httpClient, server.getPort());
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    client = null;
    if (server != null) {
      server.stop();
      server = null;
    }
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
    if (httpClient != null) {
      httpClient.close();
      httpClient = null;
    }
  }

  @Benchmark
  public int get() throws Exception {
    return client.get();
  }

  public static void main(String[] args) throws Exception {
    Http1ServerBenchmark benchmark = new Http1ServerBenchmark();
    benchmark.executorType = args.length > 0 ? args[0] : "platform";
    benchmark.setup();
    try {
      for (int i = 0; i < WARMUP_REQUESTS; i++) {
        benchmark.client.get();
      }

      int port = benchmark.server.getPort();
      List<Http1BenchmarkClient> clients = new ArrayList<>(CONCURRENCY);
      for (int i = 0; i < CONCURRENCY; i++) {
        clients.add(new Http1BenchmarkClient(benchmark.httpClient, port));
      }

      ExecutorService requestExecutor = Executors.newFixedThreadPool(CONCURRENCY);
      CountDownLatch start = new CountDownLatch(1);
      List<Long> latencies = Collections.synchronizedList(new ArrayList<>(MEASURE_REQUESTS));
      List<Future<?>> futures = new ArrayList<>(CONCURRENCY);
      int perThread = MEASURE_REQUESTS / CONCURRENCY;
      try {
        for (int t = 0; t < CONCURRENCY; t++) {
          int index = t;
          futures.add(requestExecutor.submit(() -> {
            start.await();
            for (int i = 0; i < perThread; i++) {
              long begin = System.nanoTime();
              clients.get(index).get();
              latencies.add(System.nanoTime() - begin);
            }
            return null;
          }));
        }

        long begin = System.nanoTime();
        start.countDown();
        for (Future<?> future : futures) {
          future.get();
        }
        long elapsedNanos = System.nanoTime() - begin;

        Collections.sort(latencies);
        double seconds = elapsedNanos / 1_000_000_000.0;
        System.out.printf("requests=%d concurrency=%d throughput=%.0f req/s p50=%.3f ms p99=%.3f ms%n",
                latencies.size(), CONCURRENCY, latencies.size() / seconds,
                percentileMillis(latencies, 50), percentileMillis(latencies, 99));
      }
      finally {
        requestExecutor.shutdownNow();
      }
    }
    finally {
      benchmark.tearDown();
    }
  }

  private static HttpRequestExecutor newVirtualThreadExecutor() {
    return (context, command) -> Thread.startVirtualThread(command);
  }

  private static double percentileMillis(List<Long> sortedNanos, int percentile) {
    int index = (int) Math.ceil(sortedNanos.size() * percentile / 100.0) - 1;
    return sortedNanos.get(Math.max(0, Math.min(index, sortedNanos.size() - 1))) / 1_000_000.0;
  }

}
