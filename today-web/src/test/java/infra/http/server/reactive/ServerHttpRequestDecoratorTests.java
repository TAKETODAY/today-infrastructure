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

package infra.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import infra.core.AttributeAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 14:00
 */
@SuppressWarnings("cast")
class ServerHttpRequestDecoratorTests {
  @Test
  void delegateMethodsAreCalled() {
    ServerHttpRequest delegate = mock(ServerHttpRequest.class);
    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(delegate);

    decorator.getId();
    verify(delegate).getId();

    decorator.getMethod();
    verify(delegate).getMethod();

    decorator.getMethodAsString();
    verify(delegate).getMethodAsString();

    decorator.getURI();
    verify(delegate).getURI();

    decorator.getPath();
    verify(delegate).getPath();

    decorator.getQueryParams();
    verify(delegate).getQueryParams();

    decorator.getHeaders();
    verify(delegate).getHeaders();

    decorator.getCookies();
    verify(delegate).getCookies();

    decorator.getLocalAddress();
    verify(delegate).getLocalAddress();

    decorator.getRemoteAddress();
    verify(delegate).getRemoteAddress();

    decorator.getSslInfo();
    verify(delegate).getSslInfo();

    decorator.getBody();
    verify(delegate).getBody();

    decorator.getAttributes();
    verify(delegate).getAttributes();

    Map<String, Object> attributes = Map.of();
    decorator.setAttributes(attributes);
    verify(delegate).setAttributes(attributes);

    decorator.attributeNames();
    verify(delegate).attributeNames();

    decorator.clearAttributes();
    verify(delegate).clearAttributes();

    Function<String, String> computeFunction = s -> "value";
    decorator.computeAttribute("name", computeFunction);
    verify(delegate).computeAttribute("name", computeFunction);

    AttributeAccessor source = mock(AttributeAccessor.class);
    decorator.copyFrom(source);
    verify(delegate).copyFrom(source);

    decorator.getAttribute("name");
    verify(delegate).getAttribute("name");

    decorator.getAttributeNames();
    verify(delegate).getAttributeNames();

    decorator.hasAttribute("name");
    verify(delegate).hasAttribute("name");

    decorator.hasAttributes();
    verify(delegate).hasAttributes();

    decorator.removeAttribute("name");
    verify(delegate).removeAttribute("name");

    decorator.setAttribute("name", "value");
    verify(delegate).setAttribute("name", "value");
  }

  @Test
  void getNativeRequestWithAbstractServerHttpRequest() {
    AbstractServerHttpRequest abstractRequest = mock(AbstractServerHttpRequest.class);
    Object nativeRequest = new Object();
    when(abstractRequest.getNativeRequest()).thenReturn(nativeRequest);

    assertThat((Object) ServerHttpRequestDecorator.getNativeRequest(abstractRequest)).isSameAs(nativeRequest);
  }

  @Test
  void getNativeRequestWithDecorator() {
    AbstractServerHttpRequest abstractRequest = mock(AbstractServerHttpRequest.class);
    Object nativeRequest = new Object();
    when(abstractRequest.getNativeRequest()).thenReturn(nativeRequest);

    ServerHttpRequestDecorator decorator1 = new ServerHttpRequestDecorator(abstractRequest);
    ServerHttpRequestDecorator decorator2 = new ServerHttpRequestDecorator(decorator1);

    assertThat((Object) ServerHttpRequestDecorator.getNativeRequest(decorator2)).isSameAs(nativeRequest);
  }

  @Test
  void getNativeRequestWithUnknownType() {
    ServerHttpRequest unknownRequest = mock(ServerHttpRequest.class);

    assertThatIllegalArgumentException()
            .isThrownBy(() -> ServerHttpRequestDecorator.getNativeRequest(unknownRequest))
            .withMessageContaining("Can't find native request");
  }

  @Test
  void constructorWithNullDelegate() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ServerHttpRequestDecorator(null))
            .withMessageContaining("Delegate is required");
  }

  @Test
  void toStringContainsDelegate() {
    ServerHttpRequest delegate = mock(ServerHttpRequest.class);
    when(delegate.toString()).thenReturn("MockDelegate");
    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(delegate);

    assertThat(decorator.toString()).contains("MockDelegate");
  }

}