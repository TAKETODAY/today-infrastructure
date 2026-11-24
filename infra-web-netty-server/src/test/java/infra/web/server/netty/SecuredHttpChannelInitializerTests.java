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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SNIHostName;

import infra.core.ssl.SslBundle;
import infra.web.server.Ssl;
import infra.web.server.WebServerSslBundle;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;

import static org.assertj.core.api.Assertions.assertThat;

class SecuredHttpChannelInitializerTests {

  @Test
  void http1PipelineUsesReloadedDefaultBundleForNewChannels() {
    var ssl = new Ssl();
    ssl.handshakeTimeout = Duration.ofSeconds(5);
    var initializer = initializer(ssl, false, bundle("TLSv1.2"), Map.of());
    var before = new EmbeddedChannel(initializer);
    try {
      var beforeHandler = before.pipeline().get(SslHandler.class);
      assertThat(beforeHandler.getHandshakeTimeoutMillis()).isEqualTo(5000);
      assertThat(beforeHandler.engine().getEnabledProtocols()).containsExactly("TLSv1.2");
      assertThat(before.pipeline().get(HttpServerCodec.class)).isNotNull();
      assertThat(before.pipeline().names()).startsWith("SSL-handler", HttpChannelInitializer.HttpCodec);

      initializer.updateSSLBundle("ignored.example", bundle("TLSv1.3"));
      initializer.updateSSLBundle(null, bundle("TLSv1.3"));
      var after = new EmbeddedChannel(initializer);
      try {
        assertThat(after.pipeline().get(SslHandler.class).engine().getEnabledProtocols()).containsExactly("TLSv1.3");
        assertThat(beforeHandler.engine().getEnabledProtocols()).containsExactly("TLSv1.2");
      }
      finally {
        after.finishAndReleaseAll();
      }
    }
    finally {
      before.finishAndReleaseAll();
    }
  }

  @Test
  void serverNameSelectsUpdatedBundleOrFallsBackToDefault() throws Exception {
    var initializer = initializer(new Ssl(), false, bundle("TLSv1.2"), Map.of("named.example", bundle("TLSv1.2")));
    initializer.updateSSLBundle("named.example", bundle("TLSv1.3"));

    assertSelectedProtocol(initializer, "named.example", "TLSv1.3");
    assertSelectedProtocol(initializer, "unknown.example", "TLSv1.2");
  }

  @Test
  void alpnInstallsHttp2PipelineWhenClientAdvertisesH2() throws Exception {
    var initializer = initializer(new Ssl(), true, bundle("TLSv1.2"), Map.of());
    var alpn = new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2);
    var client = newClient("key1", alpn);
    var server = new EmbeddedChannel(initializer);
    try {
      for (int i = 0; i < 10 && server.pipeline().get(Http2FrameCodec.class) == null; i++) {
        transfer(client, server);
        transfer(server, client);
      }
      assertThat(server.pipeline().get(SslHandler.class).applicationProtocol()).isEqualTo(ApplicationProtocolNames.HTTP_2);
      assertThat(server.pipeline().get(Http2FrameCodec.class)).isNotNull();
      assertThat(server.pipeline().get(Http2MultiplexHandler.class)).isNotNull();
      assertThat(server.pipeline().get(HttpServerCodec.class)).isNull();
    }
    finally {
      client.finishAndReleaseAll();
      server.finishAndReleaseAll();
    }
  }

  @Test
  void alpnFallsBackToHttp1WhenClientDoesNotAdvertiseAProtocol() throws Exception {
    var initializer = initializer(new Ssl(), true, bundle("TLSv1.2"), Map.of());
    var server = new EmbeddedChannel(initializer);
    var client = newClient("key1");
    try {
      assertThat(server.pipeline().get(ApplicationProtocolNegotiationHandler.class)).isNotNull();
      for (int i = 0; i < 10 && server.pipeline().get(HttpServerCodec.class) == null; i++) {
        transfer(client, server);
        transfer(server, client);
      }
      assertThat(server.pipeline().get(SslHandler.class).handshakeFuture().isSuccess()).isTrue();
      assertThat(server.pipeline().get(HttpServerCodec.class)).isNotNull();
      assertThat(server.pipeline().get(ApplicationProtocolNegotiationHandler.class)).isNull();
    }
    finally {
      client.finishAndReleaseAll();
      server.finishAndReleaseAll();
    }
  }

  private void assertSelectedProtocol(SecuredHttpChannelInitializer initializer, String host, String protocol) throws Exception {
    var server = new EmbeddedChannel(initializer);
    var client = newClient(host);
    try {
      assertThat(server.pipeline().get(SniHandler.class)).isNotNull();
      transfer(client, server);
      assertThat(server.pipeline().get(SniHandler.class)).isNull();
      assertThat(server.pipeline().get(SslHandler.class).engine().getEnabledProtocols()).containsExactly(protocol);
    }
    finally {
      client.finishAndReleaseAll();
      server.finishAndReleaseAll();
    }
  }

  private static void transfer(EmbeddedChannel from, EmbeddedChannel to) {
    from.runPendingTasks();
    ByteBuf buffer;
    while ((buffer = from.readOutbound()) != null) {
      to.writeInbound(buffer);
    }
  }

  private static EmbeddedChannel newClient(String host) throws Exception {
    return newClient(host, ApplicationProtocolConfig.DISABLED);
  }

  private static EmbeddedChannel newClient(String host, ApplicationProtocolConfig alpn) throws Exception {
    var handler = SslContextBuilder.forClient().sslProvider(SslProvider.JDK)
            .trustManager(SecuredHttpChannelInitializerTests.class.getResourceAsStream("/ssl/key1.crt"))
            .applicationProtocolConfig(alpn)
            .build().newHandler(ByteBufAllocator.DEFAULT, host, 443);
    var parameters = handler.engine().getSSLParameters();
    parameters.setServerNames(List.of(new SNIHostName(host)));
    handler.engine().setSSLParameters(parameters);
    return new EmbeddedChannel(handler);
  }

  private static SslBundle bundle(String protocol) {
    var ssl = new Ssl();
    ssl.certificate = "classpath:ssl/key1.crt";
    ssl.certificatePrivateKey = "classpath:ssl/key1.pem";
    ssl.enabledProtocols = new String[] { protocol };
    if (protocol.equals("TLSv1.2")) {
      ssl.ciphers = new String[] { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256" };
    }
    return WebServerSslBundle.get(ssl);
  }

  private static SecuredHttpChannelInitializer initializer(Ssl ssl, boolean http2, SslBundle bundle,
          Map<String, SslBundle> namedBundles) {
    return new SecuredHttpChannelInitializer(new ChannelInboundHandlerAdapter() {
      @Override
      public boolean isSharable() {
        return true;
      }
    }, new HttpDecoderConfig(), null, http2, ssl, bundle, namedBundles,
            () -> Http2FrameCodecBuilder.forServer().build());
  }

}
