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

package infra.web.client.reactive;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.core.ParameterizedTypeReference;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.ResponseEntity;
import infra.http.client.reactive.ClientHttpResponse;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.HttpMessageWriter;
import infra.util.MultiValueMap;
import infra.web.reactive.function.BodyExtractor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Represents an HTTP response, as returned by {@link WebClient} and also
 * {@link ExchangeFunction}. Provides access to the response status and
 * headers, and also methods to consume the response body.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ClientResponse {

  /**
   * Return the HTTP status code as an {@link HttpStatusCode} value.
   *
   * @return the HTTP status as an HttpStatusCode value (never {@code null})
   */
  HttpStatusCode statusCode();

  /**
   * Return the (potentially non-standard) status code of this response.
   *
   * @return the HTTP status as an integer value
   * @see #statusCode()
   * @see HttpStatus#resolve(int)
   */
  int rawStatusCode();

  /**
   * Return the headers of this response.
   */
  Headers headers();

  /**
   * Return the cookies of this response.
   */
  MultiValueMap<String, ResponseCookie> cookies();

  /**
   * Return the strategies used to convert the body of this response.
   */
  ExchangeStrategies strategies();

  /**
   * Extract the body with the given {@code BodyExtractor}.
   *
   * @param extractor the {@code BodyExtractor} that reads from the response
   * @param <T> the type of the body returned
   * @return the extracted body
   */
  <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor);

  /**
   * Extract the body to a {@code Mono}.
   *
   * @param elementClass the class of element in the {@code Mono}
   * @param <T> the element type
   * @return a mono containing the body of the given type {@code T}
   */
  <T> Mono<T> bodyToMono(Class<? extends T> elementClass);

  /**
   * Extract the body to a {@code Mono}.
   *
   * @param elementTypeRef the type reference of element in the {@code Mono}
   * @param <T> the element type
   * @return a mono containing the body of the given type {@code T}
   */
  <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

  /**
   * Extract the body to a {@code Flux}.
   *
   * @param elementClass the class of elements in the {@code Flux}
   * @param <T> the element type
   * @return a flux containing the body of the given type {@code T}
   */
  <T> Flux<T> bodyToFlux(Class<? extends T> elementClass);

  /**
   * Extract the body to a {@code Flux}.
   *
   * @param elementTypeRef the type reference of elements in the {@code Flux}
   * @param <T> the element type
   * @return a flux containing the body of the given type {@code T}
   */
  <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

  /**
   * Release the body of this response.
   *
   * @return a completion signal
   * @see DataBufferUtils#release(DataBuffer)
   */
  Mono<Void> releaseBody();

  /**
   * Return this response as a delayed {@code ResponseEntity}.
   *
   * @param bodyClass the expected response body type
   * @param <T> response body type
   * @return {@code Mono} with the {@code ResponseEntity}
   */
  <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

  /**
   * Return this response as a delayed {@code ResponseEntity}.
   *
   * @param bodyTypeReference a type reference describing the expected response body type
   * @param <T> response body type
   * @return {@code Mono} with the {@code ResponseEntity}
   */
  <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

  /**
   * Return this response as a delayed list of {@code ResponseEntity}s.
   *
   * @param elementClass the expected response body list element class
   * @param <T> the type of elements in the list
   * @return {@code Mono} with the list of {@code ResponseEntity}s
   */
  <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

  /**
   * Return this response as a delayed list of {@code ResponseEntity}s.
   *
   * @param elementTypeRef the expected response body list element reference type
   * @param <T> the type of elements in the list
   * @return {@code Mono} with the list of {@code ResponseEntity}s
   */
  <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

  /**
   * Return this response as a delayed {@code ResponseEntity} containing
   * status and headers, but no body. Calling this method will
   * {@linkplain #releaseBody() release} the body of the response.
   *
   * @return {@code Mono} with the bodiless {@code ResponseEntity}
   */
  Mono<ResponseEntity<Void>> toBodilessEntity();

  /**
   * Create a {@link WebClientResponseException} that contains the response
   * status, headers, body, and the originating request.
   *
   * @return a {@code Mono} with the created exception
   */
  Mono<WebClientResponseException> createException();

  /**
   * Create a {@code Mono} that terminates with a
   * {@link WebClientResponseException}, containing the response status,
   * headers, body, and the originating request.
   *
   * @param <T> the reified type
   * @return a {@code Mono} that fails with a
   * {@link WebClientResponseException}.
   * @see #createException()
   */
  <T> Mono<T> createError();

  /**
   * Return a log message prefix to use to correlate messages for this exchange.
   * <p>The prefix is based on {@linkplain ClientRequest#logPrefix()}, which
   * itself is based on the value of the {@link ClientRequest#LOG_ID_ATTRIBUTE
   * LOG_ID_ATTRIBUTE} request attribute, further surrounded with "[" and "]".
   *
   * @return the log message prefix or an empty String if the
   * {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} is not set
   */
  String logPrefix();

  /**
   * Return a builder to mutate this response, for example to change
   * the status, headers, cookies, and replace or transform the body.
   *
   * @return a builder to mutate the response with
   * @since 4.0
   */
  default Builder mutate() {
    return new DefaultClientResponseBuilder(this, true);
  }

  // Static builder methods

  /**
   * Create a builder with the status, headers, and cookies of the given response.
   * <p><strong>Note:</strong> Note that the body in the returned builder is
   * {@link Flux#empty()} by default. To carry over the one from the original
   * response, use {@code otherResponse.bodyToFlux(DataBuffer.class)} or
   * simply use the instance based {@link #mutate()} method.
   *
   * @param other the response to copy the status, headers, and cookies from
   * @return the created builder
   */
  static Builder from(ClientResponse other) {
    return new DefaultClientResponseBuilder(other, false);
  }

  /**
   * Create a response builder with the given status code and using default strategies for
   * reading the body.
   *
   * @param statusCode the status code
   * @return the created builder
   */
  static Builder create(HttpStatus statusCode) {
    return create(statusCode, ExchangeStrategies.withDefaults());
  }

  /**
   * Create a response builder with the given status code and strategies for reading the body.
   *
   * @param statusCode the status code
   * @param strategies the strategies
   * @return the created builder
   */
  static Builder create(HttpStatus statusCode, ExchangeStrategies strategies) {
    return new DefaultClientResponseBuilder(strategies).statusCode(statusCode);
  }

  /**
   * Create a response builder with the given raw status code and strategies for reading the body.
   *
   * @param statusCode the status code
   * @param strategies the strategies
   * @return the created builder
   */
  static Builder create(int statusCode, ExchangeStrategies strategies) {
    return new DefaultClientResponseBuilder(strategies).rawStatusCode(statusCode);
  }

  /**
   * Create a response builder with the given status code and message body readers.
   *
   * @param statusCode the status code
   * @param messageReaders the message readers
   * @return the created builder
   */
  static Builder create(HttpStatus statusCode, List<HttpMessageReader<?>> messageReaders) {
    return create(statusCode, new ExchangeStrategies() {
      @Override
      public List<HttpMessageReader<?>> messageReaders() {
        return messageReaders;
      }

      @Override
      public List<HttpMessageWriter<?>> messageWriters() {
        // not used in the response
        return Collections.emptyList();
      }
    });
  }

  /**
   * Represents the headers of the HTTP response.
   *
   * @see ClientResponse#headers()
   */
  interface Headers {

    /**
     * Return the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     */
    OptionalLong contentLength();

    /**
     * Return the {@linkplain MediaType media type} of the body, as specified
     * by the {@code Content-Type} header.
     */
    Optional<MediaType> contentType();

    /**
     * Return the header value(s), if any, for the header of the given name.
     * <p>Return an empty list if no header values are found.
     *
     * @param headerName the header name
     */
    List<String> header(String headerName);

    /**
     * Return the headers as an {@link HttpHeaders} instance.
     */
    HttpHeaders asHttpHeaders();
  }

  /**
   * Defines a builder for a response.
   */
  interface Builder {

    /**
     * Set the status code of the response.
     *
     * @param statusCode the new status code
     * @return this builder
     */
    Builder statusCode(HttpStatusCode statusCode);

    /**
     * Set the raw status code of the response.
     *
     * @param statusCode the new status code
     * @return this builder
     */
    Builder rawStatusCode(int statusCode);

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#setOrRemove(String, String[])
     */
    Builder header(String headerName, String... headerValues);

    /**
     * Manipulate this response's headers with the given consumer.
     * <p>The headers provided to the consumer are "live", so that the consumer
     * can be used to {@linkplain HttpHeaders#setOrRemove(String, String) overwrite}
     * existing header values, {@linkplain HttpHeaders#remove(Object) remove}
     * values, or use any of the other {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    Builder headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add a cookie with the given name and value(s).
     *
     * @param name the cookie name
     * @param values the cookie value(s)
     * @return this builder
     */
    Builder cookie(String name, String... values);

    /**
     * Manipulate this response's cookies with the given consumer.
     * <p>The map provided to the consumer is "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#setOrRemove(Object, Object) overwrite} existing cookie values,
     * {@linkplain MultiValueMap#remove(Object) remove} values, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies map
     * @return this builder
     */
    Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

    /**
     * Transform the response body, if set in the builder.
     *
     * @param transformer the transformation function to use
     * @return this builder
     */
    Builder body(Function<Flux<DataBuffer>, Flux<DataBuffer>> transformer);

    /**
     * Set the body of the response.
     * <p><strong>Note:</strong> This method will drain the existing body,
     * if set in the builder.
     *
     * @param body the new body to use
     * @return this builder
     */
    Builder body(Flux<DataBuffer> body);

    /**
     * Set the body of the response to the UTF-8 encoded bytes of the given string.
     * <p><strong>Note:</strong> This method will drain the existing body,
     * if set in the builder.
     *
     * @param body the new body.
     * @return this builder
     */
    Builder body(String body);

    /**
     * Set the request associated with the response.
     *
     * @param request the request
     * @return this builder
     */
    Builder request(HttpRequest request);

    /**
     * Build the response.
     */
    ClientResponse build();
  }

}
