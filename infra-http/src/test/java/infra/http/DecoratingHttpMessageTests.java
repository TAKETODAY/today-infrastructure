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

package infra.http;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/10 22:13
 */
class DecoratingHttpMessageTests {
  @Test
  void delegateReturnsWrappedHttpMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.delegate()).isEqualTo(mockHttpMessage);
  }

  @Test
  void getHeadersDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    HttpHeaders mockHeaders = HttpHeaders.forWritable();
    when(mockHttpMessage.getHeaders()).thenReturn(mockHeaders);

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getHeaders()).isEqualTo(mockHeaders);
    verify(mockHttpMessage).getHeaders();
  }

  @Test
  void getHeaderDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    when(mockHttpMessage.getHeader("Test-Header")).thenReturn("test-value");
    when(mockHttpMessage.getHeader("Non-Existent")).thenReturn(null);

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getHeader("Test-Header")).isEqualTo("test-value");
    assertThat(decorator.getHeader("Non-Existent")).isNull();
    verify(mockHttpMessage).getHeader("Test-Header");
    verify(mockHttpMessage).getHeader("Non-Existent");
  }

  @Test
  void getHeadersListDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    List<String> testHeaders = List.of("value1", "value2");
    when(mockHttpMessage.getHeaders("Test-Header")).thenReturn(testHeaders);
    when(mockHttpMessage.getHeaders("Non-Existent")).thenReturn(List.of());

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getHeaders("Test-Header")).containsExactly("value1", "value2");
    assertThat(decorator.getHeaders("Non-Existent")).isEmpty();
    verify(mockHttpMessage).getHeaders("Test-Header");
    verify(mockHttpMessage).getHeaders("Non-Existent");
  }

  @Test
  void getHeaderNamesDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    Collection<String> headerNames = List.of("Header-One", "Header-Two");
    when(mockHttpMessage.getHeaderNames()).thenReturn(headerNames);

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getHeaderNames()).containsExactlyInAnyOrder("Header-One", "Header-Two");
    verify(mockHttpMessage).getHeaderNames();
  }

  @Test
  void getContentTypeDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    MediaType testMediaType = MediaType.APPLICATION_JSON;
    when(mockHttpMessage.getContentType()).thenReturn(testMediaType);

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getContentType()).isEqualTo(testMediaType);
    verify(mockHttpMessage).getContentType();
  }

  @Test
  void getContentTypeAsStringDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    when(mockHttpMessage.getContentTypeAsString()).thenReturn("application/json;charset=UTF-8");

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getContentTypeAsString()).isEqualTo("application/json;charset=UTF-8");
    verify(mockHttpMessage).getContentTypeAsString();
  }

  @Test
  void getContentLengthDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    when(mockHttpMessage.getContentLength()).thenReturn(1024L);

    DecoratingHttpMessage decorator = new DecoratingHttpMessage(mockHttpMessage);

    assertThat(decorator.getContentLength()).isEqualTo(1024L);
    verify(mockHttpMessage).getContentLength();
  }

}