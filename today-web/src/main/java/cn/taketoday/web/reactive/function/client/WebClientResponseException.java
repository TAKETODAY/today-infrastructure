/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Exceptions that contain actual HTTP response data.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class WebClientResponseException extends WebClientException {

  @Serial
  private static final long serialVersionUID = 1L;

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
  public WebClientResponseException(
          int statusCode, String statusText, @Nullable HttpHeaders headers,
          @Nullable byte[] body, @Nullable Charset charset) {

    this(statusCode, statusText, headers, body, charset, null);
  }

  /**
   * Constructor with response data only, and a default message.
   */
  public WebClientResponseException(
          int status, String reasonPhrase, @Nullable HttpHeaders headers,
          @Nullable byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {
    this(HttpStatusCode.valueOf(status), reasonPhrase, headers, body, charset, request);
  }

  /**
   * Constructor with response data only, and a default message.
   */
  public WebClientResponseException(
          HttpStatusCode statusCode, String reasonPhrase, @Nullable HttpHeaders headers,
          @Nullable byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {

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
  public WebClientResponseException(
          String message, int statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset) {

    this(message, statusCode, statusText, headers, responseBody, charset, null);
  }

  /**
   * Constructor with a prepared message.
   */
  public WebClientResponseException(
          String message, int statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset,
          @Nullable HttpRequest request) {

    this(message, HttpStatusCode.valueOf(statusCode), statusText, headers, responseBody, charset, request);
  }

  /**
   * Constructor with a prepared message.
   */
  public WebClientResponseException(
          String message, HttpStatusCode statusCode, String statusText, @Nullable HttpHeaders headers,
          @Nullable byte[] responseBody, @Nullable Charset charset, @Nullable HttpRequest request) {
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
    return getResponseBodyAs(ResolvableType.forClass(targetType));
  }

  /**
   * Variant of {@link #getResponseBodyAs(Class)} with
   * {@link TypeReference}.
   */
  @Nullable
  public <E> E getResponseBodyAs(TypeReference<E> targetType) {
    return getResponseBodyAs(ResolvableType.forType(targetType.getType()));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <E> E getResponseBodyAs(ResolvableType targetType) {
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

  /**
   * Create {@code WebClientResponseException} or an HTTP status specific subclass.
   */
  public static WebClientResponseException create(
          int statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    return create(statusCode, statusText, headers, body, charset, null);
  }

  /**
   * Create {@code WebClientResponseException} or an HTTP status specific subclass.
   */
  public static WebClientResponseException create(
          int statusCode, String statusText, HttpHeaders headers,
          byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {

    return create(HttpStatusCode.valueOf(statusCode), statusText, headers, body, charset, request);
  }

  /**
   * Create {@code WebClientResponseException} or an HTTP status specific subclass.
   */
  public static WebClientResponseException create(
          HttpStatusCode statusCode, String statusText, HttpHeaders headers,
          byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {

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
  @SuppressWarnings("serial")
  public static class BadRequest extends WebClientResponseException {

    BadRequest(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.BAD_REQUEST.value(), statusText, headers, body, charset, request);
    }

  }

  /**
   * {@link WebClientResponseException} for status HTTP 401 Unauthorized.
   */
  @SuppressWarnings("serial")
  public static class Unauthorized extends WebClientResponseException {

    Unauthorized(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNAUTHORIZED.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 403 Forbidden.
   */
  @SuppressWarnings("serial")
  public static class Forbidden extends WebClientResponseException {

    Forbidden(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.FORBIDDEN.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 404 Not Found.
   */
  @SuppressWarnings("serial")
  public static class NotFound extends WebClientResponseException {

    NotFound(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.NOT_FOUND.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 405 Method Not Allowed.
   */
  @SuppressWarnings("serial")
  public static class MethodNotAllowed extends WebClientResponseException {

    MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.METHOD_NOT_ALLOWED.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 406 Not Acceptable.
   */
  @SuppressWarnings("serial")
  public static class NotAcceptable extends WebClientResponseException {

    NotAcceptable(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.NOT_ACCEPTABLE.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 409 Conflict.
   */
  @SuppressWarnings("serial")
  public static class Conflict extends WebClientResponseException {

    Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.CONFLICT.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 410 Gone.
   */
  @SuppressWarnings("serial")
  public static class Gone extends WebClientResponseException {

    Gone(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.GONE.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 415 Unsupported Media Type.
   */
  @SuppressWarnings("serial")
  public static class UnsupportedMediaType extends WebClientResponseException {

    UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 422 Unprocessable Entity.
   */
  @SuppressWarnings("serial")
  public static class UnprocessableEntity extends WebClientResponseException {

    UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNPROCESSABLE_ENTITY.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 429 Too Many Requests.
   */
  @SuppressWarnings("serial")
  public static class TooManyRequests extends WebClientResponseException {

    TooManyRequests(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.TOO_MANY_REQUESTS.value(), statusText, headers, body, charset, request);
    }
  }

  // Subclasses for specific, server-side, HTTP status codes

  /**
   * {@link WebClientResponseException} for status HTTP 500 Internal Server Error.
   */
  @SuppressWarnings("serial")
  public static class InternalServerError extends WebClientResponseException {

    InternalServerError(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.INTERNAL_SERVER_ERROR.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 501 Not Implemented.
   */
  @SuppressWarnings("serial")
  public static class NotImplemented extends WebClientResponseException {

    NotImplemented(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.NOT_IMPLEMENTED.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP HTTP 502 Bad Gateway.
   */
  @SuppressWarnings("serial")
  public static class BadGateway extends WebClientResponseException {

    BadGateway(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.BAD_GATEWAY.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 503 Service Unavailable.
   */
  @SuppressWarnings("serial")
  public static class ServiceUnavailable extends WebClientResponseException {

    ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.SERVICE_UNAVAILABLE.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 504 Gateway Timeout.
   */
  @SuppressWarnings("serial")
  public static class GatewayTimeout extends WebClientResponseException {

    GatewayTimeout(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {

      super(HttpStatus.GATEWAY_TIMEOUT.value(), statusText, headers, body, charset, request);
    }
  }

}
