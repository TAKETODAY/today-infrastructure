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
import infra.http.converter.json.MappingJackson2HttpMessageConverter;

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
    HttpMessageConverter<Object> converter = new MappingJackson2HttpMessageConverter();
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

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
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
