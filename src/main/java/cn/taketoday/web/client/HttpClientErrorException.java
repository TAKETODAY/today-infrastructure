/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.client;

import java.io.Serial;
import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when an HTTP 4xx is received.
 *
 * @author Arjen Poutsma
 * @see DefaultResponseErrorHandler
 * @since 4.0
 */
public class HttpClientErrorException extends HttpStatusCodeException {
  @Serial
  private static final long serialVersionUID = 5177019431887513952L;

  /**
   * Constructor with a status code only.
   */
  public HttpClientErrorException(HttpStatus statusCode) {
    super(statusCode);
  }

  /**
   * Constructor with a status code and status text.
   */
  public HttpClientErrorException(HttpStatus statusCode, String statusText) {
    super(statusCode, statusText);
  }

  /**
   * Constructor with a status code and status text, and content.
   */
  public HttpClientErrorException(
          HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset responseCharset) {

    super(statusCode, statusText, body, responseCharset);
  }

  /**
   * Constructor with a status code and status text, headers, and content.
   */
  public HttpClientErrorException(
          HttpStatus statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

    super(statusCode, statusText, headers, body, responseCharset);
  }

  /**
   * Constructor with a status code and status text, headers, and content,
   * and an prepared message.
   */
  public HttpClientErrorException(
          String message, HttpStatus statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

    super(message, statusCode, statusText, headers, body, responseCharset);
  }

  /**
   * Create {@code HttpClientErrorException} or an HTTP status specific sub-class.
   *
   * @since 4.0
   */
  public static HttpClientErrorException create(
          HttpStatus statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    return create(null, statusCode, statusText, headers, body, charset);
  }

  /**
   * Variant of {@link #create(HttpStatus, String, HttpHeaders, byte[], Charset)}
   * with an optional prepared message.
   */
  public static HttpClientErrorException create(
          @Nullable String message, HttpStatus statusCode,
          String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    switch (statusCode) {
      case BAD_REQUEST:
        return message != null ?
               new BadRequest(message, statusText, headers, body, charset) :
               new BadRequest(statusText, headers, body, charset);
      case UNAUTHORIZED:
        return message != null ?
               new Unauthorized(message, statusText, headers, body, charset) :
               new Unauthorized(statusText, headers, body, charset);
      case FORBIDDEN:
        return message != null ?
               new Forbidden(message, statusText, headers, body, charset) :
               new Forbidden(statusText, headers, body, charset);
      case NOT_FOUND:
        return message != null ?
               new NotFound(message, statusText, headers, body, charset) :
               new NotFound(statusText, headers, body, charset);
      case METHOD_NOT_ALLOWED:
        return message != null ?
               new MethodNotAllowed(message, statusText, headers, body, charset) :
               new MethodNotAllowed(statusText, headers, body, charset);
      case NOT_ACCEPTABLE:
        return message != null ?
               new NotAcceptable(message, statusText, headers, body, charset) :
               new NotAcceptable(statusText, headers, body, charset);
      case CONFLICT:
        return message != null ?
               new Conflict(message, statusText, headers, body, charset) :
               new Conflict(statusText, headers, body, charset);
      case GONE:
        return message != null ?
               new Gone(message, statusText, headers, body, charset) :
               new Gone(statusText, headers, body, charset);
      case UNSUPPORTED_MEDIA_TYPE:
        return message != null ?
               new UnsupportedMediaType(message, statusText, headers, body, charset) :
               new UnsupportedMediaType(statusText, headers, body, charset);
      case TOO_MANY_REQUESTS:
        return message != null ?
               new TooManyRequests(message, statusText, headers, body, charset) :
               new TooManyRequests(statusText, headers, body, charset);
      case UNPROCESSABLE_ENTITY:
        return message != null ?
               new UnprocessableEntity(message, statusText, headers, body, charset) :
               new UnprocessableEntity(statusText, headers, body, charset);
      default:
        return message != null ?
               new HttpClientErrorException(message, statusCode, statusText, headers, body, charset) :
               new HttpClientErrorException(statusCode, statusText, headers, body, charset);
    }
  }

  // Subclasses for specific HTTP status codes

  /**
   * {@link HttpClientErrorException} for status HTTP 400 Bad Request.
   */
  @SuppressWarnings("serial")
  public static final class BadRequest extends HttpClientErrorException {

    private BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
    }

    private BadRequest(
            String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 401 Unauthorized.
   */
  @SuppressWarnings("serial")
  public static final class Unauthorized extends HttpClientErrorException {

    private Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
    }

    private Unauthorized(
            String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 403 Forbidden.
   */
  @SuppressWarnings("serial")
  public static final class Forbidden extends HttpClientErrorException {

    private Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.FORBIDDEN, statusText, headers, body, charset);
    }

    private Forbidden(
            String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.FORBIDDEN, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 404 Not Found.
   */
  @SuppressWarnings("serial")
  public static final class NotFound extends HttpClientErrorException {

    private NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.NOT_FOUND, statusText, headers, body, charset);
    }

    private NotFound(String message, String statusText,
                     HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.NOT_FOUND, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 405 Method Not Allowed.
   */
  @SuppressWarnings("serial")
  public static final class MethodNotAllowed extends HttpClientErrorException {

    private MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
    }

    private MethodNotAllowed(String message, String statusText,
                             HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 406 Not Acceptable.
   */
  @SuppressWarnings("serial")
  public static final class NotAcceptable extends HttpClientErrorException {

    private NotAcceptable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
    }

    private NotAcceptable(String message, String statusText,
                          HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 409 Conflict.
   */
  @SuppressWarnings("serial")
  public static final class Conflict extends HttpClientErrorException {

    private Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.CONFLICT, statusText, headers, body, charset);
    }

    private Conflict(String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(message, HttpStatus.CONFLICT, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 410 Gone.
   */
  @SuppressWarnings("serial")
  public static final class Gone extends HttpClientErrorException {

    private Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.GONE, statusText, headers, body, charset);
    }

    private Gone(String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(message, HttpStatus.GONE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 415 Unsupported Media Type.
   */
  @SuppressWarnings("serial")
  public static final class UnsupportedMediaType extends HttpClientErrorException {

    private UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
    }

    private UnsupportedMediaType(String message, String statusText,
                                 HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 422 Unprocessable Entity.
   */
  @SuppressWarnings("serial")
  public static final class UnprocessableEntity extends HttpClientErrorException {

    private UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
    }

    private UnprocessableEntity(String message, String statusText,
                                HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 429 Too Many Requests.
   */
  @SuppressWarnings("serial")
  public static final class TooManyRequests extends HttpClientErrorException {

    private TooManyRequests(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
    }

    private TooManyRequests(String message, String statusText,
                            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
    }
  }

}
