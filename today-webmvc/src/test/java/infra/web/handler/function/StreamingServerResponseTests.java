/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.web.handler.function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import infra.http.CacheControl;
import infra.http.MediaType;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/17 14:39
 */
class StreamingServerResponseTests {

  private HttpMockRequestImpl mockRequest;

  private MockHttpResponseImpl mockResponse;

  MockRequestContext requestContext;

  @BeforeEach
  void setUp() {
    this.mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    this.mockRequest.setAsyncSupported(true);
    this.mockResponse = new MockHttpResponseImpl();

    requestContext = new MockRequestContext(mockRequest, mockResponse);
  }

  @Test
  void writeSingleString() throws Throwable {
    String body = "data: foo bar\n\n";
    ServerResponse response = ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .stream(stream -> {
              try {
                stream.write(body).complete();
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            });

    ServerResponse.Context context = Collections::emptyList;
    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isNull();
    assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM.toString());
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(body);
  }

  @Test
  void writeBytes() throws Throwable {
    String body = "data: foo bar\n\n";
    ServerResponse response = ServerResponse
            .ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .cacheControl(CacheControl.noCache())
            .stream(stream -> {
              try {
                stream.write(body.getBytes(StandardCharsets.UTF_8)).complete();
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            });
    ServerResponse.Context context = Collections::emptyList;
    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isNull();
    assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM.toString());
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(body);
  }

  @Test
  void writeWithConverters() throws Throwable {
    ServerResponse response = ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_NDJSON)
            .cacheControl(CacheControl.noCache())
            .stream(stream -> {
              try {
                stream.write(new Person("John", 51), MediaType.APPLICATION_JSON)
                        .write(new byte[] { '\n' })
                        .flush();
                stream.write(new Person("Jane", 42), MediaType.APPLICATION_JSON)
                        .write(new byte[] { '\n' })
                        .complete();
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            });

    ServerResponse.Context context = () -> Collections.singletonList(new JacksonJsonHttpMessageConverter());
    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isNull();
    assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.APPLICATION_NDJSON.toString());
    assertThat(this.mockResponse.getContentAsString()).isEqualTo("""
            {"name":"John","age":51}
            {"name":"Jane","age":42}
            """);
  }

  record Person(String name, int age) {

  }

}