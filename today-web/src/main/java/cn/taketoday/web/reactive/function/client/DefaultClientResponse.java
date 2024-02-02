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

package cn.taketoday.web.reactive.function.client;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.reactive.function.BodyExtractor;
import cn.taketoday.web.reactive.function.BodyExtractors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link ClientResponse}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultClientResponse implements ClientResponse {

  private static final byte[] EMPTY = Constant.EMPTY_BYTES;

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
    this.logPrefix = logPrefix;
    this.strategies = strategies;
    this.headers = new DefaultHeaders();
    this.requestSupplier = requestSupplier;
    this.requestDescription = requestDescription;
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
    return strategies;
  }

  @Override
  public HttpStatusCode statusCode() {
    return response.getStatusCode();
  }

  @Override
  public int rawStatusCode() {
    return response.getRawStatusCode();
  }

  @Override
  public Headers headers() {
    return headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> cookies() {
    return response.getCookies();
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
  public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
    return body(BodyExtractors.toMono(elementTypeRef));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
    return elementClass.equals(DataBuffer.class) ?
           (Flux<T>) body(BodyExtractors.toDataBuffers()) : body(BodyExtractors.toFlux(elementClass));
  }

  @Override
  public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
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
  public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference) {
    return WebClientUtils.mapToEntity(this, bodyToMono(bodyTypeReference));
  }

  @Override
  public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
    return WebClientUtils.mapToEntityList(this, bodyToFlux(elementClass));
  }

  @Override
  public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
    return WebClientUtils.mapToEntityList(this, bodyToFlux(elementTypeRef));
  }

  @Override
  public Mono<WebClientResponseException> createException() {
    return bodyToMono(byte[].class)
            .defaultIfEmpty(EMPTY)
            .onErrorReturn(ex -> !(ex instanceof Error), EMPTY)
            .map(bodyBytes -> {
              HttpRequest request = requestSupplier.get();
              Optional<MediaType> mediaType = headers().contentType();
              Charset charset = mediaType.map(MimeType::getCharset).orElse(null);
              HttpStatusCode statusCode = statusCode();

              WebClientResponseException exception;
              if (statusCode instanceof HttpStatus httpStatus) {
                exception = WebClientResponseException.create(
                        statusCode,
                        httpStatus.getReasonPhrase(),
                        headers().asHttpHeaders(),
                        bodyBytes,
                        charset,
                        request);
              }
              else {
                exception = new UnknownHttpStatusCodeException(
                        statusCode,
                        headers().asHttpHeaders(),
                        bodyBytes,
                        charset,
                        request);
              }
              exception.setBodyDecodeFunction(initDecodeFunction(bodyBytes, mediaType.orElse(null)));
              return exception;
            });
  }

  private Function<ResolvableType, ?> initDecodeFunction(@Nullable byte[] body, @Nullable MediaType contentType) {
    return targetType -> {
      if (body == null || body.length == 0) {
        return null;
      }
      Decoder<?> decoder = null;
      for (HttpMessageReader<?> reader : strategies().messageReaders()) {
        if (reader.canRead(targetType, contentType)) {
          if (reader instanceof DecoderHttpMessageReader<?> decoderReader) {
            decoder = decoderReader.getDecoder();
            break;
          }
        }
      }
      Assert.state(decoder != null, "No suitable decoder");
      DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(body);
      return decoder.decode(buffer, targetType, null, Collections.emptyMap());
    };
  }

  @Override
  public <T> Mono<T> createError() {
    return createException().flatMap(Mono::error);
  }

  @Override
  public String logPrefix() {
    return logPrefix;
  }

  // Used by DefaultClientResponseBuilder
  HttpRequest request() {
    return requestSupplier.get();
  }

  private class DefaultHeaders implements Headers {

    private final HttpHeaders httpHeaders = response.getHeaders().asReadOnly();

    @Override
    public OptionalLong contentLength() {
      return toOptionalLong(httpHeaders.getContentLength());
    }

    @Override
    public Optional<MediaType> contentType() {
      return Optional.ofNullable(httpHeaders.getContentType());
    }

    @Override
    public List<String> header(String headerName) {
      List<String> headerValues = httpHeaders.get(headerName);
      return (headerValues != null ? headerValues : Collections.emptyList());
    }

    @Override
    public HttpHeaders asHttpHeaders() {
      return httpHeaders;
    }

    private OptionalLong toOptionalLong(long value) {
      return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
    }
  }

}
