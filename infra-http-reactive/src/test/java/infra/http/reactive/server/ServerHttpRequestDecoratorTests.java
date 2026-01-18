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

package infra.http.reactive.server;

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