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

package infra.web.handler;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.http.HttpStatus;
import infra.http.converter.StringHttpMessageConverter;
import infra.session.SessionRedirectModelManager;
import infra.web.ReturnValueHandler;
import infra.web.accept.ContentNegotiationManager;
import infra.web.bind.resolver.HttpEntityMethodProcessor;
import infra.web.bind.resolver.RequestResponseBodyMethodProcessor;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.result.HttpHeadersReturnValueHandler;
import infra.web.handler.result.HttpStatusReturnValueHandler;
import infra.web.handler.result.ObjectHandlerMethodReturnValueHandler;
import infra.web.handler.result.ResponseEntityReturnValueHandler;
import infra.web.handler.result.VoidReturnValueHandler;
import infra.web.view.ViewReturnValueHandler;
import infra.web.view.freemarker.FreeMarkerViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/12 19:34
 */
class ReturnValueHandlerManagerTests {

  @Test
  void returnValueHandlerManager() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    assertThat(manager.getMessageConverters()).isNotEmpty().hasSize(3);

    ReturnValueHandlerManager manager1 = new ReturnValueHandlerManager(manager.getMessageConverters());

    assertThat(manager1.getMessageConverters()).isNotEmpty().hasSize(3);
    assertThat(manager1).isNotEqualTo(manager);

    manager.setMessageConverters(List.of(new StringHttpMessageConverter(StandardCharsets.US_ASCII)));
    assertThat(manager.getMessageConverters()).isNotEmpty().hasSize(1);

    assertThat(manager.toString()).isNotEmpty();
    manager.hashCode();
  }

  @Test
  void addHandler() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    HttpStatusReturnValueHandler returnValueHandler = new HttpStatusReturnValueHandler();
    manager.addHandlers(returnValueHandler);
    assertThat(manager.getHandlers()).hasSize(1);

    HttpStatusReturnValueHandler highestValueHandler = new HttpStatusReturnValueHandler();
    manager.addHandlers(List.of(highestValueHandler));

    assertThat(manager.getHandlers()).hasSize(2);

    assertThat(getByReturnValue(manager, HttpStatus.OK)).isNotNull();
    assertThat(getByReturnValue(manager, "")).isNull();
    assertThat(getByReturnValue(manager, null)).isNull();

    assertThat(ReturnValueHandler.select(manager, "", null)).isNull();

    // getHandler(handler)

    HandlerMethod handler = mock(HandlerMethod.class);
    when(handler.isReturn(HttpStatus.class))
            .thenReturn(true);

    assertThat(ReturnValueHandler.select(manager, handler, null))
            .isNotNull();

    // obtainHandler

    assertThat(obtainHandler(manager, handler))
            .isEqualTo(returnValueHandler)
            .isNotNull();

    assertThat(obtainHandler(manager, handler))
            .isEqualTo(returnValueHandler)
            .isNotNull();

    assertThatThrownBy(() -> obtainHandler(manager, ""))
            .isInstanceOf(ReturnValueHandlerNotFoundException.class)
            .hasMessageStartingWith("No ReturnValueHandler for handler");

  }

  @Nullable
  public ReturnValueHandler getByReturnValue(ReturnValueHandlerManager manager, @Nullable Object returnValue) {
    for (ReturnValueHandler resolver : manager) {
      if (resolver.supportsReturnValue(returnValue)) {
        return resolver;
      }
    }
    return null;
  }

  public ReturnValueHandler obtainHandler(ReturnValueHandlerManager manager, Object handler) {
    ReturnValueHandler returnValueHandler = ReturnValueHandler.select(manager, handler, null);
    if (returnValueHandler == null) {
      throw new ReturnValueHandlerNotFoundException(handler);
    }
    return returnValueHandler;
  }

  @Test
  void contains() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    HttpStatusReturnValueHandler returnValueHandler = new HttpStatusReturnValueHandler();
    manager.addHandlers(returnValueHandler);

    assertThat(manager.contains(null)).isFalse();
    assertThat(manager.contains(String.class)).isFalse();
    assertThat(manager.contains(HttpStatusReturnValueHandler.class)).isTrue();

    assertThat(manager.get(HttpStatusReturnValueHandler.class)).isNotNull();
    assertThat(manager.get((Class<?>) null)).isNull();

  }

  @Test
  void requestResponseBodyAdvice() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    assertThat(manager.getRequestResponseBodyAdvice()).isEmpty();

    manager.addRequestResponseBodyAdvice(List.of(new Object()));
    assertThat(manager.getRequestResponseBodyAdvice()).isNotEmpty().hasSize(1);

    manager.setRequestResponseBodyAdvice(List.of(new Object()));
    assertThat(manager.getRequestResponseBodyAdvice()).isNotEmpty().hasSize(1);

  }

  @Test
  void contentNegotiationManager() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();
    manager.setContentNegotiationManager(contentNegotiationManager);
  }

  @Test
  void imageFormatName() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    assertThat(manager.getImageFormatName()).isEqualTo("png");

    manager.setImageFormatName("jpg");
    assertThat(manager.getImageFormatName()).isEqualTo("jpg");
  }

  @Test
  void removeIf() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    manager.setHandlers(List.of(new HttpStatusReturnValueHandler()));

    assertThat(manager.contains(HttpStatusReturnValueHandler.class)).isTrue();
    manager.removeIf(HttpStatusReturnValueHandler.class::isInstance);
    assertThat(manager.contains(HttpStatusReturnValueHandler.class)).isFalse();

    manager.trimToSize();
  }

  @Test
  void registerDefaultHandlers() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    manager.setViewReturnValueHandler(new ViewReturnValueHandler(new FreeMarkerViewResolver()));
    manager.registerDefaultHandlers();

    assertThat(manager.contains(VoidReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(HttpHeadersReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(HttpStatusReturnValueHandler.class)).isTrue();

    assertThat(manager.contains(ObjectHandlerMethodReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(VoidReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(HttpEntityMethodProcessor.class)).isFalse();
    assertThat(manager.contains(ResponseEntityReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(RequestResponseBodyMethodProcessor.class)).isTrue();
    assertThat(manager.contains(RequestResponseBodyMethodProcessor.class)).isTrue();

  }

  @Test
  void redirectModelManager() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();

    assertThat(manager.getRedirectModelManager())
            .isNull();

    SessionRedirectModelManager redirectModelManager = new SessionRedirectModelManager();
    manager.setRedirectModelManager(redirectModelManager);

    assertThat(manager.getRedirectModelManager())
            .isEqualTo(redirectModelManager);
  }

  @Test
  void objectHandler() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();

    assertThat(manager.getObjectHandler())
            .isNull();

    ObjectHandlerMethodReturnValueHandler objectHandler = new ObjectHandlerMethodReturnValueHandler(List.of());
    manager.setObjectHandler(objectHandler);

    assertThat(manager.getObjectHandler())
            .isEqualTo(objectHandler);
  }

  @Test
  void viewResolver() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    FreeMarkerViewResolver webViewResolver = new FreeMarkerViewResolver();
    manager.setViewResolver(webViewResolver);

    manager.registerDefaultHandlers();
    ViewReturnValueHandler viewReturnValueHandler = manager.get(ViewReturnValueHandler.class);

    assertThat(viewReturnValueHandler).isNotNull();
    assertThat(webViewResolver).isEqualTo(viewReturnValueHandler.getViewResolver());
    assertThat(viewReturnValueHandler.getLocaleResolver()).isNull();
  }

  @Test
  void managerEquals() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    assertThat(manager.equals(null)).isFalse();

    assertThat(manager.equals("")).isFalse();
    assertThat(manager.equals(manager)).isTrue();

  }

  @Test
  void asSelectable() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    SelectableReturnValueHandler selectable = manager.asSelectable();
    assertThat(selectable.getInternalHandlers()).isEqualTo(manager.getHandlers());
  }
}
