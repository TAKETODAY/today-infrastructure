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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.server.HttpRequestExecutor;
import infra.web.server.SimpleHttpRequestExecutor;
import infra.web.server.WebServer;
import infra.web.server.error.SendErrorHandler;
import io.netty.util.ResourceLeakDetector;

import static infra.web.Trailer.field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * End-to-end HTTP/1.1 tests for response trailers, driven over a real socket so
 * the chunked framing and trailing fields pass through the server codec.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class TrailerServerTests {

  private WebServer server;

  private ExecutorService executor;

  private Http1TestClient client;

  private TestDispatcherHandler dispatcher;

  private Path file;

  @AfterEach
  void tearDown() throws Exception {
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
    if (file != null) {
      Files.deleteIfExists(file);
      file = null;
    }
  }

  @Test
  void trailersAreSentAfterBodyAndConnectionIsReused() throws Exception {
    start();

    Http1TestClient.Response first = client.get("/trailers");

    assertThat(first.status()).isEqualTo(200);
    assertThat(first.bodyAsString()).isEqualTo("hello");
    assertThat(first.headers()).containsEntry("transfer-encoding", "chunked");
    assertThat(first.headers()).doesNotContainKey("content-length");
    assertThat(first.headers()).containsEntry("trailer", "Content-Digest");
    assertThat(first.trailers()).containsEntry("content-digest", "sha-256=:abc:");

    Http1TestClient.Response second = client.get("/plain");

    assertThat(second.status()).isEqualTo(200);
    assertThat(second.bodyAsString()).isEqualTo("plain");
    assertThat(second.headers()).containsEntry("content-length", "5");
    assertThat(second.trailers()).isEmpty();
    assertThat(client.isActive()).isTrue();
  }

  @Test
  void multipleFlushesKeepASingleTerminatorWithTrailers() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/trailers/multiple-flushes");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("first-second");
    assertThat(response.trailers()).containsEntry("content-digest", "sha-256=:abc:");
  }

  @Test
  void emptyTrailerResultSendsDeclarationButNoFields() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/trailers/empty");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("empty-body");
    assertThat(response.headers()).containsEntry("transfer-encoding", "chunked");
    assertThat(response.headers()).containsEntry("trailer", "Content-Digest");
    assertThat(response.trailers()).isEmpty();
  }

  @Test
  void fileResponseUsesChunkedFramingAndTrailers() throws Exception {
    file = Files.createTempFile("http1-trailer", ".txt");
    Files.writeString(file, "file-content-".repeat(1024));
    start();

    Http1TestClient.Response response = client.get("/trailers/file");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo(Files.readString(file));
    assertThat(response.headers()).containsEntry("transfer-encoding", "chunked");
    assertThat(response.headers()).doesNotContainKey("content-length");
    assertThat(response.headers()).containsEntry("trailer", "Content-Digest");
    assertThat(response.trailers()).containsEntry("content-digest", "sha-256=:file:");
  }

  @Test
  void lateTrailerRegistrationKeepsFileBodyAndTrailingFields() throws Exception {
    file = Files.createTempFile("http1-trailer-late", ".txt");
    String content = "late-file-content-".repeat(1024);
    Files.writeString(file, content);
    start();

    Http1TestClient.Response response = client.get("/trailers/file/late");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo(content);
    assertThat(response.headers()).containsEntry("transfer-encoding", "chunked");
    assertThat(response.headers()).doesNotContainKey("content-length");
    assertThat(response.headers()).containsEntry("trailer", "Content-Digest");
    assertThat(response.trailers()).containsEntry("content-digest", "sha-256=:late:");
  }

  @Test
  void rangeFileWithTrailerKeepsRangeBodyAndTrailingFields() throws Exception {
    file = Files.createTempFile("http1-trailer-range", ".txt");
    String content = "0123456789".repeat(64);
    Files.writeString(file, content);
    start();

    Http1TestClient.Response response = client.get("/trailers/file/range");

    assertThat(response.status()).isEqualTo(206);
    assertThat(response.bodyAsString()).isEqualTo(content.substring(10, 110));
    assertThat(response.headers()).containsEntry("content-range", "bytes 10-109/640");
    assertThat(response.headers()).containsEntry("transfer-encoding", "chunked");
    assertThat(response.headers()).doesNotContainKey("content-length");
    assertThat(response.headers()).containsEntry("trailer", "Content-Digest");
    assertThat(response.trailers()).containsEntry("content-digest", "sha-256=:range:");
  }

  @Test
  void bodyFailureClearsRegistrationAndSkipsSupplier() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/trailers/error");

    assertThat(response.status()).isEqualTo(500);
    assertThat(response.trailers()).isEmpty();
    assertThat(dispatcher.trailerEvaluations).hasValue(0);
  }

  private void start() throws Exception {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

    executor = Executors.newFixedThreadPool(4);
    HttpRequestExecutor requestExecutor = new SimpleHttpRequestExecutor(executor);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    dispatcher = new TestDispatcherHandler(applicationContext);

    NettyRequestConfig requestConfig = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler(noopSendErrorHandler())
            .build();

    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(requestConfig, applicationContext,
            dispatcher, requestExecutor);

    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setPort(0);
    factory.setHttpTrafficHandler(trafficHandler);

    server = factory.createWebServer();
    server.start();
    client = Http1TestClient.connect(server.getPort());
  }

  private static SendErrorHandler noopSendErrorHandler() {
    return (request, message) -> {
    };
  }

  private final class TestDispatcherHandler extends DispatcherHandler {

    private final AtomicInteger trailerEvaluations = new AtomicInteger();

    private TestDispatcherHandler(ApplicationContext context) {
      super(context);
    }

    @Override
    public void handleRequest(HttpContext context) throws Exception {
      String path = context.getRequestURI();
      switch (path) {
        case "/trailers" -> {
          registerTrailer(context, "sha-256=:abc:");
          writeResponse(context, "hello");
        }
        case "/trailers/multiple-flushes" -> {
          registerTrailer(context, "sha-256=:abc:");
          context.setContentType(MediaType.TEXT_PLAIN);
          context.getOutputStream().write("first-".getBytes(StandardCharsets.UTF_8));
          context.flush();
          context.getOutputStream().write("second".getBytes(StandardCharsets.UTF_8));
          context.flush();
        }
        case "/trailers/empty" -> {
          context.setTrailerFields(
                  field("Content-Digest", () -> null));
          writeResponse(context, "empty-body");
        }
        case "/trailers/file" -> {
          long length = Files.size(file);
          context.setContentType(MediaType.TEXT_PLAIN);
          context.setContentLength(length);
          registerTrailer(context, "sha-256=:file:");
          context.asHttpOutputMessage().sendFile(file, 0, length);
        }
        case "/trailers/file/late" -> {
          long length = Files.size(file);
          context.setContentType(MediaType.TEXT_PLAIN);
          context.asHttpOutputMessage().sendFile(file, 0, length);
          registerTrailer(context, "sha-256=:late:");
        }
        case "/trailers/file/range" -> {
          long length = Files.size(file);
          context.setStatus(206);
          context.setHeader("Content-Range", "bytes 10-109/" + length);
          registerTrailer(context, "sha-256=:range:");
          context.asHttpOutputMessage().sendFile(file, 10, 100);
        }
        case "/trailers/error" -> {
          registerTrailer(context, "sha-256=:never:");
          context.getOutputStream().write("partial".getBytes(StandardCharsets.UTF_8));
          throw new IllegalStateException("body write failed");
        }
        default -> writeResponse(context, "plain");
      }
      requestCompleted(context, null);
    }

    private void registerTrailer(HttpContext context, String digest) {
      context.setTrailerFields(field("Content-Digest", () -> {
        trailerEvaluations.incrementAndGet();
        return digest;
      }));
    }

    private static void writeResponse(HttpContext context, String body) throws Exception {
      context.setContentType(MediaType.TEXT_PLAIN);
      context.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

  }

}
