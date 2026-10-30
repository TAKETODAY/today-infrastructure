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

import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.server.Http2;
import infra.web.server.HttpRequestExecutor;
import infra.web.server.SimpleHttpRequestExecutor;
import infra.web.server.Ssl;
import infra.web.server.WebServer;
import infra.web.server.error.SendErrorHandler;
import io.netty.util.ResourceLeakDetector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * End-to-end HTTP/2 tests for the Netty server. Requests are driven through the
 * native stream frame API so the tests stay valid while the HTTP/1.x intermediate
 * conversion is removed.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class Http2ServerTests {

  private WebServer server;

  private ExecutorService executor;

  private Http2TestClient client;

  @AfterEach
  void tearDown() {
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
  void cleartextGetWithoutRequestBody() throws Exception {
    start(false);

    Http2TestClient.Response response = client.get("/hello");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("hello");
    assertThat(response.headers()).containsEntry("content-type", "text/plain");
  }

  @Test
  void cleartextPostWithSingleDataFrame() throws Exception {
    start(false);

    Http2TestClient.Response response = client.post("/echo", "request-body".getBytes(StandardCharsets.UTF_8));

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("request-body");
  }

  @Test
  void cleartextPostWithMultipleDataFrames() throws Exception {
    start(false);

    List<byte[]> chunks = new ArrayList<>();
    chunks.add("first-".getBytes(StandardCharsets.UTF_8));
    chunks.add("second-".getBytes(StandardCharsets.UTF_8));
    chunks.add("third".getBytes(StandardCharsets.UTF_8));

    Http2TestClient.Response response = client.postChunked("/echo", chunks);

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("first-second-third");
  }

  @Test
  void resetStreamDoesNotKillConnection() throws Exception {
    start(false);

    client.reset("/echo");

    Http2TestClient.Response response = client.get("/hello");
    assertThat(response.status()).isEqualTo(200);
    assertThat(client.isActive()).isTrue();
  }

  @Test
  void h2cUpgradeNegotiation() throws Exception {
    int port = startServer(false);

    try (Socket socket = new Socket("localhost", port)) {
      socket.setSoTimeout(5000);
      String request = "GET /hello HTTP/1.1\r\n"
              + "Host: localhost:" + port + "\r\n"
              + "Connection: Upgrade, HTTP2-Settings\r\n"
              + "Upgrade: h2c\r\n"
              + "HTTP2-Settings: AAMAAABk\r\n\r\n";
      socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
      socket.getOutputStream().flush();

      assertThat(readStatusLine(socket)).contains("101");
    }
  }

  private static String readStatusLine(Socket socket) throws Exception {
    StringBuilder line = new StringBuilder();
    int previous = -1;
    int current;
    while ((current = socket.getInputStream().read()) != -1) {
      if (previous == '\r' && current == '\n') {
        break;
      }
      if (current != '\r') {
        line.append((char) current);
      }
      previous = current;
    }
    return line.toString();
  }

  @Test
  void requestAfterResetStreamStillWorks() throws Exception {
    start(false);

    client.reset("/echo");
    Http2TestClient.Response response = client.post("/echo", "still-alive".getBytes(StandardCharsets.UTF_8));

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("still-alive");
  }

  @Test
  void requestWithTrailingHeadersIsHandled() throws Exception {
    start(false);

    Http2TestClient.Response response = client.postWithTrailers("/echo",
            "with-trailers".getBytes(StandardCharsets.UTF_8), Map.of("x-checksum", "abc"));

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("with-trailers");
  }

  @Test
  void expectContinueSendsInformationalResponse() throws Exception {
    start(false);

    Http2TestClient.ContinueResponse result = client.expectContinue("/echo",
            "continued-body".getBytes(StandardCharsets.UTF_8));

    assertThat(result.firstStatus()).isEqualTo(100);
    assertThat(result.response().status()).isEqualTo(200);
    assertThat(result.response().bodyAsString()).isEqualTo("continued-body");
  }

  @Test
  void largeResponseIsDeliveredAcrossDataFrames() throws Exception {
    start(false);

    Http2TestClient.Response response = client.get("/large");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).hasSize(256 * 1024);
  }

  @Test
  void unhandledExceptionProducesServerError() throws Exception {
    start(false);

    Http2TestClient.Response response = client.get("/error");

    assertThat(response.status()).isEqualTo(500);
  }

  @Test
  void concurrentStreamsAreServedIndependently() throws Exception {
    start(false);

    ExecutorService requestExecutor = Executors.newFixedThreadPool(8);
    try {
      List<CompletableFuture<Http2TestClient.Response>> futures = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        String path = "/hello?i=" + i;
        futures.add(CompletableFuture.supplyAsync(() -> {
          try {
            return client.get(path);
          }
          catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        }, requestExecutor));
      }
      for (var future : futures) {
        assertThat(future.get().status()).isEqualTo(200);
      }
    }
    finally {
      requestExecutor.shutdownNow();
    }
  }

  @Test
  void virtualThreadExecutorServesRequests() throws Exception {
    startWithVirtualThreads();

    Http2TestClient.Response getResponse = client.get("/hello");
    assertThat(getResponse.status()).isEqualTo(200);
    assertThat(getResponse.bodyAsString()).isEqualTo("hello");

    Http2TestClient.Response postResponse = client.post("/echo", "virtual".getBytes(StandardCharsets.UTF_8));
    assertThat(postResponse.status()).isEqualTo(200);
    assertThat(postResponse.bodyAsString()).isEqualTo("virtual");
  }

  @Test
  void secureGetNegotiatedWithAlpn() throws Exception {
    start(true);

    Http2TestClient.Response response = client.get("/hello");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.bodyAsString()).isEqualTo("hello");
  }

  private void start(boolean secure) throws Exception {
    int port = startServer(secure);
    client = secure ? Http2TestClient.secure(port) : Http2TestClient.cleartext(port);
  }

  private void startWithVirtualThreads() throws Exception {
    int port = startServer(false, true);
    client = Http2TestClient.cleartext(port);
  }

  private int startServer(boolean secure) throws InterruptedException {
    return startServer(secure, false);
  }

  private int startServer(boolean secure, boolean virtualThreads) throws InterruptedException {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

    HttpRequestExecutor requestExecutor;
    if (virtualThreads) {
      requestExecutor = virtualThreadExecutor();
    }
    else {
      executor = Executors.newFixedThreadPool(4);
      requestExecutor = new SimpleHttpRequestExecutor(executor);
    }
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    DispatcherHandler dispatcher = new TestDispatcherHandler(applicationContext);

    NettyRequestConfig requestConfig = NettyRequestConfig.forBuilder(secure)
            .multipartParser(mock())
            .sendErrorHandler(noopSendErrorHandler())
            .build();

    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(requestConfig, applicationContext,
            dispatcher, requestExecutor);

    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setPort(0);
    factory.setHttp2(newHttp2());
    if (secure) {
      factory.setSsl(newSsl());
    }
    factory.setHttpTrafficHandler(trafficHandler);

    server = factory.createWebServer();
    server.start();
    return server.getPort();
  }

  private static HttpRequestExecutor virtualThreadExecutor() {
    try {
      java.lang.reflect.Method startVirtualThread = Thread.class.getMethod("startVirtualThread", Runnable.class);
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

  private static Http2 newHttp2() {
    Http2 http2 = new Http2();
    http2.setEnabled(true);
    return http2;
  }

  private static Ssl newSsl() {
    Ssl ssl = new Ssl();
    ssl.enabled = true;
    ssl.certificate = "classpath:ssl/key1.crt";
    ssl.certificatePrivateKey = "classpath:ssl/key1.pem";
    return ssl;
  }

  private static SendErrorHandler noopSendErrorHandler() {
    return (request, message) -> {
    };
  }

  private static final class TestDispatcherHandler extends DispatcherHandler {

    private TestDispatcherHandler(ApplicationContext context) {
      super(context);
    }

    @Override
    public void handleRequest(HttpContext context) throws Exception {
      String path = context.getRequestURI();
      if ("/echo".equals(path)) {
        byte[] body = readAll(context.getInputStream());
        writeResponse(context, body);
      }
      else if ("/large".equals(path)) {
        writeResponse(context, new byte[256 * 1024]);
      }
      else if ("/error".equals(path)) {
        throw new IllegalStateException("boom");
      }
      else {
        writeResponse(context, "hello".getBytes(StandardCharsets.UTF_8));
      }
      requestCompleted(context, null);
    }

    private static void writeResponse(HttpContext context, byte[] body) throws Exception {
      context.setContentType(MediaType.TEXT_PLAIN);
      context.setContentLength(body.length);
      context.getOutputStream().write(body);
    }

    private static byte[] readAll(InputStream inputStream) throws Exception {
      return inputStream.readAllBytes();
    }

  }

}
