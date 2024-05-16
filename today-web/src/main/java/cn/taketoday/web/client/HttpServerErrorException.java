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
 * Exception thrown when an HTTP 5xx is received.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultResponseErrorHandler
 * @since 4.0
 */
public class HttpServerErrorException extends HttpStatusCodeException {

  /**
   * Constructor with a status code only.
   */
  public HttpServerErrorException(HttpStatusCode statusCode) {
    super(statusCode);
  }

  /**
   * Constructor with a status code and status text.
   */
  public HttpServerErrorException(HttpStatusCode statusCode, String statusText) {
    super(statusCode, statusText);
  }

  /**
   * Constructor with a status code and status text, and content.
   */
  public HttpServerErrorException(
          HttpStatusCode statusCode, String statusText, @Nullable byte[] body, @Nullable Charset charset) {
    super(statusCode, statusText, body, charset);
  }

  /**
   * Constructor with a status code and status text, headers, and content.
   */
  public HttpServerErrorException(HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {
    super(statusCode, statusText, headers, body, charset);
  }

  /**
   * Constructor with a status code and status text, headers, content, and an
   * prepared message.
   */
  public HttpServerErrorException(@Nullable String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {
    super(message, statusCode, statusText, headers, body, charset);
  }

  /**
   * Create an {@code HttpServerErrorException} or an HTTP status specific sub-class.
   */
  public static HttpServerErrorException create(
          HttpStatus statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
    return create(null, statusCode, statusText, headers, body, charset);
  }

  /**
   * Variant of {@link #create(String, HttpStatusCode, String, HttpHeaders, byte[], Charset)}
   * with an optional prepared message.
   */
  public static HttpServerErrorException create(@Nullable String message, HttpStatusCode statusCode,
          String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    if (statusCode instanceof HttpStatus status) {
      return switch (status) {
        case INTERNAL_SERVER_ERROR -> new InternalServerError(message, statusText, headers, body, charset);
        case NOT_IMPLEMENTED -> new NotImplemented(message, statusText, headers, body, charset);
        case BAD_GATEWAY -> new BadGateway(message, statusText, headers, body, charset);
        case SERVICE_UNAVAILABLE -> new ServiceUnavailable(message, statusText, headers, body, charset);
        case GATEWAY_TIMEOUT -> new GatewayTimeout(message, statusText, headers, body, charset);
        default -> new HttpServerErrorException(message, statusCode, statusText, headers, body, charset);
      };
    }
    if (message != null) {
      return new HttpServerErrorException(message, statusCode, statusText, headers, body, charset);
    }
    else {
      return new HttpServerErrorException(statusCode, statusText, headers, body, charset);
    }

  }

  // Subclasses for specific HTTP status codes

  /**
   * {@link HttpServerErrorException} for status HTTP 500 Internal Server Error.
   */
  public static final class InternalServerError extends HttpServerErrorException {

    @Serial
    private static final long serialVersionUID = 1L;

    private InternalServerError(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP 501 Not Implemented.
   */
  public static final class NotImplemented extends HttpServerErrorException {

    @Serial
    private static final long serialVersionUID = 1L;

    private NotImplemented(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP HTTP 502 Bad Gateway.
   */
  public static final class BadGateway extends HttpServerErrorException {

    private BadGateway(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP 503 Service Unavailable.
   */
  public static final class ServiceUnavailable extends HttpServerErrorException {

    @Serial
    private static final long serialVersionUID = 1L;

    private ServiceUnavailable(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP 504 Gateway Timeout.
   */
  public static final class GatewayTimeout extends HttpServerErrorException {

    @Serial
    private static final long serialVersionUID = 1L;

    private GatewayTimeout(@Nullable String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
    }
  }

}
