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

package infra.http.client.support;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import infra.core.AttributeAccessor;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 13:52
 */
class HttpRequestDecoratorTests {

  @Test
  void constructorWithValidRequest() {
    HttpRequest request = mock(HttpRequest.class);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getRequest()).isSameAs(request);
  }

  @Test
  void constructorWithNullRequestThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new HttpRequestDecorator(null))
            .withMessage("HttpRequest is required");
  }

  @Test
  void getMethodDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    when(request.getMethod()).thenReturn(HttpMethod.POST);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getMethod()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void getMethodValueDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    when(request.getMethodValue()).thenReturn("POST");
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getMethodValue()).isEqualTo("POST");
  }

  @Test
  void getURIDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    URI uri = URI.create("http://example.com");
    when(request.getURI()).thenReturn(uri);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getURI()).isSameAs(uri);
  }

  @Test
  void getHeadersDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(request.getHeaders()).thenReturn(headers);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getHeaders()).isSameAs(headers);
  }

  @Test
  void getAttributesDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    Map<String, Object> attributes = Map.of("key", "value");
    when(request.getAttributes()).thenReturn(attributes);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getAttributes()).isSameAs(attributes);
  }

  @Test
  void setAttributesDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    Map<String, Object> attributes = Map.of("key", "value");
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    decorator.setAttributes(attributes);
    verify(request).setAttributes(attributes);
  }

  @Test
  void attributeNamesDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    Iterable<String> attributeNames = List.of("attr1", "attr2");
    when(request.attributeNames()).thenReturn(attributeNames);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.attributeNames()).isSameAs(attributeNames);
  }

  @Test
  void clearAttributesDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    decorator.clearAttributes();
    verify(request).clearAttributes();
  }

  @Test
  void computeAttributeDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    Function<String, String> computeFunction = s -> "computed";
    when(request.computeAttribute("name", computeFunction)).thenReturn("computed");
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.computeAttribute("name", computeFunction)).isEqualTo("computed");
  }

  @Test
  void copyFromDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    AttributeAccessor source = mock(AttributeAccessor.class);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    decorator.copyFrom(source);
    verify(request).copyFrom(source);
  }

  @Test
  void getAttributeDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    when(request.getAttribute("name")).thenReturn("value");
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void getAttributeNamesDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    String[] attributeNames = { "attr1", "attr2" };
    when(request.getAttributeNames()).thenReturn(attributeNames);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.getAttributeNames()).isSameAs(attributeNames);
  }

  @Test
  void hasAttributeDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    when(request.hasAttribute("name")).thenReturn(true);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.hasAttribute("name")).isTrue();
  }

  @Test
  void hasAttributesDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    when(request.hasAttributes()).thenReturn(true);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.hasAttributes()).isTrue();
  }

  @Test
  void removeAttributeDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    when(request.removeAttribute("name")).thenReturn("value");
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    assertThat(decorator.removeAttribute("name")).isEqualTo("value");
  }

  @Test
  void setAttributeDelegatesToWrappedRequest() {
    HttpRequest request = mock(HttpRequest.class);
    HttpRequestDecorator decorator = new HttpRequestDecorator(request);
    decorator.setAttribute("name", "value");
    verify(request).setAttribute("name", "value");
  }

}