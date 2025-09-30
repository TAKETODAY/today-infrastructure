/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.core.codec.DecodingException;
import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.lang.Constant;

/**
 * Exceptions that contain actual HTTP response data.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebClientResponseException extends WebClientException {

  private final HttpStatusCode statusCode;

  private final String statusText;

  private final byte[] responseBody;

  private final HttpHeaders headers;

  @Nullable
  private final Charset responseCharset;

  @Nullable
  private final HttpRequest request;

  @SuppressWarnings("MutableException")
  @Nullable
  private Function<ResolvableType, ?> bodyDecodeFunction;

  /**
   * Constructor with response data only, and a default message.
   */
  public WebClientResponseException(HttpStatusCode statusCode, String reasonPhrase,
          @Nullable HttpHeaders headers, byte @Nullable [] body, @Nullable Charset charset, @Nullable HttpRequest request) {

    this(initMessage(statusCode, reasonPhrase, request),
            statusCode, reasonPhrase, headers, body, charset, request);
  }

  private static String initMessage(HttpStatusCode status, String reasonPhrase, @Nullable HttpRequest request) {
    return status.value() + " " + reasonPhrase +
            (request != null ? " from " + request.getMethod() + " " + request.getURI() : "");
  }

  /**
   * Constructor with a prepared message.
   */
  public WebClientResponseException(String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, byte @Nullable [] responseBody, @Nullable Charset charset, @Nullable HttpRequest request) {
    super(message);

    this.statusCode = statusCode;
    this.statusText = statusText;
    this.headers = HttpHeaders.copyOf(headers);
    this.responseBody = (responseBody != null ? responseBody : Constant.EMPTY_BYTES);
    this.responseCharset = charset;
    this.request = request;
  }

  /**
   * Return the HTTP status code value.
   *
   * @throws IllegalArgumentException in case of an unknown HTTP status code
   */
  public HttpStatusCode getStatusCode() {
    return this.statusCode;
  }

  /**
   * Return the raw HTTP status code value.
   */
  public int getRawStatusCode() {
    return this.statusCode.value();
  }

  /**
   * Return the HTTP status text.
   */
  public String getStatusText() {
    return this.statusText;
  }

  /**
   * Return the HTTP response headers.
   */
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  /**
   * Return the response body as a byte array.
   */
  public byte[] getResponseBodyAsByteArray() {
    return this.responseBody;
  }

  /**
   * Return the response content as a String using the charset of media type
   * for the response, if available, or otherwise falling back on
   * {@literal UTF-8}. Use {@link #getResponseBodyAsString(Charset)} if
   * you want to fall back on a different, default charset.
   *
   * @see StandardCharsets#UTF_8
   */
  public String getResponseBodyAsString() {
    return getResponseBodyAsString(StandardCharsets.UTF_8);
  }

  /**
   * Variant of {@link #getResponseBodyAsString()} that allows specifying the
   * charset to fall back on, if a charset is not available from the media
   * type for the response.
   *
   * @param defaultCharset the charset to use if the {@literal Content-Type}
   * of the response does not specify one.
   */
  public String getResponseBodyAsString(Charset defaultCharset) {
    return new String(this.responseBody,
            (this.responseCharset != null ? this.responseCharset : defaultCharset));
  }

  /**
   * Decode the error content to the specified type.
   *
   * @param targetType the type to decode to
   * @param <E> the expected target type
   * @return the decoded content, or {@code null} if there is no content
   * @throws IllegalStateException if a Decoder cannot be found
   * @throws DecodingException if decoding fails
   */
  @Nullable
  public <E> E getResponseBodyAs(Class<E> targetType) {
    return decodeBody(ResolvableType.forClass(targetType));
  }

  /**
   * Variant of {@link #getResponseBodyAs(Class)} with
   * {@link ParameterizedTypeReference}.
   */
  @Nullable
  public <E> E getResponseBodyAs(ParameterizedTypeReference<E> targetType) {
    return decodeBody(ResolvableType.forType(targetType.getType()));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <E> E decodeBody(ResolvableType targetType) {
    Assert.state(bodyDecodeFunction != null, "Decoder function not set");
    return (E) bodyDecodeFunction.apply(targetType);
  }

  /**
   * Return the corresponding request.
   */
  @Nullable
  public HttpRequest getRequest() {
    return this.request;
  }

  /**
   * Provide a function to find a decoder the given target type.
   * For use with {@link #getResponseBodyAs(Class)}.
   *
   * @param decoderFunction the function to find a decoder with
   */
  public void setBodyDecodeFunction(Function<ResolvableType, ?> decoderFunction) {
    this.bodyDecodeFunction = decoderFunction;
  }

  @Override
  public String getMessage() {
    String message = String.valueOf(super.getMessage());
    if (shouldHintAtResponseFailure()) {
      return message + ", but response failed with cause: " + getCause();
    }
    return message;
  }

  private boolean shouldHintAtResponseFailure() {
    return this.statusCode.is1xxInformational()
            || this.statusCode.is2xxSuccessful()
            || this.statusCode.is3xxRedirection();
  }

  /**
   * Create {@code WebClientResponseException} or an HTTP status specific subclass.
   */
  public static WebClientResponseException create(HttpStatusCode statusCode, String statusText,
          HttpHeaders headers, byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {

    if (statusCode instanceof HttpStatus httpStatus) {
      return switch (httpStatus) {
        case BAD_REQUEST -> new BadRequest(statusText, headers, body, charset, request);
        case UNAUTHORIZED -> new Unauthorized(statusText, headers, body, charset, request);
        case FORBIDDEN -> new Forbidden(statusText, headers, body, charset, request);
        case NOT_FOUND -> new NotFound(statusText, headers, body, charset, request);
        case METHOD_NOT_ALLOWED -> new MethodNotAllowed(statusText, headers, body, charset, request);
        case NOT_ACCEPTABLE -> new NotAcceptable(statusText, headers, body, charset, request);
        case CONFLICT -> new Conflict(statusText, headers, body, charset, request);
        case GONE -> new Gone(statusText, headers, body, charset, request);
        case UNSUPPORTED_MEDIA_TYPE -> new UnsupportedMediaType(statusText, headers, body, charset, request);
        case TOO_MANY_REQUESTS -> new TooManyRequests(statusText, headers, body, charset, request);
        case UNPROCESSABLE_ENTITY -> new UnprocessableEntity(statusText, headers, body, charset, request);
        case INTERNAL_SERVER_ERROR -> new InternalServerError(statusText, headers, body, charset, request);
        case NOT_IMPLEMENTED -> new NotImplemented(statusText, headers, body, charset, request);
        case BAD_GATEWAY -> new BadGateway(statusText, headers, body, charset, request);
        case SERVICE_UNAVAILABLE -> new ServiceUnavailable(statusText, headers, body, charset, request);
        case GATEWAY_TIMEOUT -> new GatewayTimeout(statusText, headers, body, charset, request);
        default -> new WebClientResponseException(statusCode, statusText, headers, body, charset, request);
      };
    }
    return new WebClientResponseException(statusCode, statusText, headers, body, charset, request);
  }

  // Subclasses for specific, client-side, HTTP status codes

  /**
   * {@link WebClientResponseException} for status HTTP 400 Bad Request.
   */
  public static class BadRequest extends WebClientResponseException {

    BadRequest(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.BAD_REQUEST, statusText, headers, body, charset, request);
    }

  }

  /**
   * {@link WebClientResponseException} for status HTTP 401 Unauthorized.
   */
  public static class Unauthorized extends WebClientResponseException {

    Unauthorized(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNAUTHORIZED, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 403 Forbidden.
   */
  public static class Forbidden extends WebClientResponseException {

    Forbidden(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.FORBIDDEN, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 404 Not Found.
   */
  public static class NotFound extends WebClientResponseException {

    NotFound(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.NOT_FOUND, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 405 Method Not Allowed.
   */
  public static class MethodNotAllowed extends WebClientResponseException {

    MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 406 Not Acceptable.
   */
  public static class NotAcceptable extends WebClientResponseException {

    NotAcceptable(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 409 Conflict.
   */
  public static class Conflict extends WebClientResponseException {

    Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.CONFLICT, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 410 Gone.
   */
  public static class Gone extends WebClientResponseException {

    Gone(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.GONE, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 415 Unsupported Media Type.
   */
  public static class UnsupportedMediaType extends WebClientResponseException {

    UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 422 Unprocessable Entity.
   */
  public static class UnprocessableEntity extends WebClientResponseException {

    UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 429 Too Many Requests.
   */
  public static class TooManyRequests extends WebClientResponseException {

    TooManyRequests(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset, request);
    }
  }

  // Subclasses for specific, server-side, HTTP status codes

  /**
   * {@link WebClientResponseException} for status HTTP 500 Internal Server Error.
   */
  public static class InternalServerError extends WebClientResponseException {

    InternalServerError(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 501 Not Implemented.
   */
  public static class NotImplemented extends WebClientResponseException {

    NotImplemented(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP HTTP 502 Bad Gateway.
   */
  public static class BadGateway extends WebClientResponseException {

    BadGateway(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.BAD_GATEWAY, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 503 Service Unavailable.
   */
  public static class ServiceUnavailable extends WebClientResponseException {

    ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 504 Gateway Timeout.
   */
  public static class GatewayTimeout extends WebClientResponseException {

    GatewayTimeout(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {

      super(HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset, request);
    }
  }

}
