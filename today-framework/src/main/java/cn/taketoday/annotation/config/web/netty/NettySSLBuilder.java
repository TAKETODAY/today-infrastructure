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

package cn.taketoday.annotation.config.web.netty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.lang.Assert;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import static cn.taketoday.framework.web.server.Ssl.ClientAuth.map;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/24 15:09
 */
public class NettySSLBuilder {

  public static SslContext build(ServerProperties.NettySSL ssl, ResourceLoader resourceLoader) {
    Resource privateKeyResource = resourceLoader.getResource(ssl.privateKey);
    Resource publicKeyResource = resourceLoader.getResource(ssl.publicKey);

    Assert.state(publicKeyResource.exists(), "publicKey not found");
    Assert.state(privateKeyResource.exists(), "privateKey not found");

    try (InputStream publicKeyStream = publicKeyResource.getInputStream();
            InputStream privateKeyStream = privateKeyResource.getInputStream()) {
      return SslContextBuilder.forServer(publicKeyStream, privateKeyStream, ssl.keyPassword)
              .ciphers(ssl.ciphers != null ? Arrays.asList(ssl.ciphers) : null)
              .clientAuth(map(ssl.clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE))
              .protocols(ssl.enabledProtocols)
              .build();
    }
    catch (IOException e) {
      throw new IllegalStateException("publicKey or publicKey resource I/O error", e);
    }
  }

}
