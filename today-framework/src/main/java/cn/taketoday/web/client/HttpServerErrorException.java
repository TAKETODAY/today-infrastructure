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
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when an HTTP 5xx is received.
 *
 * @author Arjen Poutsma
 * @see DefaultResponseErrorHandler
 * @since 4.0
 */
public class HttpServerErrorException extends HttpStatusCodeException {

  @Serial
  private static final long serialVersionUID = -2915754006618138282L;

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
  public HttpServerErrorException(String message, HttpStatusCode statusCode, String statusText,
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
  public static HttpServerErrorException create(
          @Nullable String message, HttpStatusCode statusCode,
          String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

    if (statusCode instanceof HttpStatus status) {
      return switch (status) {
        case INTERNAL_SERVER_ERROR -> message != null ?
                                      new InternalServerError(message, statusText, headers, body, charset) :
                                      new InternalServerError(statusText, headers, body, charset);
        case NOT_IMPLEMENTED -> message != null ?
                                new NotImplemented(message, statusText, headers, body, charset) :
                                new NotImplemented(statusText, headers, body, charset);
        case BAD_GATEWAY -> message != null ?
                            new BadGateway(message, statusText, headers, body, charset) :
                            new BadGateway(statusText, headers, body, charset);
        case SERVICE_UNAVAILABLE -> message != null ?
                                    new ServiceUnavailable(message, statusText, headers, body, charset) :
                                    new ServiceUnavailable(statusText, headers, body, charset);
        case GATEWAY_TIMEOUT -> message != null ?
                                new GatewayTimeout(message, statusText, headers, body, charset) :
                                new GatewayTimeout(statusText, headers, body, charset);
        default -> message != null ?
                   new HttpServerErrorException(message, statusCode, statusText, headers, body, charset) :
                   new HttpServerErrorException(statusCode, statusText, headers, body, charset);
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

    private InternalServerError(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
    }

    private InternalServerError(String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP 501 Not Implemented.
   */
  public static final class NotImplemented extends HttpServerErrorException {

    private NotImplemented(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
    }

    private NotImplemented(String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP HTTP 502 Bad Gateway.
   */
  public static final class BadGateway extends HttpServerErrorException {

    private BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
    }

    private BadGateway(String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP 503 Service Unavailable.
   */
  public static final class ServiceUnavailable extends HttpServerErrorException {

    private ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
    }

    private ServiceUnavailable(String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
    }
  }

  /**
   * {@link HttpServerErrorException} for status HTTP 504 Gateway Timeout.
   */
  public static final class GatewayTimeout extends HttpServerErrorException {

    private GatewayTimeout(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
      super(HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
    }

    private GatewayTimeout(String message, String statusText,
            HttpHeaders headers, byte[] body, @Nullable Charset charset) {

      super(message, HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
    }
  }

}
