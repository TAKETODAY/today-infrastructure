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