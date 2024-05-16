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

package cn.taketoday.web.client;

import java.io.Serial;
import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when an HTTP 4xx is received.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultResponseErrorHandler
 * @since 4.0
 */
public class HttpClientErrorException extends HttpStatusCodeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with a status code only.
   */
  public HttpClientErrorException(HttpStatusCode statusCode) {
    super(statusCode);
  }

  /**
   * Constructor with a status code and status text.
   */
  public HttpClientErrorException(HttpStatusCode statusCode, String statusText) {
    super(statusCode, statusText);
  }

  /**
   * Constructor with a status code and status text, and content.
   */
  public HttpClientErrorException(HttpStatusCode statusCode, String statusText,
          @Nullable byte[] body, @Nullable Charset responseCharset) {

    super(statusCode, statusText, body, responseCharset);
  }

  /**
   * Constructor with a status code and status text, headers, and content.
   */
  public HttpClientErrorException(HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

    super(statusCode, statusText, headers, body, responseCharset);
  }

  /**
   * Constructor with a status code and status text, headers, and content,
   * and an prepared message.
   */
  public HttpClientErrorException(@Nullable String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

    super(message, statusCode, statusText, headers, body, responseCharset);
  }

  /**
   * Create {@code HttpClientErrorException} or an HTTP status specific sub-class.
   */
  public static HttpClientErrorException create(HttpStatusCode statusCode,
          String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    return create(null, statusCode, statusText, headers, body, charset);
  }

  /**
   * Variant of {@link #create(HttpStatusCode, String, HttpHeaders, byte[], Charset)}
   * with an optional prepared message.
   */
  public static HttpClientErrorException create(@Nullable String message, HttpStatusCode statusCode,
          String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    if (statusCode instanceof HttpStatus status) {
      return switch (status) {
        case BAD_REQUEST -> new BadRequest(message, statusText, headers, body, charset);
        case UNAUTHORIZED -> new Unauthorized(message, statusText, headers, body, charset);
        case FORBIDDEN -> new Forbidden(message, statusText, headers, body, charset);
        case NOT_FOUND -> new NotFound(message, statusText, headers, body, charset);
        case METHOD_NOT_ALLOWED -> new MethodNotAllowed(message, statusText, headers, body, charset);
        case NOT_ACCEPTABLE -> new NotAcceptable(message, statusText, headers, body, charset);
        case CONFLICT -> new Conflict(message, statusText, headers, body, charset);
        case GONE -> new Gone(message, statusText, headers, body, charset);
        case UNSUPPORTED_MEDIA_TYPE -> new UnsupportedMediaType(message, statusText, headers, body, charset);
        case TOO_MANY_REQUESTS -> new TooManyRequests(message, statusText, headers, body, charset);
        case UNPROCESSABLE_ENTITY -> new UnprocessableEntity(message, statusText, headers, body, charset);
        default -> new HttpClientErrorException(message, statusCode, statusText, headers, body, charset);
      };
    }
    if (message != null) {
      return new HttpClientErrorException(message, statusCode, statusText, headers, body, charset);
    }
    else {
      return new HttpClientErrorException(statusCode, statusText, headers, body, charset);
    }
  }

  // Subclasses for specific HTTP status codes

  /**
   * {@link HttpClientErrorException} for status HTTP 400 Bad Request.
   */
  public static final class BadRequest extends HttpClientErrorException {

    private BadRequest(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 401 Unauthorized.
   */
  public static final class Unauthorized extends HttpClientErrorException {

    private Unauthorized(@Nullable String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(message, HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 403 Forbidden.
   */
  public static final class Forbidden extends HttpClientErrorException {

    private Forbidden(@Nullable String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(message, HttpStatus.FORBIDDEN, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 404 Not Found.
   */
  public static final class NotFound extends HttpClientErrorException {

    private NotFound(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.NOT_FOUND, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 405 Method Not Allowed.
   */
  public static final class MethodNotAllowed extends HttpClientErrorException {

    private MethodNotAllowed(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 406 Not Acceptable.
   */
  public static final class NotAcceptable extends HttpClientErrorException {

    private NotAcceptable(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 409 Conflict.
   */
  public static final class Conflict extends HttpClientErrorException {

    private Conflict(@Nullable String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(message, HttpStatus.CONFLICT, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 410 Gone.
   */
  public static final class Gone extends HttpClientErrorException {

    private Gone(@Nullable String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(message, HttpStatus.GONE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 415 Unsupported Media Type.
   */
  public static final class UnsupportedMediaType extends HttpClientErrorException {

    private UnsupportedMediaType(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 422 Unprocessable Entity.
   */
  public static final class UnprocessableEntity extends HttpClientErrorException {

    private UnprocessableEntity(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpClientErrorException} for status HTTP 429 Too Many Requests.
   */
  public static final class TooManyRequests extends HttpClientErrorException {

    private TooManyRequests(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
    }
  }

}
