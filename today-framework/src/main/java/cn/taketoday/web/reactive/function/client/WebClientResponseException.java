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

import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;

/**
 * Exceptions that contain actual HTTP response data.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class WebClientResponseException extends WebClientException {

  @Serial
  private static final long serialVersionUID = 4127543205414951611L;

  private final int statusCode;

  private final String statusText;

  private final byte[] responseBody;

  private final HttpHeaders headers;

  @Nullable
  private final Charset responseCharset;

  @Nullable
  private final HttpRequest request;

  /**
   * Constructor with response data only, and a default message.
   *
   * @since 4.0
   */
  public WebClientResponseException(int statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

    this(statusCode, statusText, headers, body, charset, null);
  }

  /**
   * Constructor with response data only, and a default message.
   *
   * @since 4.0
   */
  public WebClientResponseException(int status, String reasonPhrase,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset,
          @Nullable HttpRequest request) {

    this(initMessage(status, reasonPhrase, request), status, reasonPhrase, headers, body, charset, request);
  }

  private static String initMessage(int status, String reasonPhrase, @Nullable HttpRequest request) {
    return status + " " + reasonPhrase +
            (request != null ? " from " + request.getMethod() + " " + request.getURI() : "");
  }

  /**
   * Constructor with a prepared message.
   */
  public WebClientResponseException(String message, int statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset) {
    this(message, statusCode, statusText, headers, responseBody, charset, null);
  }

  /**
   * Constructor with a prepared message.
   *
   * @since 4.0
   */
  public WebClientResponseException(String message, int statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset,
          @Nullable HttpRequest request) {

    super(message);

    this.statusCode = statusCode;
    this.statusText = statusText;
    this.headers = (headers != null ? headers : HttpHeaders.EMPTY);
    this.responseBody = (responseBody != null ? responseBody : new byte[0]);
    this.responseCharset = charset;
    this.request = request;
  }

  /**
   * Return the HTTP status code value.
   *
   * @throws IllegalArgumentException in case of an unknown HTTP status code
   */
  public HttpStatus getStatusCode() {
    return HttpStatus.valueOf(this.statusCode);
  }

  /**
   * Return the raw HTTP status code value.
   */
  public int getRawStatusCode() {
    return this.statusCode;
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
   * {@literal ISO-8859-1}. Use {@link #getResponseBodyAsString(Charset)} if
   * you want to fall back on a different, default charset.
   */
  public String getResponseBodyAsString() {
    return getResponseBodyAsString(StandardCharsets.ISO_8859_1);
  }

  /**
   * Variant of {@link #getResponseBodyAsString()} that allows specifying the
   * charset to fall back on, if a charset is not available from the media
   * type for the response.
   *
   * @param defaultCharset the charset to use if the {@literal Content-Type}
   * of the response does not specify one.
   * @since 4.0
   */
  public String getResponseBodyAsString(Charset defaultCharset) {
    return new String(this.responseBody,
            (this.responseCharset != null ? this.responseCharset : defaultCharset));
  }

  /**
   * Return the corresponding request.
   *
   * @since 4.0
   */
  @Nullable
  public HttpRequest getRequest() {
    return this.request;
  }

  /**
   * Create {@code WebClientResponseException} or an HTTP status specific subclass.
   *
   * @since 4.0
   */
  public static WebClientResponseException create(
          int statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    return create(statusCode, statusText, headers, body, charset, null);
  }

  /**
   * Create {@code WebClientResponseException} or an HTTP status specific subclass.
   *
   * @since 4.0
   */
  public static WebClientResponseException create(
          int statusCode, String statusText, HttpHeaders headers, byte[] body,
          @Nullable Charset charset, @Nullable HttpRequest request) {

    HttpStatus httpStatus = HttpStatus.resolve(statusCode);
    if (httpStatus != null) {
      switch (httpStatus) {
        case BAD_REQUEST:
          return new BadRequest(statusText, headers, body, charset, request);
        case UNAUTHORIZED:
          return new Unauthorized(statusText, headers, body, charset, request);
        case FORBIDDEN:
          return new Forbidden(statusText, headers, body, charset, request);
        case NOT_FOUND:
          return new NotFound(statusText, headers, body, charset, request);
        case METHOD_NOT_ALLOWED:
          return new MethodNotAllowed(statusText, headers, body, charset, request);
        case NOT_ACCEPTABLE:
          return new NotAcceptable(statusText, headers, body, charset, request);
        case CONFLICT:
          return new Conflict(statusText, headers, body, charset, request);
        case GONE:
          return new Gone(statusText, headers, body, charset, request);
        case UNSUPPORTED_MEDIA_TYPE:
          return new UnsupportedMediaType(statusText, headers, body, charset, request);
        case TOO_MANY_REQUESTS:
          return new TooManyRequests(statusText, headers, body, charset, request);
        case UNPROCESSABLE_ENTITY:
          return new UnprocessableEntity(statusText, headers, body, charset, request);
        case INTERNAL_SERVER_ERROR:
          return new InternalServerError(statusText, headers, body, charset, request);
        case NOT_IMPLEMENTED:
          return new NotImplemented(statusText, headers, body, charset, request);
        case BAD_GATEWAY:
          return new BadGateway(statusText, headers, body, charset, request);
        case SERVICE_UNAVAILABLE:
          return new ServiceUnavailable(statusText, headers, body, charset, request);
        case GATEWAY_TIMEOUT:
          return new GatewayTimeout(statusText, headers, body, charset, request);
      }
    }
    return new WebClientResponseException(statusCode, statusText, headers, body, charset, request);
  }

  // Subclasses for specific, client-side, HTTP status codes

  /**
   * {@link WebClientResponseException} for status HTTP 400 Bad Request.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class BadRequest extends WebClientResponseException {

    BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.BAD_REQUEST.value(), statusText, headers, body, charset, request);
    }

  }

  /**
   * {@link WebClientResponseException} for status HTTP 401 Unauthorized.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class Unauthorized extends WebClientResponseException {

    Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.UNAUTHORIZED.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 403 Forbidden.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class Forbidden extends WebClientResponseException {

    Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.FORBIDDEN.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 404 Not Found.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class NotFound extends WebClientResponseException {

    NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.NOT_FOUND.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 405 Method Not Allowed.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class MethodNotAllowed extends WebClientResponseException {

    MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {
      super(HttpStatus.METHOD_NOT_ALLOWED.value(), statusText, headers, body, charset,
              request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 406 Not Acceptable.
   *
   * @since 4.0
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
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class Conflict extends WebClientResponseException {

    Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.CONFLICT.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 410 Gone.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class Gone extends WebClientResponseException {

    Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.GONE.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 415 Unsupported Media Type.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class UnsupportedMediaType extends WebClientResponseException {

    UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {

      super(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), statusText, headers, body, charset,
              request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 422 Unprocessable Entity.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class UnprocessableEntity extends WebClientResponseException {

    UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {
      super(HttpStatus.UNPROCESSABLE_ENTITY.value(), statusText, headers, body, charset,
              request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 429 Too Many Requests.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class TooManyRequests extends WebClientResponseException {

    TooManyRequests(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {
      super(HttpStatus.TOO_MANY_REQUESTS.value(), statusText, headers, body, charset,
              request);
    }
  }

  // Subclasses for specific, server-side, HTTP status codes

  /**
   * {@link WebClientResponseException} for status HTTP 500 Internal Server Error.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class InternalServerError extends WebClientResponseException {

    InternalServerError(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {
      super(HttpStatus.INTERNAL_SERVER_ERROR.value(), statusText, headers, body, charset,
              request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 501 Not Implemented.
   *
   * @since 4.0
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
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class BadGateway extends WebClientResponseException {

    BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
            @Nullable HttpRequest request) {
      super(HttpStatus.BAD_GATEWAY.value(), statusText, headers, body, charset, request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 503 Service Unavailable.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class ServiceUnavailable extends WebClientResponseException {

    ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {
      super(HttpStatus.SERVICE_UNAVAILABLE.value(), statusText, headers, body, charset,
              request);
    }
  }

  /**
   * {@link WebClientResponseException} for status HTTP 504 Gateway Timeout.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static class GatewayTimeout extends WebClientResponseException {

    GatewayTimeout(String statusText, HttpHeaders headers, byte[] body,
            @Nullable Charset charset, @Nullable HttpRequest request) {
      super(HttpStatus.GATEWAY_TIMEOUT.value(), statusText, headers, body, charset,
              request);
    }
  }

}
