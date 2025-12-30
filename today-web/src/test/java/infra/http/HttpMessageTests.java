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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/10 17:35
 */
class HttpMessageTests {

  @Test
  void getHeadersReturnsEmptyHeadersWhenNoHeadersSet() {
    HttpMessage httpMessage = createHttpMessage();

    assertThat(httpMessage.getHeaders()).isNotNull();
    assertThat(httpMessage.getHeaders().size()).isEqualTo(0);
  }

  @Test
  void getHeaderReturnsNullWhenHeaderNotPresent() {
    HttpMessage httpMessage = createHttpMessage();

    assertThat(httpMessage.getHeader("NonExistentHeader")).isNull();
  }

  @Test
  void getHeaderReturnsFirstValueWhenHeaderPresent() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().add("Test-Header", "value1");
    httpMessage.getHeaders().add("Test-Header", "value2");

    assertThat(httpMessage.getHeader("Test-Header")).isEqualTo("value1");
  }

  @Test
  void getHeadersReturnsAllValuesWhenHeaderPresent() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().add("Test-Header", "value1");
    httpMessage.getHeaders().add("Test-Header", "value2");

    List<String> headerValues = httpMessage.getHeaders("Test-Header");
    assertThat(headerValues).containsExactly("value1", "value2");
  }

  @Test
  void getHeadersReturnsEmptyListWhenHeaderNotPresent() {
    HttpMessage httpMessage = createHttpMessage();

    List<String> headerValues = httpMessage.getHeaders("NonExistentHeader");
    assertThat(headerValues).isEmpty();
  }

  @Test
  void getHeaderNamesReturnsAllHeaderNames() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().add("Header-One", "value1");
    httpMessage.getHeaders().add("Header-Two", "value2");

    Collection<String> headerNames = httpMessage.getHeaderNames();
    assertThat(headerNames).containsExactlyInAnyOrder("Header-One", "Header-Two");
  }

  @Test
  void getContentTypeReturnsNullWhenNotSet() {
    HttpMessage httpMessage = createHttpMessage();

    assertThat(httpMessage.getContentType()).isNull();
  }

  @Test
  void getContentTypeReturnsParsedMediaTypeWhenSet() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    assertThat(httpMessage.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void getContentTypeAsStringReturnsRawHeaderValue() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().set("Content-Type", "application/json;charset=UTF-8");

    assertThat(httpMessage.getContentTypeAsString()).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  void getContentTypeAsStringReturnsNullWhenNotSet() {
    HttpMessage httpMessage = createHttpMessage();

    assertThat(httpMessage.getContentTypeAsString()).isNull();
  }

  @Test
  void getContentLengthReturnsNegativeOneWhenNotSet() {
    HttpMessage httpMessage = createHttpMessage();

    assertThat(httpMessage.getContentLength()).isEqualTo(-1);
  }

  @Test
  void getContentLengthReturnsSetValue() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().setContentLength(1024);

    assertThat(httpMessage.getContentLength()).isEqualTo(1024);
  }

  @Test
  void getContentLengthReturnsZeroWhenSetToZero() {
    HttpMessage httpMessage = createHttpMessage();
    httpMessage.getHeaders().setContentLength(0);

    assertThat(httpMessage.getContentLength()).isEqualTo(0);
  }

  private HttpMessage createHttpMessage() {
    return new HttpMessage() {
      private final HttpHeaders headers = HttpHeaders.forWritable();

      @Override
      public HttpHeaders getHeaders() {
        return headers;
      }
    };
  }

}