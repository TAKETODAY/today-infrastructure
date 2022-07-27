/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.bind.resolver.HttpEntityMethodProcessor;
import cn.taketoday.web.bind.resolver.RequestResponseBodyMethodProcessor;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.result.HttpHeadersReturnValueHandler;
import cn.taketoday.web.handler.result.HttpStatusReturnValueHandler;
import cn.taketoday.web.handler.result.ModelAndViewReturnValueHandler;
import cn.taketoday.web.handler.result.ObjectHandlerMethodReturnValueHandler;
import cn.taketoday.web.handler.result.VoidReturnValueHandler;
import cn.taketoday.web.view.SessionRedirectModelManager;
import cn.taketoday.web.view.ViewReturnValueHandler;
import cn.taketoday.web.view.freemarker.FreeMarkerViewResolver;

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

    assertThat(manager.getByReturnValue(HttpStatus.OK)).isNotNull();

    assertThat(manager.getByReturnValue("")).isNull();
    assertThat(manager.getByReturnValue(null)).isNull();

    assertThat(manager.getHandler("")).isNull();

    // getHandler(handler)

    HandlerMethod handler = mock(HandlerMethod.class);
    when(handler.isReturn(HttpStatus.class))
            .thenReturn(true);

    ActionMappingAnnotationHandler annotationHandler = new ActionMappingAnnotationHandler(handler, null, Object.class) {
      @Override
      public Object getHandlerObject() {
        return null;
      }
    };

    assertThat(manager.getHandler(annotationHandler))
            .isNotNull();

    // obtainHandler

    assertThat(manager.obtainHandler(annotationHandler))
            .isEqualTo(returnValueHandler)
            .isNotNull();

    assertThat(manager.obtainHandler(annotationHandler))
            .isEqualTo(returnValueHandler)
            .isNotNull();

    assertThatThrownBy(() -> manager.obtainHandler(""))
            .isInstanceOf(ReturnValueHandlerNotFoundException.class)
            .hasMessageStartingWith("No ReturnValueHandler for handler");

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

    assertThat(manager.contains(ModelAndViewReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(VoidReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(HttpHeadersReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(HttpStatusReturnValueHandler.class)).isTrue();

    assertThat(manager.contains(ObjectHandlerMethodReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(VoidReturnValueHandler.class)).isTrue();
    assertThat(manager.contains(HttpEntityMethodProcessor.class)).isTrue();
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
    assertThat(viewReturnValueHandler.getModelManager()).isNull();
    assertThat(webViewResolver).isEqualTo(viewReturnValueHandler.getViewResolver());
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
