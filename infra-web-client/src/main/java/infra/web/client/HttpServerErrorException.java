/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.nio.charset.Charset;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;

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
  public HttpServerErrorException(HttpStatusCode statusCode,
          String statusText, byte @Nullable [] body, @Nullable Charset charset) {
    super(statusCode, statusText, body, charset);
  }

  /**
   * Constructor with a status code and status text, headers, and content.
   */
  public HttpServerErrorException(HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, byte @Nullable [] body, @Nullable Charset charset) {
    super(statusCode, statusText, headers, body, charset);
  }

  /**
   * Constructor with a status code and status text, headers, content, and an
   * prepared message.
   */
  public HttpServerErrorException(@Nullable String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, byte @Nullable [] body, @Nullable Charset charset) {
    super(message, statusCode, statusText, headers, body, charset);
  }

  /**
   * Create an {@code HttpServerErrorException} or an HTTP status specific sub-class.
   */
  public static HttpServerErrorException create(HttpStatusCode statusCode,
          String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
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
