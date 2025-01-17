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

package infra.web.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.client.ClientHttpResponse;

import static infra.http.HttpStatus.BAD_GATEWAY;
import static infra.http.HttpStatus.BAD_REQUEST;
import static infra.http.HttpStatus.CONFLICT;
import static infra.http.HttpStatus.FORBIDDEN;
import static infra.http.HttpStatus.GATEWAY_TIMEOUT;
import static infra.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static infra.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static infra.http.HttpStatus.I_AM_A_TEAPOT;
import static infra.http.HttpStatus.METHOD_NOT_ALLOWED;
import static infra.http.HttpStatus.NOT_ACCEPTABLE;
import static infra.http.HttpStatus.NOT_FOUND;
import static infra.http.HttpStatus.NOT_IMPLEMENTED;
import static infra.http.HttpStatus.SERVICE_UNAVAILABLE;
import static infra.http.HttpStatus.TOO_MANY_REQUESTS;
import static infra.http.HttpStatus.UNAUTHORIZED;
import static infra.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultResponseErrorHandler} handling of specific
 * HTTP status codes.
 */
class DefaultResponseErrorHandlerHttpStatusTests {

  private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

  private final ClientHttpResponse response = mock(ClientHttpResponse.class);

  @ParameterizedTest(name = "[{index}] error: [{0}]")
  @DisplayName("hasError() returns true")
  @MethodSource("errorCodes")
  void hasErrorTrue(HttpStatus httpStatus) throws Exception {
    given(this.response.getRawStatusCode()).willReturn(httpStatus.value());
    given(this.response.getStatusCode()).willReturn(httpStatus);
    assertThat(this.handler.hasError(this.response)).isTrue();
  }

  @ParameterizedTest(name = "[{index}] error: [{0}], exception: [{1}]")
  @DisplayName("handleError() throws an exception")
  @MethodSource("errorCodes")
  void handleErrorException(HttpStatus httpStatus, Class<? extends Throwable> expectedExceptionClass) throws Exception {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);

    given(this.response.getRawStatusCode()).willReturn(httpStatus.value());
    given(this.response.getStatusCode()).willReturn(httpStatus);
    given(this.response.getHeaders()).willReturn(headers);

    HttpRequest request = mockRequest();

    assertThatExceptionOfType(expectedExceptionClass)
            .isThrownBy(() -> this.handler.handleError(request, this.response));
  }

  static HttpRequest mockRequest() throws URISyntaxException {
    HttpRequest request = mock();
    given(request.getURI()).willReturn(new URI("/"));
    given(request.getMethod()).willReturn(HttpMethod.GET);
    given(request.getHeaders()).willReturn(HttpHeaders.forWritable());
    given(request.getAttributes()).willReturn(new HashMap<>());
    return request;
  }

  static Object[][] errorCodes() {
    return new Object[][] {
            // 4xx
            { BAD_REQUEST, HttpClientErrorException.BadRequest.class },
            { UNAUTHORIZED, HttpClientErrorException.Unauthorized.class },
            { FORBIDDEN, HttpClientErrorException.Forbidden.class },
            { NOT_FOUND, HttpClientErrorException.NotFound.class },
            { METHOD_NOT_ALLOWED, HttpClientErrorException.MethodNotAllowed.class },
            { NOT_ACCEPTABLE, HttpClientErrorException.NotAcceptable.class },
            { CONFLICT, HttpClientErrorException.Conflict.class },
            { TOO_MANY_REQUESTS, HttpClientErrorException.TooManyRequests.class },
            { UNPROCESSABLE_ENTITY, HttpClientErrorException.UnprocessableEntity.class },
            { I_AM_A_TEAPOT, HttpClientErrorException.class },
            // 5xx
            { INTERNAL_SERVER_ERROR, HttpServerErrorException.InternalServerError.class },
            { NOT_IMPLEMENTED, HttpServerErrorException.NotImplemented.class },
            { BAD_GATEWAY, HttpServerErrorException.BadGateway.class },
            { SERVICE_UNAVAILABLE, HttpServerErrorException.ServiceUnavailable.class },
            { GATEWAY_TIMEOUT, HttpServerErrorException.GatewayTimeout.class },
            { HTTP_VERSION_NOT_SUPPORTED, HttpServerErrorException.class }
    };
  }

}
