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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CompressionConnectorCustomizer {

  public static void customize(Connector connector, @Nullable Compression compression) {
    if (Compression.isEnabled(compression)) {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractHttp11Protocol<?> http11Protocol) {
        customize(http11Protocol, compression);
      }
    }
  }

  private static void customize(AbstractHttp11Protocol<?> protocol, Compression compression) {
    protocol.setCompression("on");
    protocol.setCompressionMinSize(getMinResponseSize(compression));
    protocol.setCompressibleMimeType(getMimeTypes(compression));
    if (compression.getExcludedUserAgents() != null) {
      protocol.setNoCompressionUserAgents(getExcludedUserAgents(compression));
    }
  }

  private static int getMinResponseSize(Compression compression) {
    return (int) compression.getMinResponseSize().toBytes();
  }

  private static String getMimeTypes(Compression compression) {
    return StringUtils.arrayToCommaDelimitedString(compression.getMimeTypes());
  }

  private static String getExcludedUserAgents(Compression compression) {
    return StringUtils.arrayToCommaDelimitedString(compression.getExcludedUserAgents());
  }

}
