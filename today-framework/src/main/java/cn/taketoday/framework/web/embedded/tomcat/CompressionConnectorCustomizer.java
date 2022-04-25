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

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;

import cn.taketoday.framework.web.server.Compression;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures compression support on the given
 * Connector.
 *
 * @author Brian Clozel
 */
class CompressionConnectorCustomizer implements TomcatConnectorCustomizer {
  @Nullable
  private final Compression compression;

  CompressionConnectorCustomizer(@Nullable Compression compression) {
    this.compression = compression;
  }

  @Override
  public void customize(Connector connector) {
    if (this.compression != null && this.compression.isEnabled()) {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractHttp11Protocol) {
        customize((AbstractHttp11Protocol<?>) handler, compression);
      }
    }
  }

  private void customize(AbstractHttp11Protocol<?> protocol, Compression compression) {
    protocol.setCompression("on");
    protocol.setCompressionMinSize(getMinResponseSize(compression));
    protocol.setCompressibleMimeType(getMimeTypes(compression));
    if (compression.getExcludedUserAgents() != null) {
      protocol.setNoCompressionUserAgents(getExcludedUserAgents(compression));
    }
  }

  private int getMinResponseSize(Compression compression) {
    return (int) compression.getMinResponseSize().toBytes();
  }

  private String getMimeTypes(Compression compression) {
    return StringUtils.arrayToCommaDelimitedString(compression.getMimeTypes());
  }

  private String getExcludedUserAgents(Compression compression) {
    return StringUtils.arrayToCommaDelimitedString(compression.getExcludedUserAgents());
  }

}
