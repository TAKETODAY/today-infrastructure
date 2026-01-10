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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.client.ClientHttpResponse;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;

import static infra.web.client.DefaultResponseErrorHandlerHttpStatusTests.mockRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class ExtractingResponseErrorHandlerTests {

  private ExtractingResponseErrorHandler errorHandler;

  private final ClientHttpResponse response = mock(ClientHttpResponse.class);

  @BeforeEach
  public void setup() {
    HttpMessageConverter<Object> converter = new JacksonJsonHttpMessageConverter();
    this.errorHandler = new ExtractingResponseErrorHandler(
            Collections.singletonList(converter));

    this.errorHandler.setStatusMapping(
            Collections.singletonMap(HttpStatus.I_AM_A_TEAPOT, MyRestClientException.class));
    this.errorHandler.setSeriesMapping(Collections
            .singletonMap(HttpStatus.Series.SERVER_ERROR, MyRestClientException.class));
  }

  @Test
  public void hasError() throws Exception {
    given(response.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
    assertThat(this.errorHandler.hasError(this.response)).isTrue();

    given(response.getStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(this.errorHandler.hasError(this.response)).isTrue();

    given(response.getStatusCode()).willReturn(HttpStatus.OK);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
    assertThat(this.errorHandler.hasError(this.response)).isFalse();
  }

  @Test
  public void hasErrorOverride() throws Exception {
    this.errorHandler.setSeriesMapping(Collections
            .singletonMap(HttpStatus.Series.CLIENT_ERROR, null));
    given(response.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
    assertThat(this.errorHandler.hasError(this.response)).isTrue();

    given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    assertThat(this.errorHandler.hasError(this.response)).isFalse();

    given(response.getStatusCode()).willReturn(HttpStatus.OK);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
    assertThat(this.errorHandler.hasError(this.response)).isFalse();
  }

  @Test
  public void handleErrorStatusMatch() throws Exception {
    given(response.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
    given(response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(response.getContentType()).willReturn(MediaType.APPLICATION_JSON);
    given(response.getContentLength()).willReturn((long) body.length);
    given(response.getBody()).willReturn(new ByteArrayInputStream(body));

    assertThatExceptionOfType(MyRestClientException.class)
            .isThrownBy(() -> errorHandler.handleError(mockRequest(), response))
            .satisfies(ex -> assertThat(ex.getFoo()).isEqualTo("bar"));
  }

  @Test
  public void handleErrorSeriesMatch() throws Exception {
    given(response.getStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(response.getHeaders()).willReturn(responseHeaders);
    given(response.getContentType()).willReturn(MediaType.APPLICATION_JSON);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(response.getContentLength()).willReturn((long) body.length);
    given(response.getBody()).willReturn(new ByteArrayInputStream(body));

    assertThatExceptionOfType(MyRestClientException.class)
            .isThrownBy(() -> errorHandler.handleError(mockRequest(), response))
            .satisfies(ex -> Assertions.assertThat(ex.getFoo()).isEqualTo("bar"));
  }

  @Test
  public void handleNoMatch() throws Exception {
    given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(this.response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

    assertThatExceptionOfType(HttpClientErrorException.class)
            .isThrownBy(() -> this.errorHandler.handleError(mockRequest(), this.response))
            .satisfies(ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(ex.getResponseBodyAsByteArray()).isEqualTo(body);
            });
  }

  @Test
  public void handleNoMatchOverride() throws Exception {
    this.errorHandler.setSeriesMapping(Collections
            .singletonMap(HttpStatus.Series.CLIENT_ERROR, null));
    given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(this.response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

    this.errorHandler.handleError(mockRequest(), this.response);
  }

  @SuppressWarnings("serial")
  private static class MyRestClientException extends RestClientException {

    private String foo;

    public MyRestClientException(String msg) {
      super(msg);
    }

    public MyRestClientException(String msg, Throwable ex) {
      super(msg, ex);
    }

    public String getFoo() {
      return this.foo;
    }

  }

}
