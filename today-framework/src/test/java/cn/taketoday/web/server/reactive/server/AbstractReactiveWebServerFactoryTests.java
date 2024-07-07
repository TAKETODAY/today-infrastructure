/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.server.reactive.server;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.web.server.Compression;
import cn.taketoday.web.server.GracefulShutdownResult;
import cn.taketoday.web.server.Http2;
import cn.taketoday.web.server.Shutdown;
import cn.taketoday.web.server.Ssl;
import cn.taketoday.web.server.WebServer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.util.DataSize;
import cn.taketoday.web.reactive.function.BodyInserters;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.reactive.function.client.WebClientRequestException;
import cn.taketoday.web.server.reactive.AbstractReactiveWebServerFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.NettyPipeline;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Base for testing classes that extends {@link AbstractReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 */
public abstract class AbstractReactiveWebServerFactoryTests {

  protected WebServer webServer;

  @AfterEach
  void tearDown() {
    if (this.webServer != null) {
      try {
        this.webServer.stop();
        try {
          this.webServer.destroy();
        }
        catch (Exception ex) {
          // Ignore
        }
      }
      catch (Exception ex) {
        // Ignore
      }
    }
  }

  protected abstract AbstractReactiveWebServerFactory getFactory();

  @Test
  void specificPort() throws Exception {
    AbstractReactiveWebServerFactory factory = getFactory();
    int specificPort = doWithRetry(() -> {
      factory.setPort(0);
      this.webServer = factory.getWebServer(new EchoHandler());
      this.webServer.start();
      return this.webServer.getPort();
    });
    Mono<String> result = getWebClient(this.webServer.getPort()).build()
            .post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
    assertThat(this.webServer.getPort()).isEqualTo(specificPort);
  }

  @Test
  protected void restartAfterStop() throws Exception {
    AbstractReactiveWebServerFactory factory = getFactory();
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    int port = this.webServer.getPort();
    assertThat(getResponse(port, "/test")).isEqualTo("Hello World");
    this.webServer.stop();
    assertThatException().isThrownBy(() -> getResponse(port, "/test"));
    this.webServer.start();
    assertThat(getResponse(this.webServer.getPort(), "/test")).isEqualTo("Hello World");
  }

  private String getResponse(int port, String uri) {
    WebClient webClient = getWebClient(port).build();
    Mono<String> result = webClient.post()
            .uri(uri)
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    return result.block(Duration.ofSeconds(30));
  }

  @Test
  void portIsMinusOneWhenConnectionIsClosed() {
    AbstractReactiveWebServerFactory factory = getFactory();
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    assertThat(this.webServer.getPort()).isGreaterThan(0);
    this.webServer.destroy();
    assertThat(this.webServer.getPort()).isEqualTo(-1);
  }

  @Test
  void basicSslFromClassPath() {
    testBasicSslWithKeyStore("classpath:test.jks", "password");
  }

  @Test
  void basicSslFromFileSystem() {
    testBasicSslWithKeyStore("src/test/resources/test.jks", "password");

  }

  protected final void testBasicSslWithKeyStore(String keyStore, String keyPassword) {
    AbstractReactiveWebServerFactory factory = getFactory();
    Ssl ssl = new Ssl();
    ssl.keyStore = (keyStore);
    ssl.keyPassword = (keyPassword);
    ssl.keyStorePassword = ("secret");
    factory.setSsl(ssl);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    ReactorClientHttpConnector connector = buildTrustAllSslConnector();
    WebClient client = WebClient.builder()
            .baseURI("https://localhost:" + this.webServer.getPort())
            .clientConnector(connector)
            .build();
    Mono<String> result = client.post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
  }

  @Test
  void sslWithValidAlias() {
    String keyStore = "classpath:test.jks";
    String keyPassword = "password";
    AbstractReactiveWebServerFactory factory = getFactory();
    Ssl ssl = new Ssl();
    ssl.keyStore = (keyStore);
    ssl.keyStorePassword = ("secret");
    ssl.keyPassword = (keyPassword);
    ssl.keyAlias = ("test-alias");
    factory.setSsl(ssl);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    ReactorClientHttpConnector connector = buildTrustAllSslConnector();
    WebClient client = WebClient.builder()
            .baseURI("https://localhost:" + this.webServer.getPort())
            .clientConnector(connector)
            .build();

    Mono<String> result = client.post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);

    StepVerifier.create(result).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
  }

  @Test
  void sslWithInvalidAliasFailsDuringStartup() {
    String keyStore = "classpath:test.jks";
    String keyPassword = "password";
    AbstractReactiveWebServerFactory factory = getFactory();
    Ssl ssl = new Ssl();
    ssl.keyStore = (keyStore);
    ssl.keyPassword = (keyPassword);
    ssl.keyAlias = ("test-alias-404");
    factory.setSsl(ssl);
    assertThatSslWithInvalidAliasCallFails(() -> factory.getWebServer(new EchoHandler()).start());
  }

  protected void assertThatSslWithInvalidAliasCallFails(ThrowingCallable call) {
    assertThatThrownBy(call).hasStackTraceContaining("Keystore does not contain alias 'test-alias-404'");
  }

  protected ReactorClientHttpConnector buildTrustAllSslConnector() {
    Http11SslContextSpec sslContextSpec = Http11SslContextSpec.forClient()
            .configure((builder) -> builder.sslProvider(SslProvider.JDK)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE));
    HttpClient client = HttpClient.create().wiretap(true).secure((spec) -> spec.sslContext(sslContextSpec));
    return new ReactorClientHttpConnector(client);
  }

  @Test
  void sslWantsClientAuthenticationSucceedsWithClientCertificate() throws Exception {
    Ssl ssl = new Ssl();
    ssl.clientAuth = (Ssl.ClientAuth.WANT);
    ssl.keyStore = ("classpath:test.jks");
    ssl.keyPassword = ("password");
    ssl.keyStorePassword = ("secret");
    ssl.trustStore = ("classpath:test.jks");
    testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector("test.jks", "password"));
  }

  @Test
  void sslWantsClientAuthenticationSucceedsWithoutClientCertificate() {
    Ssl ssl = new Ssl();
    ssl.clientAuth = (Ssl.ClientAuth.WANT);
    ssl.keyStore = ("classpath:test.jks");
    ssl.keyPassword = ("password");
    ssl.trustStore = ("classpath:test.jks");
    ssl.keyStorePassword = ("secret");
    testClientAuthSuccess(ssl, buildTrustAllSslConnector());
  }

  protected ReactorClientHttpConnector buildTrustAllSslWithClientKeyConnector(String keyStoreFile,
          String keyStorePassword) throws Exception {
    KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream stream = new FileInputStream("src/test/resources/" + keyStoreFile)) {
      clientKeyStore.load(stream, "secret".toCharArray());
    }
    KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm());
    clientKeyManagerFactory.init(clientKeyStore, keyStorePassword.toCharArray());

    Http11SslContextSpec sslContextSpec = Http11SslContextSpec.forClient()
            .configure((builder) -> builder.sslProvider(SslProvider.JDK)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .keyManager(clientKeyManagerFactory));
    HttpClient client = HttpClient.create().wiretap(true).secure((spec) -> spec.sslContext(sslContextSpec));
    return new ReactorClientHttpConnector(client);
  }

  protected void testClientAuthSuccess(Ssl sslConfiguration, ReactorClientHttpConnector clientConnector) {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setSsl(sslConfiguration);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    WebClient client = WebClient.builder()
            .baseURI("https://localhost:" + this.webServer.getPort())
            .clientConnector(clientConnector)
            .build();
    Mono<String> result = client.post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
  }

  @Test
  void sslNeedsClientAuthenticationSucceedsWithClientCertificate() throws Exception {
    Ssl ssl = new Ssl();
    ssl.clientAuth = (Ssl.ClientAuth.NEED);
    ssl.keyStore = ("classpath:test.jks");
    ssl.keyStorePassword = ("secret");
    ssl.keyPassword = ("password");
    ssl.trustStore = ("classpath:test.jks");
    testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector("test.jks", "password"));
  }

  @Test
  void sslNeedsClientAuthenticationFailsWithoutClientCertificate() {
    Ssl ssl = new Ssl();
    ssl.clientAuth = (Ssl.ClientAuth.NEED);
    ssl.keyStore = ("classpath:test.jks");
    ssl.keyStorePassword = ("secret");
    ssl.keyPassword = ("password");
    ssl.trustStore = ("classpath:test.jks");
    testClientAuthFailure(ssl, buildTrustAllSslConnector());
  }

  @Test
  void sslWithPemCertificates() throws Exception {
    Ssl ssl = new Ssl();
    ssl.clientAuth = (Ssl.ClientAuth.NEED);
    ssl.certificate = "classpath:test-cert.pem";
    ssl.certificatePrivateKey = ("classpath:test-key.pem");
    ssl.trustCertificate = ("classpath:test-cert.pem");
    testClientAuthSuccess(ssl, buildTrustAllSslWithClientKeyConnector("test.p12", "secret"));
  }

  protected void testClientAuthFailure(Ssl sslConfiguration, ReactorClientHttpConnector clientConnector) {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setSsl(sslConfiguration);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    WebClient client = WebClient.builder()
            .baseURI("https://localhost:" + this.webServer.getPort())
            .clientConnector(clientConnector)
            .build();
    Mono<String> result = client.post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    StepVerifier.create(result).expectError(WebClientRequestException.class).verify(Duration.ofSeconds(10));
  }

  protected WebClient.Builder getWebClient(int port) {
    return getWebClient(HttpClient.create().wiretap(true), port);
  }

  protected WebClient.Builder getWebClient(HttpClient client, int port) {
    InetSocketAddress address = new InetSocketAddress(port);
    String baseUrl = "http://" + address.getHostString() + ":" + address.getPort();
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).baseURI(baseUrl);
  }

  @Test
  protected void compressionOfResponseToGetRequest() {
    WebClient client = prepareCompressionTest();
    ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    assertResponseIsCompressed(response);
  }

  @Test
  protected void compressionOfResponseToPostRequest() {
    WebClient client = prepareCompressionTest();
    ResponseEntity<Void> response = client.post().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    assertResponseIsCompressed(response);
  }

  @Test
  void noCompressionForSmallResponse() {
    Compression compression = new Compression();
    compression.setEnabled(true);
    compression.setMinResponseSize(DataSize.ofBytes(3001));
    WebClient client = prepareCompressionTest(compression);
    ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    assertResponseIsNotCompressed(response);
  }

  @Test
  void noCompressionForMimeType() {
    Compression compression = new Compression();
    compression.setEnabled(true);
    compression.setMimeTypes(new String[] { "application/json" });
    WebClient client = prepareCompressionTest(compression);
    ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    assertResponseIsNotCompressed(response);
  }

  @Test
  protected void noCompressionForUserAgent() {
    Compression compression = new Compression();
    compression.setEnabled(true);
    compression.setExcludedUserAgents(new String[] { "testUserAgent" });
    WebClient client = prepareCompressionTest(compression);
    ResponseEntity<Void> response = client.get()
            .header("User-Agent", "testUserAgent")
            .retrieve()
            .toBodilessEntity()
            .block(Duration.ofSeconds(30));
    assertResponseIsNotCompressed(response);
  }

  @Test
  void noCompressionForResponseWithInvalidContentType() {
    Compression compression = new Compression();
    compression.setEnabled(true);
    compression.setMimeTypes(new String[] { "application/json" });
    WebClient client = prepareCompressionTest(compression, "test~plain");
    ResponseEntity<Void> response = client.get().retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
    assertResponseIsNotCompressed(response);
  }

  @Test
  void whenSslIsEnabledAndNoKeyStoreIsConfiguredThenServerFailsToStart() {
    assertThatIllegalStateException().isThrownBy(() -> testBasicSslWithKeyStore(null, null))
            .withMessageContaining("SSL is enabled but no trust material is configured");
  }

  @Test
  void whenThereAreNoInFlightRequestsShutDownGracefullyReturnsTrueBeforePeriodElapses() throws Exception {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
    this.webServer.shutDownGracefully(result::set);
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> GracefulShutdownResult.IDLE == result.get());
  }

  @Test
  void whenARequestRemainsInFlightThenShutDownGracefullyDoesNotInvokeCallbackUntilTheRequestCompletes()
          throws Exception {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build()
            .get()
            .retrieve()
            .toBodilessEntity();
    AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
    CountDownLatch responseLatch = new CountDownLatch(1);
    request.subscribe((response) -> {
      responseReference.set(response);
      responseLatch.countDown();
    });
    blockingHandler.awaitQueue();
    AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
    this.webServer.shutDownGracefully(result::set);
    assertThat(responseReference.get()).isNull();
    blockingHandler.completeOne();
    assertThat(responseLatch.await(5, TimeUnit.SECONDS)).isTrue();
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> GracefulShutdownResult.IDLE == result.get());
  }

  @Test
  void givenAnInflightRequestWhenTheServerIsStoppedThenGracefulShutdownCallbackIsCalledWithRequestsActive()
          throws Exception {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build()
            .get()
            .retrieve()
            .toBodilessEntity();
    AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
    CountDownLatch responseLatch = new CountDownLatch(1);
    request.subscribe((response) -> {
      responseReference.set(response);
      responseLatch.countDown();
    });
    blockingHandler.awaitQueue();
    AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
    this.webServer.shutDownGracefully(result::set);
    assertThat(responseReference.get()).isNull();
    try {
      this.webServer.stop();
    }
    catch (Exception ex) {
      // Continue
    }
    System.out.println("Stopped");
    Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .until(() -> GracefulShutdownResult.REQUESTS_ACTIVE == result.get());
    blockingHandler.completeOne();
  }

  @Test
  void whenARequestIsActiveAfterGracefulShutdownEndsThenStopWillComplete() throws InterruptedException {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build()
            .get()
            .retrieve()
            .toBodilessEntity();
    AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
    CountDownLatch responseLatch = new CountDownLatch(1);
    request.subscribe((response) -> {
      responseReference.set(response);
      responseLatch.countDown();
    });
    blockingHandler.awaitQueue();
    AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
    this.webServer.shutDownGracefully(result::set);
    this.webServer.stop();
    Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .until(() -> GracefulShutdownResult.REQUESTS_ACTIVE == result.get());
    blockingHandler.completeOne();
  }

  @Test
  void whenARequestIsActiveThenStopWillComplete() throws InterruptedException {
    AbstractReactiveWebServerFactory factory = getFactory();
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    Mono<ResponseEntity<Void>> request = getWebClient(this.webServer.getPort()).build()
            .get()
            .retrieve()
            .toBodilessEntity();
    AtomicReference<ResponseEntity<Void>> responseReference = new AtomicReference<>();
    CountDownLatch responseLatch = new CountDownLatch(1);
    request.subscribe((response) -> {
      responseReference.set(response);
      responseLatch.countDown();
    });
    blockingHandler.awaitQueue();
    try {
      this.webServer.stop();
    }
    catch (Exception ex) {
      // Continue
    }
    blockingHandler.completeOne();
  }

  @Test
  protected void whenHttp2IsEnabledAndSslIsDisabledThenHttp11CanStillBeUsed() {
    AbstractReactiveWebServerFactory factory = getFactory();
    Http2 http2 = new Http2();
    http2.setEnabled(true);
    factory.setHttp2(http2);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    Mono<String> result = getWebClient(this.webServer.getPort()).build()
            .post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
  }

  @Test
  void startedLogMessageWithSinglePort() {
    AbstractReactiveWebServerFactory factory = getFactory();
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    assertThat(startedLogMessage()).matches("(Jetty|Netty|Tomcat|Undertow) started on port "
            + this.webServer.getPort() + "( \\(http(/1.1)?\\))?( with context path '(/)?')?");
  }

  @Test
  protected void startedLogMessageWithMultiplePorts() {
    AbstractReactiveWebServerFactory factory = getFactory();
    addConnector(0, factory);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    assertThat(startedLogMessage()).matches("(Jetty|Tomcat|Undertow) started on ports " + this.webServer.getPort()
            + "( \\(http(/1.1)?\\))?, [0-9]+( \\(http(/1.1)?\\))?( with context path '(/)?')?");
  }

  protected WebClient prepareCompressionTest() {
    Compression compression = new Compression();
    compression.setEnabled(true);
    return prepareCompressionTest(compression);
  }

  protected WebClient prepareCompressionTest(Compression compression) {
    return prepareCompressionTest(compression, MediaType.TEXT_PLAIN_VALUE);
  }

  protected WebClient prepareCompressionTest(Compression compression, String responseContentType) {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setCompression(compression);
    this.webServer = factory.getWebServer(new CharsHandler(3000, responseContentType));
    this.webServer.start();

    HttpClient client = HttpClient.create()
            .wiretap(true)
            .compress(true)
            .doOnConnected((connection) -> connection.channel()
                    .pipeline()
                    .addBefore(NettyPipeline.HttpDecompressor, "CompressionTest", new CompressionDetectionHandler()));
    return getWebClient(client, this.webServer.getPort()).build();
  }

  protected void assertResponseIsCompressed(ResponseEntity<Void> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst("X-Test-Compressed")).isEqualTo("true");
  }

  protected void assertResponseIsNotCompressed(ResponseEntity<Void> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().keySet()).doesNotContain("X-Test-Compressed");
  }

  protected void assertForwardHeaderIsUsed(AbstractReactiveWebServerFactory factory) {
    this.webServer = factory.getWebServer(new XForwardedHandler());
    this.webServer.start();
    String body = getWebClient(this.webServer.getPort()).build()
            .get()
            .header("X-Forwarded-Proto", "https")
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(30));
    assertThat(body).isEqualTo("https");
  }

  private <T> T doWithRetry(Callable<T> action) throws Exception {
    Exception lastFailure = null;
    for (int i = 0; i < 10; i++) {
      try {
        return action.call();
      }
      catch (Exception ex) {
        lastFailure = ex;
      }
    }
    throw new IllegalStateException("Action was not successful in 10 attempts", lastFailure);
  }

  protected final void doWithBlockedPort(BlockedPortAction action) throws Exception {
    try (ServerSocket serverSocket = new ServerSocket()) {
      int blockedPort = doWithRetry(() -> {
        serverSocket.bind(null);
        return serverSocket.getLocalPort();
      });
      action.run(blockedPort);
    }
  }

  protected abstract String startedLogMessage();

  protected abstract void addConnector(int port, AbstractReactiveWebServerFactory factory);

  public interface BlockedPortAction {

    void run(int port);

  }

  protected static class EchoHandler implements HttpHandler {

    public EchoHandler() {
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      response.setStatusCode(HttpStatus.OK);
      return response.writeWith(request.getBody());
    }

  }

  protected static class BlockingHandler implements HttpHandler {

    private final BlockingQueue<Sinks.Empty<Void>> processors = new ArrayBlockingQueue<>(10);

    private volatile boolean blocking = true;

    public BlockingHandler() {

    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      if (this.blocking) {
        Sinks.Empty<Void> completion = Sinks.empty();
        this.processors.add(completion);
        return completion.asMono().then(Mono.empty());
      }
      return Mono.empty();
    }

    public void completeOne() {
      try {
        Sinks.Empty<Void> processor = this.processors.take();
        processor.tryEmitEmpty();
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

    public void awaitQueue() throws InterruptedException {
      while (this.processors.isEmpty()) {
        Thread.sleep(100);
      }
    }

    public void stopBlocking() {
      this.blocking = false;
      this.processors.forEach(Sinks.Empty::tryEmitEmpty);
    }

  }

  static class CompressionDetectionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof HttpResponse response) {
        boolean compressed = response.headers().contains(HttpHeaderNames.CONTENT_ENCODING, "gzip", true);
        if (compressed) {
          response.headers().set("X-Test-Compressed", "true");
        }
      }
      ctx.fireChannelRead(msg);
    }

  }

  static class CharsHandler implements HttpHandler {

    private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

    private final DataBuffer bytes;

    private final String mediaType;

    CharsHandler(int contentSize, String mediaType) {
      char[] chars = new char[contentSize];
      Arrays.fill(chars, 'F');
      this.bytes = factory.wrap(new String(chars).getBytes(StandardCharsets.UTF_8));
      this.mediaType = mediaType;
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      response.setStatusCode(HttpStatus.OK);
      response.getHeaders().set(HttpHeaders.CONTENT_TYPE, this.mediaType);
      response.getHeaders().setContentLength(this.bytes.readableByteCount());
      return response.writeWith(Mono.just(this.bytes));
    }

  }

  static class XForwardedHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      String scheme = request.getURI().getScheme();
      DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
      DataBuffer buffer = bufferFactory.wrap(scheme.getBytes(StandardCharsets.UTF_8));
      return response.writeWith(Mono.just(buffer));
    }

  }

}
