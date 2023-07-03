/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.framework.web.server.Compression;
import cn.taketoday.util.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompressionConnectorCustomizer}
 *
 * @author Rudy Adams
 */
class CompressionConnectorCustomizerTests {

  private static final int MIN_SIZE = 100;

  private final String[] mimeTypes = { "text/html", "text/xml", "text/xhtml" };

  private final String[] excludedUserAgents = { "SomeUserAgent", "AnotherUserAgent" };

  private Compression compression;

  @BeforeEach
  void setup() {
    this.compression = new Compression();
    this.compression.setEnabled(true);
    this.compression.setMinResponseSize(DataSize.ofBytes(MIN_SIZE));
    this.compression.setMimeTypes(this.mimeTypes);
    this.compression.setExcludedUserAgents(this.excludedUserAgents);
  }

  @Test
  void shouldCustomizeCompression() throws LifecycleException {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    Http2Protocol upgradeProtocol = new Http2Protocol();
    upgradeProtocol.setHttp11Protocol((AbstractHttp11Protocol<?>) connector.getProtocolHandler());
    connector.addUpgradeProtocol(upgradeProtocol);
    CompressionConnectorCustomizer.customize(connector, compression);
    AbstractHttp11Protocol<?> abstractHttp11Protocol = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
    compressionOn(abstractHttp11Protocol.getCompression());
    minSize(abstractHttp11Protocol.getCompressionMinSize());
    mimeType(abstractHttp11Protocol.getCompressibleMimeTypes());
    excludedUserAgents(abstractHttp11Protocol.getNoCompressionUserAgents());
  }

  private void compressionOn(String compression) {
    assertThat(compression).isEqualTo("on");
  }

  private void minSize(int minSize) {
    assertThat(minSize).isEqualTo(MIN_SIZE);
  }

  private void mimeType(String[] mimeTypes) {
    assertThat(mimeTypes).isEqualTo(this.mimeTypes);
  }

  private void excludedUserAgents(String combinedUserAgents) {
    assertThat(combinedUserAgents).isEqualTo("SomeUserAgent,AnotherUserAgent");
  }

}
