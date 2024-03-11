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

package cn.taketoday.web.handler.function;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Represents a typed server-side HTTP response, as returned
 * by a {@linkplain HandlerFunction handler function} or
 * {@linkplain HandlerFilterFunction filter function}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ServerResponse {

  /**
   * This value indicates that the handler did not return a value, or the result
   * has been processed
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Return the status code of this response.
   *
   * @return the status as an HttpStatusCode value
   */
  HttpStatusCode statusCode();

  /**
   * Return the status code of this response as integer.
   *
   * @return the status as an integer
   */
  int rawStatusCode();

  /**
   * Return the headers of this response.
   */
  HttpHeaders headers();

  /**
   * Return the cookies of this response.
   */
  MultiValueMap<String, HttpCookie> cookies();

  /**
   * Write this response to the given servlet response.
   *
   * @param request the current request
   * @param context the context to use when writing
   * @return a Web result to render, or {@code null} if handled directly
   */
  @Nullable
  Object writeTo(RequestContext request, Context context) throws Throwable;

  // Static methods

  /**
   * Create a builder with the status code and headers of the given response.
   *
   * @param other the response to copy the status and headers from
   * @return the created builder
   */
  static BodyBuilder from(ServerResponse other) {
    return new DefaultServerResponseBuilder(other);
  }

  /**
   * Create a {@code ServerResponse} from the given {@link ErrorResponse}.
   *
   * @param response the {@link ErrorResponse} to initialize from
   * @return the built response
   */
  static ServerResponse from(ErrorResponse response) {
    return status(response.getStatusCode())
            .headers(headers -> headers.putAll(response.getHeaders()))
            .body(response.getBody());
  }

  /**
   * Create a builder with the given HTTP status.
   *
   * @param status the response status
   * @return the created builder
   */
  static BodyBuilder status(HttpStatusCode status) {
    return new DefaultServerResponseBuilder(status);
  }

  /**
   * Create a builder with the given HTTP status.
   *
   * @param status the response status
   * @return the created builder
   */
  static BodyBuilder status(int status) {
    return new DefaultServerResponseBuilder(HttpStatusCode.valueOf(status));
  }

  /**
   * Create a builder with the status set to {@linkplain HttpStatus#OK 200 OK}.
   *
   * @return the created builder
   */
  static BodyBuilder ok() {
    return status(HttpStatus.OK);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#CREATED 201 Created} status
   * and a location header set to the given URI.
   *
   * @param location the location URI
   * @return the created builder
   */
  static BodyBuilder created(URI location) {
    BodyBuilder builder = status(HttpStatus.CREATED);
    return builder.location(location);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#ACCEPTED 202 Accepted} status.
   *
   * @return the created builder
   */
  static BodyBuilder accepted() {
    return status(HttpStatus.ACCEPTED);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#NO_CONTENT 204 No Content} status.
   *
   * @return the created builder
   */
  static HeadersBuilder<?> noContent() {
    return status(HttpStatus.NO_CONTENT);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#SEE_OTHER 303 See Other}
   * status and a location header set to the given URI.
   *
   * @param location the location URI
   * @return the created builder
   */
  static BodyBuilder seeOther(URI location) {
    BodyBuilder builder = status(HttpStatus.SEE_OTHER);
    return builder.location(location);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect}
   * status and a location header set to the given URI.
   *
   * @param location the location URI
   * @return the created builder
   */
  static BodyBuilder temporaryRedirect(URI location) {
    BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
    return builder.location(location);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect}
   * status and a location header set to the given URI.
   *
   * @param location the location URI
   * @return the created builder
   */
  static BodyBuilder permanentRedirect(URI location) {
    BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
    return builder.location(location);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST 400 Bad Request} status.
   *
   * @return the created builder
   */
  static BodyBuilder badRequest() {
    return status(HttpStatus.BAD_REQUEST);
  }

  /**
   * Create a builder with a {@linkplain HttpStatus#NOT_FOUND 404 Not Found} status.
   *
   * @return the created builder
   */
  static HeadersBuilder<?> notFound() {
    return status(HttpStatus.NOT_FOUND);
  }

  /**
   * Create a builder with a
   * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity} status.
   *
   * @return the created builder
   */
  static BodyBuilder unprocessableEntity() {
    return status(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Create a (built) response with the given asynchronous response.
   * Parameter {@code asyncResponse} can be a
   * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
   * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
   * asynchronous producer of a single {@code ServerResponse} that can be
   * adapted via the {@link ReactiveAdapterRegistry}).
   *
   * <p>This method can be used to set the response status code, headers, and
   * body based on an asynchronous result. If only the body is asynchronous,
   * {@link BodyBuilder#body(Object)} can be used instead.
   *
   * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
   * {@code Publisher<ServerResponse>}
   * @return the asynchronous response
   */
  static ServerResponse async(Object asyncResponse) {
    return AsyncServerResponse.create(asyncResponse, null);
  }

  /**
   * Create a (built) response with the given asynchronous response.
   * Parameter {@code asyncResponse} can be a
   * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
   * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
   * asynchronous producer of a single {@code ServerResponse} that can be
   * adapted via the {@link ReactiveAdapterRegistry}).
   *
   * <p>This method can be used to set the response status code, headers, and
   * body based on an asynchronous result. If only the body is asynchronous,
   * {@link BodyBuilder#body(Object)} can be used instead.
   *
   * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
   * {@code Publisher<ServerResponse>}
   * @param timeout maximum time period to wait for before timing out
   * @return the asynchronous response
   */
  static ServerResponse async(Object asyncResponse, @Nullable Duration timeout) {
    return AsyncServerResponse.create(asyncResponse, timeout);
  }

  /**
   * Create a server-sent event response. The {@link SseBuilder} provided to
   * {@code consumer} can be used to build and send events.
   *
   * <p>For example:
   * <pre>{@code
   * public ServerResponse handleSse(ServerRequest request) {
   *     return ServerResponse.sse(sse -> sse.send("Hello World!"));
   * }
   * }</pre>
   *
   * <p>or, to set both the id and event type:
   * <pre>{@code
   * public ServerResponse handleSse(ServerRequest request) {
   *     return ServerResponse.sse(sse -> sse
   *         .id("42)
   *         .event("event")
   *         .send("Hello World!"));
   * }
   * }</pre>
   *
   * @param consumer consumer that will be provided with an event builder
   * @return the server-side event response
   * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>
   */
  static ServerResponse sse(Consumer<SseBuilder> consumer) {
    return new SseServerResponse(consumer, null);
  }

  /**
   * Create a server-sent event response. The {@link SseBuilder} provided to
   * {@code consumer} can be used to build and send events.
   *
   * <p>For example:
   * <pre>{@code
   * public ServerResponse handleSse(ServerRequest request) {
   *     return ServerResponse.sse(sse -> sse.send("Hello World!"));
   * }
   * }</pre>
   *
   * <p>or, to set both the id and event type:
   * <pre>{@code
   * public ServerResponse handleSse(ServerRequest request) {
   *     return ServerResponse.sse(sse -> sse
   *         .id("42)
   *         .event("event")
   *         .send("Hello World!"));
   * }
   * }</pre>
   *
   * @param consumer consumer that will be provided with an event builder
   * @param timeout maximum time period to wait before timing out
   * @return the server-side event response
   * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>
   */
  static ServerResponse sse(Consumer<SseBuilder> consumer, Duration timeout) {
    return new SseServerResponse(consumer, timeout);
  }

  /**
   * Defines a builder that adds headers to the response.
   *
   * @param <B> the builder subclass
   */
  interface HeadersBuilder<B extends HeadersBuilder<B>> {

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return this builder
     * @see HttpHeaders#add(String, String)
     */
    B header(String headerName, String... headerValues);

    /**
     * Manipulate this response's headers with the given consumer. The
     * headers provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
     * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
     * {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return this builder
     */
    B headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Add the given cookie to the response.
     *
     * @param cookie the cookie to add
     * @return this builder
     */
    B cookie(HttpCookie cookie);

    /**
     * Manipulate this response's cookies with the given consumer. The
     * cookies provided to the consumer are "live", so that the consumer can be used to
     * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
     * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
     * {@link MultiValueMap} methods.
     *
     * @param cookiesConsumer a function that consumes the cookies
     * @return this builder
     */
    B cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer);

    /**
     * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
     * by the {@code Allow} header.
     *
     * @param allowedMethods the allowed methods
     * @return this builder
     * @see HttpHeaders#setAllow(Collection)
     */
    B allow(HttpMethod... allowedMethods);

    /**
     * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
     * by the {@code Allow} header.
     *
     * @param allowedMethods the allowed methods
     * @return this builder
     * @see HttpHeaders#setAllow(Collection)
     */
    B allow(Set<HttpMethod> allowedMethods);

    /**
     * Set the entity tag of the body, as specified by the {@code ETag} header.
     *
     * @param eTag the new entity tag
     * @return this builder
     * @see HttpHeaders#setETag(String)
     */
    B eTag(String eTag);

    /**
     * Set the time the resource was last changed, as specified by the
     * {@code Last-Modified} header.
     *
     * @param lastModified the last modified date
     * @return this builder
     * @see HttpHeaders#setLastModified(long)
     */
    B lastModified(ZonedDateTime lastModified);

    /**
     * Set the time the resource was last changed, as specified by the
     * {@code Last-Modified} header.
     *
     * @param lastModified the last modified date
     * @return this builder
     * @see HttpHeaders#setLastModified(long)
     */
    B lastModified(Instant lastModified);

    /**
     * Set the location of a resource, as specified by the {@code Location} header.
     *
     * @param location the location
     * @return this builder
     * @see HttpHeaders#setLocation(URI)
     */
    B location(URI location);

    /**
     * Set the caching directives for the resource, as specified by the HTTP 1.1
     * {@code Cache-Control} header.
     * <p>A {@code CacheControl} instance can be built like
     * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}.
     *
     * @param cacheControl a builder for cache-related HTTP response headers
     * @return this builder
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
     */
    B cacheControl(CacheControl cacheControl);

    /**
     * Configure one or more request header names (e.g. "Accept-Language") to
     * add to the "Vary" response header to inform clients that the response is
     * subject to content negotiation and variances based on the value of the
     * given request headers. The configured request header names are added only
     * if not already present in the response "Vary" header.
     *
     * @param requestHeaders request header names
     * @return this builder
     */
    B varyBy(String... requestHeaders);

    /**
     * Build the response entity with no body.
     */
    ServerResponse build();

    /**
     * Build the response entity with a custom write function.
     *
     * @param writeFunction the function used to write to the {@link HttpServletResponse}
     */
    ServerResponse build(WriteFunction writeFunction);

    /**
     * Defines the contract for {@link #build(WriteFunction)}.
     */
    @FunctionalInterface
    interface WriteFunction {

      /**
       * Write to the given {@code servletResponse}, or return a
       * {@code ModelAndView} to be rendered.
       *
       * @param request the HTTP request, HTTP response to write to
       * @return a {@code ModelAndView} to render, or {@code null} if handled directly
       * @throws Exception in case of Servlet errors
       */
      @Nullable
      Object write(RequestContext request) throws Exception;

    }

  }

  /**
   * Defines a builder that adds a body to the response.
   */
  interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

    /**
     * Set the length of the body in bytes, as specified by the
     * {@code Content-Length} header.
     *
     * @param contentLength the content length
     * @return this builder
     * @see HttpHeaders#setContentLength(long)
     */
    BodyBuilder contentLength(long contentLength);

    /**
     * Set the {@linkplain MediaType media type} of the body, as specified by the
     * {@code Content-Type} header.
     *
     * @param contentType the content type
     * @return this builder
     * @see HttpHeaders#setContentType(MediaType)
     */
    BodyBuilder contentType(MediaType contentType);

    /**
     * Set the body of the response to the given {@code Object} and return
     * it.
     *
     * <p>Asynchronous response bodies are supported by providing a
     * {@link CompletionStage} or {@link Publisher} as body (or any
     * asynchronous producer of a single entity that can be adapted via the
     * {@link ReactiveAdapterRegistry}).
     *
     * @param body the body of the response
     * @return the built response
     */
    ServerResponse body(Object body);

    /**
     * Set the body of the response to the given {@code Object} and return it. The parameter
     * {@code bodyType} is used to capture the generic type.
     *
     * @param body the body of the response
     * @param bodyType the type of the body, used to capture the generic type
     * @return the built response
     */
    <T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType);

    /**
     * Render the template with the given {@code name} using the given {@code modelAttributes}.
     * The model attributes are mapped under a
     * {@linkplain cn.taketoday.core.Conventions#getVariableName generated name}.
     * <p><em>Note: Empty {@link Collection Collections} are not added to
     * the model when using this method because we cannot correctly determine
     * the true convention name.</em>
     *
     * @param name the name of the template to be rendered
     * @param modelAttributes the modelAttributes used to render the template
     * @return the built response
     */
    ServerResponse render(String name, Object... modelAttributes);

    /**
     * Render the template with the given {@code name} using the given {@code model}.
     *
     * @param name the name of the template to be rendered
     * @param model the model used to render the template
     * @return the built response
     */
    ServerResponse render(String name, Map<String, ?> model);

    /**
     * Render the template with the given {@code ModelAndView}.
     *
     * @param modelAndView the model and view used to render the template
     * @return the built response
     */
    ServerResponse render(ModelAndView modelAndView);

  }

  /**
   * Defines a builder for a body that sends server-sent events.
   */
  interface SseBuilder {

    /**
     * Sends the given object as a server-sent event.
     * Strings will be sent as UTF-8 encoded bytes, and other objects will
     * be converted into JSON using
     * {@linkplain HttpMessageConverter message converters}.
     *
     * <p>This convenience method has the same effect as
     * {@link #data(Object)}.
     *
     * @param object the object to send
     * @throws IOException in case of I/O errors
     */
    void send(Object object) throws IOException;

    /**
     * Sends the buffered content as a server-sent event, without data.
     * Only the {@link #event(String) events} and {@link #comment(String) comments}
     * will be sent.
     *
     * @throws IOException in case of I/O errors
     */
    void send() throws IOException;

    /**
     * Add an SSE "id" line.
     *
     * @param id the event identifier
     * @return this builder
     */
    SseBuilder id(String id);

    /**
     * Add an SSE "event" line.
     *
     * @param eventName the event name
     * @return this builder
     */
    SseBuilder event(String eventName);

    /**
     * Add an SSE "retry" line.
     *
     * @param duration the duration to convert into millis
     * @return this builder
     */
    SseBuilder retry(Duration duration);

    /**
     * Add an SSE comment.
     *
     * @param comment the comment
     * @return this builder
     */
    SseBuilder comment(String comment);

    /**
     * Add an SSE "data" line for the given object and sends the built
     * server-sent event to the client.
     * Strings will be sent as UTF-8 encoded bytes, and other objects will
     * be converted into JSON using
     * {@linkplain HttpMessageConverter message converters}.
     *
     * @param object the object to send as data
     * @throws IOException in case of I/O errors
     */
    void data(Object object) throws IOException;

    /**
     * Add an SSE "data" line for the given object and sends the built
     * server-sent event to the client.
     * Strings will be sent as UTF-8 encoded bytes, and other objects will
     * be converted into your provided {@code mediaType} from
     * {@linkplain HttpMessageConverter message converters}.
     *
     * @param object the object to send as data
     * @throws IOException in case of I/O errors
     */
    void data(Object object, @Nullable MediaType mediaType) throws IOException;

    /**
     * Completes the event stream with the given error.
     *
     * <p>The throwable is dispatched back into Web MVC, and passed to
     * its exception handling mechanism. Since the response has
     * been committed by this point, the response status can not change.
     *
     * @param t the throwable to dispatch
     */
    void error(Throwable t);

    /**
     * Completes the event stream.
     */
    void complete();

    /**
     * Register a callback to be invoked when an SSE request times
     * out.
     *
     * @param onTimeout the callback to invoke on timeout
     * @return this builder
     */
    SseBuilder onTimeout(Runnable onTimeout);

    /**
     * Register a callback to be invoked when an error occurs during SSE
     * processing.
     *
     * @param onError the callback to invoke on error
     * @return this builder
     */
    SseBuilder onError(Consumer<Throwable> onError);

    /**
     * Register a callback to be invoked when the SSE request completes.
     *
     * @param onCompletion the callback to invoked on completion
     * @return this builder
     */
    SseBuilder onComplete(Runnable onCompletion);
  }

  /**
   * Defines the context used during the {@link #writeTo(RequestContext, Context)}.
   */
  interface Context {

    /**
     * Return the {@link HttpMessageConverter HttpMessageConverters} to be used for response body conversion.
     *
     * @return the list of message writers
     */
    List<HttpMessageConverter<?>> messageConverters();

  }

}
