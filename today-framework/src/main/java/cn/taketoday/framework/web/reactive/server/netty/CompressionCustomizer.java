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

package cn.taketoday.framework.web.reactive.server.netty;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import cn.taketoday.framework.web.server.Compression;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.InvalidMimeTypeException;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * Configure the HTTP compression on a Reactor Netty request/response handler.
 *
 * @author Stephane Maldini
 * @author Phillip Webb
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class CompressionCustomizer implements ReactorNettyServerCustomizer {

  private static final CompressionPredicate ALWAYS_COMPRESS = (request, response) -> true;

  private final Compression compression;

  CompressionCustomizer(Compression compression) {
    this.compression = compression;
  }

  @Override
  public HttpServer apply(HttpServer server) {
    if (!this.compression.getMinResponseSize().isNegative()) {
      server = server.compress(this.compression.getMinResponseSize().toBytesInt());
    }
    CompressionPredicate mimeTypes = getMimeTypesPredicate(this.compression.getMimeTypes());
    CompressionPredicate excludedUserAgents = getExcludedUserAgentsPredicate(this.compression.getExcludedUserAgents());
    server = server.compress(mimeTypes.and(excludedUserAgents));
    return server;
  }

  private CompressionPredicate getMimeTypesPredicate(String[] mimeTypeValues) {
    if (ObjectUtils.isEmpty(mimeTypeValues)) {
      return ALWAYS_COMPRESS;
    }
    List<MimeType> mimeTypes = Arrays.stream(mimeTypeValues).map(MimeTypeUtils::parseMimeType).toList();
    return (request, response) -> {
      String contentType = response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE);
      if (StringUtils.isEmpty(contentType)) {
        return false;
      }
      try {
        MimeType contentMimeType = MimeTypeUtils.parseMimeType(contentType);
        return mimeTypes.stream().anyMatch(candidate -> candidate.isCompatibleWith(contentMimeType));
      }
      catch (InvalidMimeTypeException ex) {
        return false;
      }
    };
  }

  private CompressionPredicate getExcludedUserAgentsPredicate(@Nullable String[] excludedUserAgents) {
    if (ObjectUtils.isEmpty(excludedUserAgents)) {
      return ALWAYS_COMPRESS;
    }
    return (request, response) -> {
      HttpHeaders headers = request.requestHeaders();
      return Arrays.stream(excludedUserAgents)
              .noneMatch(candidate -> headers.contains(HttpHeaderNames.USER_AGENT, candidate, true));
    };
  }

  private interface CompressionPredicate extends BiPredicate<HttpServerRequest, HttpServerResponse> {

  }

}
