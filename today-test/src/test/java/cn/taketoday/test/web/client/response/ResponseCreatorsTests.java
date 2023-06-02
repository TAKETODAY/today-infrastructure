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

package cn.taketoday.test.web.client.response;

import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.http.client.MockClientHttpResponse;
import cn.taketoday.test.web.client.ResponseCreator;
import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the {@link MockRestResponseCreators} static factory methods.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("resource")
class ResponseCreatorsTests {

  @Test
  void success() throws Exception {
    MockClientHttpResponse response = (MockClientHttpResponse) MockRestResponseCreators.withSuccess().createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void successWithContent() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", MediaType.TEXT_PLAIN);
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(StreamUtils.copyToByteArray(response.getBody())).isEqualTo("foo".getBytes());
  }

  @Test
  void successWithContentWithoutContentType() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", null);
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isNull();
    assertThat(StreamUtils.copyToByteArray(response.getBody())).isEqualTo("foo".getBytes());
  }

  @Test
  void created() throws Exception {
    URI location = new URI("/foo");
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withCreatedEntity(location);
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).isEqualTo(location);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void accepted() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withAccepted();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void noContent() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withNoContent();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void badRequest() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withBadRequest();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void unauthorized() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withUnauthorizedRequest();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void forbiddenRequest() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withForbiddenRequest();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void resourceNotFound() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withResourceNotFound();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void requestConflict() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withRequestConflict();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void tooManyRequests() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withTooManyRequests();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.RETRY_AFTER);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void tooManyRequestsWithRetryAfter() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withTooManyRequests(512);
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("512");
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void serverError() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withServerError();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void badGateway() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withBadGateway();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void serviceUnavailable() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withServiceUnavailable();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void gatewayTimeout() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withGatewayTimeout();
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void withStatus() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN);
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
  }

  @Test
  void withCustomStatus() throws Exception {
    DefaultResponseCreator responseCreator = MockRestResponseCreators.withRawStatus(454);
    MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(454));
    assertThat(response.getStatusText()).isEmpty();
  }

  @Test
  void withException() {
    ResponseCreator responseCreator = MockRestResponseCreators.withException(new SocketTimeoutException());
    assertThatExceptionOfType(SocketTimeoutException.class)
            .isThrownBy(() -> responseCreator.createResponse(null));
  }

}
