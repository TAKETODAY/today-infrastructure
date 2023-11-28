/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.embedded.netty;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Arrays;

import cn.taketoday.core.ssl.DefaultSslBundleRegistry;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.core.ssl.pem.PemSslStoreBundle;
import cn.taketoday.core.ssl.pem.PemSslStoreDetails;
import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import cn.taketoday.framework.web.server.PortInUseException;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ReactorResourceFactory;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.http.server.reactive.ReactorHttpHandlerAdapter;
import cn.taketoday.web.reactive.function.BodyInserters;
import cn.taketoday.web.reactive.function.client.WebClient;
import io.netty.channel.Channel;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactorNettyReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @author Chris Bono
 */
class ReactorNettyReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

  @Test
  void exceptionIsThrownWhenPortIsAlreadyInUse() {
    AbstractReactiveWebServerFactory factory = getFactory();
    factory.setPort(0);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    factory.setPort(this.webServer.getPort());
    assertThatExceptionOfType(PortInUseException.class).isThrownBy(factory.getWebServer(new EchoHandler())::start)
            .satisfies(this::portMatchesRequirement)
            .withCauseInstanceOf(Throwable.class);
  }

  @Test
  void getPortWhenDisposableServerPortOperationIsUnsupportedReturnsMinusOne() {
    ReactorNettyReactiveWebServerFactory factory = new NoPortNettyReactiveWebServerFactory(0);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    assertThat(this.webServer.getPort()).isEqualTo(-1);
  }

  @Test
  void resourceFactoryAndWebServerLifecycle() {
    ReactorNettyReactiveWebServerFactory factory = getFactory();
    factory.setPort(0);
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    factory.setResourceFactory(resourceFactory);
    this.webServer = factory.getWebServer(new EchoHandler());
    assertThatNoException().isThrownBy(() -> {
      resourceFactory.start();
      this.webServer.start();
      this.webServer.stop();
      resourceFactory.stop();
      resourceFactory.start();
      this.webServer.start();
    });
  }

  private void portMatchesRequirement(PortInUseException exception) {
    assertThat(exception.getPort()).isEqualTo(this.webServer.getPort());
  }

  @Test
  void nettyCustomizers() {
    ReactorNettyReactiveWebServerFactory factory = getFactory();
    ReactorNettyServerCustomizer[] customizers = new ReactorNettyServerCustomizer[2];
    for (int i = 0; i < customizers.length; i++) {
      customizers[i] = mock(ReactorNettyServerCustomizer.class);
      given(customizers[i].apply(any(HttpServer.class))).will((invocation) -> invocation.getArgument(0));
    }
    factory.setServerCustomizers(Arrays.asList(customizers[0], customizers[1]));
    this.webServer = factory.getWebServer(new EchoHandler());
    InOrder ordered = inOrder((Object[]) customizers);
    for (ReactorNettyServerCustomizer customizer : customizers) {
      ordered.verify(customizer).apply(any(HttpServer.class));
    }
  }

  @Test
  void useForwardedHeaders() {
    ReactorNettyReactiveWebServerFactory factory = getFactory();
    factory.setUseForwardHeaders(true);
    assertForwardHeaderIsUsed(factory);
  }

  @Test
  void whenSslIsConfiguredWithAValidAliasARequestSucceeds() {
    Mono<String> result = testSslWithAlias("test-alias");
    StepVerifier.create(result).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
  }

  @Test
  void whenSslBundleIsUpdatedThenSslIsReloaded() {
    DefaultSslBundleRegistry bundles = new DefaultSslBundleRegistry("bundle1", createSslBundle("1.key", "1.crt"));
    Mono<String> result = testSslWithBundle(bundles, "bundle1");
    StepVerifier.create(result).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
    bundles.updateBundle("bundle1", createSslBundle("2.key", "2.crt"));
    Mono<String> result2 = executeSslRequest();
    StepVerifier.create(result2).expectNext("Hello World").expectComplete().verify(Duration.ofSeconds(30));
  }

  @Test
  void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() {
    ReactorNettyReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    WebClient webClient = getWebClient(this.webServer.getPort()).build();
    this.webServer.shutDownGracefully((result) -> {
    });
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
      blockingHandler.stopBlocking();
      try {
        webClient.get().retrieve().toBodilessEntity().block();
        return false;
      }
      catch (RuntimeException ex) {
        return ex.getCause() instanceof ConnectException;
      }
    });
    this.webServer.stop();
  }

  @Override
  @Test
  @Disabled("Reactor Netty does not support mutiple ports")
  protected void startedLogMessageWithMultiplePorts() {
  }

  protected Mono<String> testSslWithAlias(String alias) {
    String keyStore = "classpath:test.jks";
    String keyPassword = "password";
    ReactorNettyReactiveWebServerFactory factory = getFactory();
    Ssl ssl = new Ssl();
    ssl.setKeyStore(keyStore);
    ssl.setKeyPassword(keyPassword);
    ssl.setKeyAlias(alias);
    factory.setSsl(ssl);
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    return executeSslRequest();
  }

  private Mono<String> testSslWithBundle(SslBundles sslBundles, String bundle) {
    ReactorNettyReactiveWebServerFactory factory = getFactory();
    factory.setSslBundles(sslBundles);
    factory.setSsl(Ssl.forBundle(bundle));
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    return executeSslRequest();
  }

  private Mono<String> executeSslRequest() {
    ReactorClientHttpConnector connector = buildTrustAllSslConnector();
    WebClient client = WebClient.builder()
            .baseUrl("https://localhost:" + this.webServer.getPort())
            .clientConnector(connector)
            .build();
    return client.post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
  }

  @Override
  protected ReactorNettyReactiveWebServerFactory getFactory() {
    return new ReactorNettyReactiveWebServerFactory(0);
  }

  @Override
  protected String startedLogMessage() {
    return ((ReactorNettyWebServer) this.webServer).getStartedLogMessage();
  }

  @Override
  protected void addConnector(int port, AbstractReactiveWebServerFactory factory) {
    throw new UnsupportedOperationException("Reactor Netty does not support multiple ports");
  }

  private static SslBundle createSslBundle(String key, String certificate) {
    return SslBundle.of(new PemSslStoreBundle(
            new PemSslStoreDetails(null, "classpath:cn/taketoday/framework/web/embedded/netty/" + certificate,
                    "classpath:cn/taketoday/framework/web/embedded/netty/" + key),
            null));
  }

  static class NoPortNettyReactiveWebServerFactory extends ReactorNettyReactiveWebServerFactory {

    NoPortNettyReactiveWebServerFactory(int port) {
      super(port);
    }

    @Override
    ReactorNettyWebServer createNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
            Duration lifecycleTimeout, Shutdown shutdown) {
      return new NoPortNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown);
    }

  }

  static class NoPortNettyWebServer extends ReactorNettyWebServer {

    NoPortNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter, Duration lifecycleTimeout,
            Shutdown shutdown) {
      super(httpServer, handlerAdapter, lifecycleTimeout, shutdown, null);
    }

    @Override
    DisposableServer startHttpServer() {
      return new NoPortDisposableServer(super.startHttpServer());
    }

  }

  static class NoPortDisposableServer implements DisposableServer {

    private final DisposableServer delegate;

    NoPortDisposableServer(DisposableServer delegate) {
      this.delegate = delegate;
    }

    @Override
    public SocketAddress address() {
      return this.delegate.address();
    }

    @Override
    public String host() {
      return this.delegate.host();
    }

    @Override
    public String path() {
      return this.delegate.path();
    }

    @Override
    public Channel channel() {
      return this.delegate.channel();
    }

    @Override
    public void dispose() {
      this.delegate.dispose();
    }

    @Override
    public void disposeNow() {
      this.delegate.disposeNow();
    }

    @Override
    public void disposeNow(Duration timeout) {
      this.delegate.disposeNow(timeout);
    }

    @Override
    public CoreSubscriber<Void> disposeSubscriber() {
      return this.delegate.disposeSubscriber();
    }

    @Override
    public boolean isDisposed() {
      return this.delegate.isDisposed();
    }

    @Override
    public Mono<Void> onDispose() {
      return this.delegate.onDispose();
    }

    @Override
    public DisposableChannel onDispose(Disposable onDispose) {
      return this.delegate.onDispose(onDispose);
    }

  }

}
