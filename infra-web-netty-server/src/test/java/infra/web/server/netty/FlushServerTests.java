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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.server.HttpRequestExecutor;
import infra.web.server.SimpleHttpRequestExecutor;
import infra.web.server.WebServer;
import infra.web.server.error.SendErrorHandler;
import io.netty.util.ResourceLeakDetector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * End-to-end HTTP/1.1 tests for response flushing and framing, driven over a
 * real socket so the head and body actually pass through the server codec.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class FlushServerTests {

  private WebServer server;

  private ExecutorService executor;

  private Http1TestClient client;

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
  }

  @Test
  void emptyFlushDeliversHeadImmediatelyAndKeepsConnectionOpen() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/empty-flush");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEmpty();
    assertThat(response.headers()).containsEntry("content-length", "0");
    assertThat(response.headers()).doesNotContainKey("transfer-encoding");
    assertThat(client.isActive()).isTrue();

    Http1TestClient.Response second = client.get("/plain");
    assertThat(second.bodyAsString()).isEqualTo("plain");
  }

  @Test
  void emptyWriterFlushDeliversHeadWithoutBody() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/empty-writer-flush");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEmpty();
    assertThat(response.headers()).containsEntry("content-length", "0");
    assertThat(client.isActive()).isTrue();
  }

  @Test
  void chunkedFlushWritesEverySegment() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/chunked");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("first-|second");
    assertThat(response.headers()).containsEntry("transfer-encoding", "chunked");
    assertThat(response.headers()).doesNotContainKey("content-length");
    assertThat(client.isActive()).isTrue();
  }

  @Test
  void firstFlushWithoutChunkedFixesTheResponseLength() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/fixed");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("first");
    assertThat(response.headers()).containsEntry("content-length", "5");
    assertThat(response.headers()).doesNotContainKey("transfer-encoding");
    assertThat(client.isActive()).isTrue();
  }

  @Test
  void writerFlushSendsBufferedCharactersWithAccurateLength() throws Exception {
    start();

    Http1TestClient.Response response = client.get("/writer");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("hello-writer");
    assertThat(response.headers()).containsEntry("content-length", "12");
    assertThat(client.isActive()).isTrue();
  }

  private void start() throws Exception {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

    executor = Executors.newFixedThreadPool(4);
    HttpRequestExecutor requestExecutor = new SimpleHttpRequestExecutor(executor);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    TestDispatcherHandler dispatcher = new TestDispatcherHandler(applicationContext);

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

    private TestDispatcherHandler(ApplicationContext context) {
      super(context);
    }

    @Override
    public void handleRequest(HttpContext context) throws Exception {
      switch (context.getRequestURI()) {
        case "/empty-flush" -> {
          // no body at all: the head must reach the client on this flush
          context.flush();
        }
        case "/empty-writer-flush" -> {
          // nothing buffered, but flushing the writer must still push the head
          context.getWriter().flush();
        }
        case "/chunked" -> {
          context.setContentType(MediaType.TEXT_PLAIN);
          context.setHeader("Transfer-Encoding", "chunked");
          var out = context.getOutputStream();
          out.write("first-|".getBytes(StandardCharsets.UTF_8));
          context.flush();
          out.write("second".getBytes(StandardCharsets.UTF_8));
          context.flush();
        }
        case "/fixed" -> {
          context.setContentType(MediaType.TEXT_PLAIN);
          context.getOutputStream().write("first".getBytes(StandardCharsets.UTF_8));
          context.flush();
        }
        case "/writer" -> {
          context.setContentType(MediaType.TEXT_PLAIN);
          context.getWriter().write("hello-writer");
          context.flush();
        }
        default -> {
          context.setContentType(MediaType.TEXT_PLAIN);
          context.getOutputStream().write("plain".getBytes(StandardCharsets.UTF_8));
        }
      }
      completeRequest(context, null);
    }
  }

}
