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

import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.reactive.function.BodyInserter;
import reactor.core.publisher.Mono;

/**
 * Represents a typed, immutable, client-side HTTP request, as executed by the
 * {@link ExchangeFunction}. Instances of this interface can be created via static
 * builder methods.
 *
 * <p>Note that applications are more likely to perform requests through
 * {@link WebClient} rather than using this directly.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ClientRequest {

  /**
   * Name of {@link #attributes() attribute} whose value can be used to
   * correlate log messages for this request. Use {@link #logPrefix()} to
   * obtain a consistently formatted prefix based on this attribute.
   *
   * @see #logPrefix()
   */
  String LOG_ID_ATTRIBUTE = ClientRequest.class.getName() + ".LOG_ID";

  /**
   * Return the HTTP method.
   */
  HttpMethod method();

  /**
   * Return the request URI.
   */
  URI uri();

  /**
   * Return the headers of this request.
   */
  HttpHeaders headers();

  /**
   * Return the cookies of this request.
   */
  MultiValueMap<String, String> cookies();

  /**
   * Return the body inserter of this request.
   */
  BodyInserter<?, ? super ClientHttpRequest> body();

  /**
   * Return the request attribute value if present.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  default Optional<Object> attribute(String name) {
    return Optional.ofNullable(attributes().get(name));
  }

  /**
   * Return the attributes of this request.
   */
  Map<String, Object> attributes();

  /**
   * Return consumer(s) configured to access to the {@link ClientHttpRequest}.
   */
  @Nullable
  Consumer<ClientHttpRequest> httpRequest();

  /**
   * Return a log message prefix to use to correlate messages for this request.
   * The prefix is based on the value of the attribute {@link #LOG_ID_ATTRIBUTE
   * LOG_ID_ATTRIBUTE} surrounded with "[" and "]".
   *
   * @return the log message prefix or an empty String if the
   * {@link #LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} is not set.
   */
  String logPrefix();

  /**
   * Write this request to the given {@link ClientHttpRequest}.
   *
   * @param request the client http request to write to
   * @param strategies the strategies to use when writing
   * @return {@code Mono<Void>} to indicate when writing is complete
   */
  Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies);

  // Static builder methods

  /**
   * Create a builder initialized with the HTTP method, url, headers, cookies,
   * attributes, and body of the given request.
   *
   * @param other the request to copy from
   * @return the builder instance
   */
  static Builder from(ClientRequest other) {
    return new DefaultClientRequestBuilder(other);
  }

  /**
   * Create a request builder with the given HTTP method and url.
   *
   * @param method the HTTP method (GET, POST, etc)
   * @param url the url (as a URI instance)
   * @return the created builder
   */
  static Builder create(HttpMethod method, URI url) {
    return new DefaultClientRequestBuilder(method, url);
  }

  /**
   * Defines a builder for a request.
   */
  interface Builder {

    /**
     * Set the method of the request.
     *
     * @param method the new method
     * @return this builder
     */
    Builder method(HttpMethod method);

    /**
     * Set the url of the request.
     *
     * @param uri the new url
     * @return this builder
     */
    Builder uri(URI uri);

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    Builder header(String headerName, String... headerValues);

    /**
     * Manipulate this request's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    Builder headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add the given HttpHeaders.
     *
     * @param headers the headers
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder headers(@Nullable HttpHeaders headers);

    /**
     * Add a cookie with the given name and value(s).
     *
     * @param name the cookie name
     * @param values the cookie value(s)
     * @return this builder
     */
    Builder cookie(String name, String... values);

    /**
     * Manipulate this request's cookies with the given consumer. The
     * map provided to the consumer is "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookie values,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies map
     * @return this builder
     */
    Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

    /**
     * Add a cookies with the given name and values.
     *
     * @param cookies the cookies
     * @return this builder
     * @see MultiValueMap#setAll(Map)
     * @since 5.0
     */
    Builder cookies(@Nullable MultiValueMap<String, String> cookies);

    /**
     * Set the body of the request to the given {@code BodyInserter}.
     *
     * @param inserter the {@code BodyInserter} that writes to the request
     * @return this builder
     */
    Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter);

    /**
     * Set the body of the request to the given {@code Publisher} and return it.
     *
     * @param publisher the {@code Publisher} to write to the request
     * @param elementClass the class of elements contained in the publisher
     * @param <S> the type of the elements contained in the publisher
     * @param <P> the type of the {@code Publisher}
     * @return the built request
     */
    <S, P extends Publisher<S>> Builder body(P publisher, Class<S> elementClass);

    /**
     * Set the body of the request to the given {@code Publisher} and return it.
     *
     * @param publisher the {@code Publisher} to write to the request
     * @param typeReference a type reference describing the elements contained in the publisher
     * @param <S> the type of the elements contained in the publisher
     * @param <P> the type of the {@code Publisher}
     * @return the built request
     */
    <S, P extends Publisher<S>> Builder body(P publisher, ParameterizedTypeReference<S> typeReference);

    /**
     * Set the attribute with the given name to the given value.
     *
     * @param name the name of the attribute to add
     * @param value the value of the attribute to add
     * @return this builder
     */
    Builder attribute(String name, Object value);

    /**
     * Manipulate the request attributes with the given consumer. The attributes provided to
     * the consumer are "live", so that the consumer can be used to inspect attributes,
     * remove attributes, or use any of the other map-provided methods.
     *
     * @param attributesConsumer a function that consumes the attributes
     * @return this builder
     */
    Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

    /**
     * Add the attributes with the given name to the given value.
     *
     * @param attributes the attributes of to add
     * @return this builder
     * @see Map#putAll(Map)
     * @since 5.0
     */
    Builder attributes(@Nullable Map<String, Object> attributes);

    /**
     * Callback for access to the {@link ClientHttpRequest} that in turn
     * provides access to the native request of the underlying HTTP library.
     * This could be useful for setting advanced, per-request options that
     * exposed by the underlying library.
     *
     * @param requestConsumer a consumer to access the
     * {@code ClientHttpRequest} with
     * @return this builder
     */
    Builder httpRequest(Consumer<ClientHttpRequest> requestConsumer);

    /**
     * Build the request.
     */
    ClientRequest build();
  }

}
