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

package infra.web.multipart.support;

import org.junit.jupiter.api.Test;

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:42
 */
class AbstractMultipartTests {

  @Test
  void getHeadersReturnsDefaultHeadersWhenNotSet() {
    AbstractPart multipart = mock(AbstractPart.class);
    when(multipart.getHeaders()).thenCallRealMethod();
    when(multipart.getContentType()).thenReturn("text/plain");
    when(multipart.createHttpHeaders()).thenCallRealMethod();

    HttpHeaders headers = multipart.getHeaders();

    assertThat(headers).isNotNull();
    assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void getHeadersReturnsSetHeaders() {
    AbstractPart multipart = mock(AbstractPart.class);
    HttpHeaders customHeaders = new DefaultHttpHeaders();
    customHeaders.set("X-Custom", "value");

    when(multipart.getHeaders()).thenCallRealMethod();
    multipart.headers = customHeaders;

    assertThat(multipart.getHeaders()).isSameAs(customHeaders);
  }

  @Test
  void createHttpHeadersIncludesContentType() {
    AbstractPart multipart = mock(AbstractPart.class);
    when(multipart.createHttpHeaders()).thenCallRealMethod();
    when(multipart.getContentType()).thenReturn("application/json");

    HttpHeaders headers = multipart.createHttpHeaders();

    assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void createHttpHeadersHandlesNullContentType() {
    AbstractPart multipart = mock(AbstractPart.class);
    when(multipart.createHttpHeaders()).thenCallRealMethod();
    when(multipart.getContentType()).thenReturn(null);

    HttpHeaders headers = multipart.createHttpHeaders();

    assertThat(headers.getContentType()).isNull();
  }

  @Test
  void toStringReturnsFormattedString() {
    AbstractPart multipart = mock(AbstractPart.class);
    when(multipart.toString()).thenCallRealMethod();
    when(multipart.getName()).thenReturn("test-name");
    when(multipart.getContentAsString()).thenReturn("test-value");

    String result = multipart.toString();

    assertThat(result).endsWith("test-name=test-value");
  }

}