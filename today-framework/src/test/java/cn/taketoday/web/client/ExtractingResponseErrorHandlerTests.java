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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;

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
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
    assertThat(this.errorHandler.hasError(this.response)).isTrue();

    given(this.response.getRawStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(this.errorHandler.hasError(this.response)).isTrue();

    given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
    assertThat(this.errorHandler.hasError(this.response)).isFalse();
  }

  @Test
  public void hasErrorOverride() throws Exception {
    this.errorHandler.setSeriesMapping(Collections
            .singletonMap(HttpStatus.Series.CLIENT_ERROR, null));

    given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
    assertThat(this.errorHandler.hasError(this.response)).isTrue();

    given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    assertThat(this.errorHandler.hasError(this.response)).isFalse();

    given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
    assertThat(this.errorHandler.hasError(this.response)).isFalse();
  }

  @Test
  public void handleErrorStatusMatch() throws Exception {
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
    HttpHeaders responseHeaders = HttpHeaders.create();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(this.response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

    assertThatExceptionOfType(MyRestClientException.class).isThrownBy(() ->
                    this.errorHandler.handleError(this.response))
            .satisfies(ex -> assertThat(ex.getFoo()).isEqualTo("bar"));
  }

  @Test
  public void handleErrorSeriesMatch() throws Exception {
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
    HttpHeaders responseHeaders = HttpHeaders.create();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(this.response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

    assertThatExceptionOfType(MyRestClientException.class).isThrownBy(() ->
                    this.errorHandler.handleError(this.response))
            .satisfies(ex -> assertThat(ex.getFoo()).isEqualTo("bar"));
  }

  @Test
  public void handleNoMatch() throws Exception {
    given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    HttpHeaders responseHeaders = HttpHeaders.create();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(this.response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

    assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
                    this.errorHandler.handleError(this.response))
            .satisfies(ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(ex.getResponseBodyAsByteArray()).isEqualTo(body);
            });
  }

  @Test
  public void handleNoMatchOverride() throws Exception {
    this.errorHandler.setSeriesMapping(Collections
            .singletonMap(HttpStatus.Series.CLIENT_ERROR, null));

    given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    HttpHeaders responseHeaders = HttpHeaders.create();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(this.response.getHeaders()).willReturn(responseHeaders);

    byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    responseHeaders.setContentLength(body.length);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

    this.errorHandler.handleError(this.response);
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
