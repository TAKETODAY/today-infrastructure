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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
import infra.http.MediaType;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.multipart.parsing.DefaultMultipartParser;
import infra.web.server.Http2;
import infra.web.server.HttpRequestExecutor;
import infra.web.server.SimpleHttpRequestExecutor;
import infra.web.server.WebServer;

/**
 * HTTP/2 request/response benchmark for the Netty server.
 * <p>
 * The JMH entry point measures single-request throughput and latency. The
 * {@link #main(String[])} entry point runs a small closed-loop benchmark with a
 * fixed concurrency and prints throughput plus p50/p99 latency, which is handy
 * for comparing two branches without running a full JMH session.
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
public class Http2ServerBenchmark {

  private static final int WARMUP_REQUESTS = 2000;

  private static final int MEASURE_REQUESTS = 20000;

  private static final int CONCURRENCY = 8;

  @Param({ "platform", "virtual" })
  public String executorType;

  private WebServer server;

  private ExecutorService executor;

  private Http2BenchmarkClient client;

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
    DispatcherHandler dispatcher = new BenchmarkDispatcher(applicationContext);

    NettyRequestConfig requestConfig = NettyRequestConfig.forBuilder(false)
            .multipartParser(new DefaultMultipartParser())
            .sendErrorHandler((request, message) -> {
            })
            .build();

    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(requestConfig, applicationContext,
            dispatcher, requestExecutor);

    Http2 http2 = new Http2();
    http2.setEnabled(true);

    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setPort(0);
    factory.setHttp2(http2);
    factory.setHttpTrafficHandler(trafficHandler);

    server = factory.createWebServer();
    server.start();
    client = new Http2BenchmarkClient(server.getPort());
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    if (client != null) {
      client.close();
      client = null;
    }
    if (server != null) {
      server.stop();
      server = null;
    }
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
  }

  @Benchmark
  public int get() throws Exception {
    return client.get("/bench");
  }

  public static void main(String[] args) throws Exception {
    Http2ServerBenchmark benchmark = new Http2ServerBenchmark();
    benchmark.executorType = args.length > 0 ? args[0] : "platform";
    benchmark.setup();
    try {
      for (int i = 0; i < WARMUP_REQUESTS; i++) {
        benchmark.client.get("/bench");
      }

      ExecutorService requestExecutor = Executors.newFixedThreadPool(CONCURRENCY);
      CountDownLatch start = new CountDownLatch(1);
      List<Long> latencies = Collections.synchronizedList(new ArrayList<>(MEASURE_REQUESTS));
      List<Future<?>> futures = new ArrayList<>(CONCURRENCY);
      int perThread = MEASURE_REQUESTS / CONCURRENCY;
      try {
        for (int t = 0; t < CONCURRENCY; t++) {
          futures.add(requestExecutor.submit(() -> {
            start.await();
            for (int i = 0; i < perThread; i++) {
              long begin = System.nanoTime();
              benchmark.client.get("/bench");
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
    try {
      Method startVirtualThread = Thread.class.getMethod("startVirtualThread", Runnable.class);
      return (context, command) -> {
        try {
          startVirtualThread.invoke(null, command);
        }
        catch (ReflectiveOperationException ex) {
          throw new IllegalStateException("Failed to start a virtual thread", ex);
        }
      };
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalStateException("Virtual threads are unavailable", ex);
    }
  }

  private static double percentileMillis(List<Long> sortedNanos, int percentile) {
    int index = (int) Math.ceil(sortedNanos.size() * percentile / 100.0) - 1;
    return sortedNanos.get(Math.max(0, Math.min(index, sortedNanos.size() - 1))) / 1_000_000.0;
  }

  private static final class BenchmarkDispatcher extends DispatcherHandler {

    private static final byte[] BODY = "hello".getBytes(StandardCharsets.UTF_8);

    private BenchmarkDispatcher(ApplicationContext context) {
      super(context);
    }

    @Override
    public void handleRequest(HttpContext context) throws Exception {
      context.setContentType(MediaType.TEXT_PLAIN);
      context.setContentLength(BODY.length);
      context.getOutputStream().write(BODY);
      requestCompleted(context, null);
    }

  }

}
