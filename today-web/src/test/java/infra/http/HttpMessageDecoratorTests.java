/*
 * Copyright 2017 - 2025 the original author or authors.
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
class HttpMessageDecoratorTests {
  @Test
  void delegateReturnsWrappedHttpMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

    assertThat(decorator.delegate()).isEqualTo(mockHttpMessage);
  }

  @Test
  void getHeadersDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    HttpHeaders mockHeaders = HttpHeaders.forWritable();
    when(mockHttpMessage.getHeaders()).thenReturn(mockHeaders);

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

    assertThat(decorator.getHeaders()).isEqualTo(mockHeaders);
    verify(mockHttpMessage).getHeaders();
  }

  @Test
  void getHeaderDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    when(mockHttpMessage.getHeader("Test-Header")).thenReturn("test-value");
    when(mockHttpMessage.getHeader("Non-Existent")).thenReturn(null);

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

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

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

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

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

    assertThat(decorator.getHeaderNames()).containsExactlyInAnyOrder("Header-One", "Header-Two");
    verify(mockHttpMessage).getHeaderNames();
  }

  @Test
  void getContentTypeDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    MediaType testMediaType = MediaType.APPLICATION_JSON;
    when(mockHttpMessage.getContentType()).thenReturn(testMediaType);

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

    assertThat(decorator.getContentType()).isEqualTo(testMediaType);
    verify(mockHttpMessage).getContentType();
  }

  @Test
  void getContentTypeAsStringDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    when(mockHttpMessage.getContentTypeAsString()).thenReturn("application/json;charset=UTF-8");

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

    assertThat(decorator.getContentTypeAsString()).isEqualTo("application/json;charset=UTF-8");
    verify(mockHttpMessage).getContentTypeAsString();
  }

  @Test
  void getContentLengthDelegatesToWrappedMessage() {
    HttpMessage mockHttpMessage = mock(HttpMessage.class);
    when(mockHttpMessage.getContentLength()).thenReturn(1024L);

    HttpMessageDecorator decorator = new HttpMessageDecorator(mockHttpMessage);

    assertThat(decorator.getContentLength()).isEqualTo(1024L);
    verify(mockHttpMessage).getContentLength();
  }

}