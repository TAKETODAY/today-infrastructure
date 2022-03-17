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

package cn.taketoday.framework.web.embedded.undertow;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.framework.web.server.Compression;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.InvalidMimeTypeException;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import io.undertow.attribute.RequestHeaderAttribute;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

/**
 * {@link HttpHandlerFactory} that adds a compression handler.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class CompressionHttpHandlerFactory implements HttpHandlerFactory {

  private final Compression compression;

  CompressionHttpHandlerFactory(Compression compression) {
    this.compression = compression;
  }

  @Override
  public HttpHandler getHandler(HttpHandler next) {
    if (!this.compression.getEnabled()) {
      return next;
    }
    ContentEncodingRepository repository = new ContentEncodingRepository();
    repository.addEncodingHandler("gzip", new GzipEncodingProvider(), 50,
            Predicates.and(getCompressionPredicates(this.compression)));
    return new EncodingHandler(repository).setNext(next);
  }

  private static Predicate[] getCompressionPredicates(Compression compression) {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(new MaxSizePredicate((int) compression.getMinResponseSize().toBytes()));
    predicates.add(new CompressibleMimeTypePredicate(compression.getMimeTypes()));
    if (compression.getExcludedUserAgents() != null) {
      for (String agent : compression.getExcludedUserAgents()) {
        RequestHeaderAttribute agentHeader = new RequestHeaderAttribute(new HttpString(HttpHeaders.USER_AGENT));
        predicates.add(Predicates.not(Predicates.regex(agentHeader, agent)));
      }
    }
    return predicates.toArray(new Predicate[0]);
  }

  /**
   * Predicate used to match specific mime types.
   */
  private static class CompressibleMimeTypePredicate implements Predicate {

    private final List<MimeType> mimeTypes;

    CompressibleMimeTypePredicate(String[] mimeTypes) {
      this.mimeTypes = new ArrayList<>(mimeTypes.length);
      for (String mimeTypeString : mimeTypes) {
        this.mimeTypes.add(MimeTypeUtils.parseMimeType(mimeTypeString));
      }
    }

    @Override
    public boolean resolve(HttpServerExchange value) {
      String contentType = value.getResponseHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
      if (contentType != null) {
        try {
          MimeType parsed = MimeTypeUtils.parseMimeType(contentType);
          for (MimeType mimeType : this.mimeTypes) {
            if (mimeType.isCompatibleWith(parsed)) {
              return true;
            }
          }
        }
        catch (InvalidMimeTypeException ex) {
          return false;
        }
      }
      return false;
    }

  }

  /**
   * Predicate that returns true if the Content-Size of a request is above a given value
   * or is missing.
   */
  private static class MaxSizePredicate implements Predicate {

    private final Predicate maxContentSize;

    MaxSizePredicate(int size) {
      this.maxContentSize = Predicates.requestLargerThan(size);
    }

    @Override
    public boolean resolve(HttpServerExchange value) {
      if (value.getResponseHeaders().contains(Headers.CONTENT_LENGTH)) {
        return this.maxContentSize.resolve(value);
      }
      return true;
    }

  }

}
