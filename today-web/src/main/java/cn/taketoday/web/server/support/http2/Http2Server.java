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

package cn.taketoday.web.server.support.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * An HTTP/2 Server that responds to requests with a Hello World. Once started, you can test the
 * server with the example client.
 */
public final class Http2Server {

  static final boolean SSL = System.getProperty("ssl") != null;

  static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

  public static void main(String[] args) throws Exception {
    // Configure SSL.
    final SslContext sslCtx;
    if (SSL) {
      SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
              .sslProvider(provider)
              /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
               * Please refer to the HTTP/2 specification for cipher requirements. */
              .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
              .applicationProtocolConfig(new ApplicationProtocolConfig(
                      Protocol.ALPN,
                      // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                      SelectorFailureBehavior.NO_ADVERTISE,
                      // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                      SelectedListenerFailureBehavior.ACCEPT,
                      ApplicationProtocolNames.HTTP_2,
                      ApplicationProtocolNames.HTTP_1_1))
              .build();
    }
    else {
      sslCtx = null;
    }
    // Configure the server.
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.group(group)
              .channel(NioServerSocketChannel.class)
              .handler(new LoggingHandler(LogLevel.INFO))
              .childHandler(new Http2ServerInitializer(sslCtx));

      Channel ch = b.bind(PORT).sync().channel();

      System.err.println("Open your HTTP/2-enabled web browser and navigate to " +
              (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');

      ch.closeFuture().sync();
    }
    finally {
      group.shutdownGracefully();
    }
  }
}