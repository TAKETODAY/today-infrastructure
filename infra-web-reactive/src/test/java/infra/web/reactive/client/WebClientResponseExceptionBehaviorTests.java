/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.reactive.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebClientResponseExceptionBehaviorTests {

  @Test
  void createsStatusSpecificExceptionTypes() {
    assertThat(WebClientResponseException.create(HttpStatus.BAD_REQUEST, "bad", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.BadRequest.class);
    assertThat(WebClientResponseException.create(HttpStatus.UNAUTHORIZED, "unauthorized", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.Unauthorized.class);
    assertThat(WebClientResponseException.create(HttpStatus.FORBIDDEN, "forbidden", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.Forbidden.class);
    assertThat(WebClientResponseException.create(HttpStatus.NOT_FOUND, "not found", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.NotFound.class);
    assertThat(WebClientResponseException.create(HttpStatus.METHOD_NOT_ALLOWED, "method", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.MethodNotAllowed.class);
    assertThat(WebClientResponseException.create(HttpStatus.NOT_ACCEPTABLE, "acceptable", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.NotAcceptable.class);
    assertThat(WebClientResponseException.create(HttpStatus.CONFLICT, "conflict", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.Conflict.class);
    assertThat(WebClientResponseException.create(HttpStatus.GONE, "gone", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.Gone.class);
    assertThat(WebClientResponseException.create(HttpStatus.PRECONDITION_FAILED, "precondition", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.PreconditionFailed.class);
    assertThat(WebClientResponseException.create(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "media", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.UnsupportedMediaType.class);
    assertThat(WebClientResponseException.create(HttpStatus.TOO_MANY_REQUESTS, "requests", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.TooManyRequests.class);
    assertThat(WebClientResponseException.create(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.UnprocessableEntity.class);
    assertThat(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR, "server", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.InternalServerError.class);
    assertThat(WebClientResponseException.create(HttpStatus.NOT_IMPLEMENTED, "implemented", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.NotImplemented.class);
    assertThat(WebClientResponseException.create(HttpStatus.BAD_GATEWAY, "gateway", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.BadGateway.class);
    assertThat(WebClientResponseException.create(HttpStatus.SERVICE_UNAVAILABLE, "unavailable", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.ServiceUnavailable.class);
    assertThat(WebClientResponseException.create(HttpStatus.GATEWAY_TIMEOUT, "timeout", HttpHeaders.empty(), new byte[0], null, null))
            .isInstanceOf(WebClientResponseException.GatewayTimeout.class);
  }

  @Test
  void retainsResponseDataAndUsesConfiguredCharset() {
    HttpHeaders headers = HttpHeaders.forWritable();
    byte[] body = "你好".getBytes(StandardCharsets.UTF_16);
    var exception = new WebClientResponseException(HttpStatus.BAD_REQUEST, "bad", headers, body,
            StandardCharsets.UTF_16, null);

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getRawStatusCode()).isEqualTo(400);
    assertThat(exception.getStatusText()).isEqualTo("bad");
    assertThat(exception.getHeaders()).isNotSameAs(headers);
    assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    assertThat(exception.getResponseBodyAsString()).isEqualTo("你好");
    assertThat(exception.getRequest()).isNull();
  }

  @Test
  void decodesBodyWithClassAndParameterizedType() {
    var exception = new WebClientResponseException(HttpStatus.BAD_REQUEST, "bad", null,
            "body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, null);
    exception.setBodyDecodeFunction(type -> type.toString());

    assertThat(exception.getResponseBodyAs(String.class)).contains("java.lang.String");
    assertThat(exception.getResponseBodyAs(new ParameterizedTypeReference<String>() { }))
            .contains("java.lang.String");
  }

  @Test
  void requiresDecoderFunctionBeforeDecodingBody() {
    var exception = new WebClientResponseException(HttpStatus.BAD_REQUEST, "bad", null, null, null, null);

    assertThatThrownBy(() -> exception.getResponseBodyAs(ResolvableType.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Decoder function not set");
  }

  @Test
  void createsGenericExceptionForUnknownStatusCode() {
    HttpStatusCode status = HttpStatusCode.valueOf(599);

    assertThat(WebClientResponseException.create(status, "unknown", HttpHeaders.empty(), new byte[0], null, null))
            .isExactlyInstanceOf(WebClientResponseException.class);
  }

}
