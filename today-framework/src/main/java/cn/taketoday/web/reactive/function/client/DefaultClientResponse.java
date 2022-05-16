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

package cn.taketoday.web.reactive.function.client;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.util.MimeType;
import cn.taketoday.web.reactive.function.BodyExtractor;
import cn.taketoday.web.reactive.function.BodyExtractors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link ClientResponse}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 4.0
 */
class DefaultClientResponse implements ClientResponse {

  private static final byte[] EMPTY = new byte[0];

  private final ClientHttpResponse response;

  private final Headers headers;

  private final ExchangeStrategies strategies;

  private final String logPrefix;

  private final String requestDescription;

  private final Supplier<HttpRequest> requestSupplier;

  private final BodyExtractor.Context bodyExtractorContext;

  public DefaultClientResponse(ClientHttpResponse response, ExchangeStrategies strategies,
          String logPrefix, String requestDescription, Supplier<HttpRequest> requestSupplier) {

    this.response = response;
    this.strategies = strategies;
    this.headers = new DefaultHeaders();
    this.logPrefix = logPrefix;
    this.requestDescription = requestDescription;
    this.requestSupplier = requestSupplier;
    this.bodyExtractorContext = new BodyExtractor.Context() {
      @Override
      public List<HttpMessageReader<?>> messageReaders() {
        return strategies.messageReaders();
      }

      @Override
      public Optional<ServerHttpResponse> serverResponse() {
        return Optional.empty();
      }

      @Override
      public Map<String, Object> hints() {
        return Hints.from(Hints.LOG_PREFIX_HINT, logPrefix);
      }
    };
  }

  @Override
  public ExchangeStrategies strategies() {
    return this.strategies;
  }

  @Override
  public HttpStatusCode statusCode() {
    return this.response.getStatusCode();
  }

  @Override
  public int rawStatusCode() {
    return this.response.getRawStatusCode();
  }

  @Override
  public Headers headers() {
    return this.headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> cookies() {
    return this.response.getCookies();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
    T result = extractor.extract(response, bodyExtractorContext);
    if (result instanceof Mono<?> mono) {
      return (T) mono.checkpoint("Body from " + requestDescription + " [DefaultClientResponse]");
    }
    else if (result instanceof Flux<?> flux) {
      return (T) flux.checkpoint("Body from " + requestDescription + " [DefaultClientResponse]");
    }
    else {
      return result;
    }
  }

  @Override
  public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
    return body(BodyExtractors.toMono(elementClass));
  }

  @Override
  public <T> Mono<T> bodyToMono(TypeReference<T> elementTypeRef) {
    return body(BodyExtractors.toMono(elementTypeRef));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
    return elementClass.equals(DataBuffer.class) ?
           (Flux<T>) body(BodyExtractors.toDataBuffers()) : body(BodyExtractors.toFlux(elementClass));
  }

  @Override
  public <T> Flux<T> bodyToFlux(TypeReference<T> elementTypeRef) {
    return body(BodyExtractors.toFlux(elementTypeRef));
  }

  @Override
  public Mono<Void> releaseBody() {
    return body(BodyExtractors.toDataBuffers()).map(DataBufferUtils::release).then();
  }

  @Override
  public Mono<ResponseEntity<Void>> toBodilessEntity() {
    return releaseBody().then(WebClientUtils.mapToEntity(this, Mono.empty()));
  }

  @Override
  public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
    return WebClientUtils.mapToEntity(this, bodyToMono(bodyType));
  }

  @Override
  public <T> Mono<ResponseEntity<T>> toEntity(TypeReference<T> bodyTypeReference) {
    return WebClientUtils.mapToEntity(this, bodyToMono(bodyTypeReference));
  }

  @Override
  public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
    return WebClientUtils.mapToEntityList(this, bodyToFlux(elementClass));
  }

  @Override
  public <T> Mono<ResponseEntity<List<T>>> toEntityList(TypeReference<T> elementTypeRef) {
    return WebClientUtils.mapToEntityList(this, bodyToFlux(elementTypeRef));
  }

  @Override
  public Mono<WebClientResponseException> createException() {
    return bodyToMono(byte[].class)
            .defaultIfEmpty(EMPTY)
            .onErrorReturn(ex -> !(ex instanceof Error), EMPTY)
            .map(bodyBytes -> {
              HttpRequest request = this.requestSupplier.get();
              Charset charset = headers().contentType().map(MimeType::getCharset).orElse(null);
              int statusCode = rawStatusCode();
              HttpStatus httpStatus = HttpStatus.resolve(statusCode);
              if (httpStatus != null) {
                return WebClientResponseException.create(
                        statusCode,
                        httpStatus.getReasonPhrase(),
                        headers().asHttpHeaders(),
                        bodyBytes,
                        charset,
                        request);
              }
              else {
                return new UnknownHttpStatusCodeException(
                        statusCode,
                        headers().asHttpHeaders(),
                        bodyBytes,
                        charset,
                        request);
              }
            });
  }

  @Override
  public <T> Mono<T> createError() {
    return createException().flatMap(Mono::error);
  }

  @Override
  public String logPrefix() {
    return this.logPrefix;
  }

  // Used by DefaultClientResponseBuilder
  HttpRequest request() {
    return this.requestSupplier.get();
  }

  private class DefaultHeaders implements Headers {

    private final HttpHeaders httpHeaders =
            HttpHeaders.readOnlyHttpHeaders(response.getHeaders());

    @Override
    public OptionalLong contentLength() {
      return toOptionalLong(this.httpHeaders.getContentLength());
    }

    @Override
    public Optional<MediaType> contentType() {
      return Optional.ofNullable(this.httpHeaders.getContentType());
    }

    @Override
    public List<String> header(String headerName) {
      List<String> headerValues = this.httpHeaders.get(headerName);
      return (headerValues != null ? headerValues : Collections.emptyList());
    }

    @Override
    public HttpHeaders asHttpHeaders() {
      return this.httpHeaders;
    }

    private OptionalLong toOptionalLong(long value) {
      return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
    }
  }

}
