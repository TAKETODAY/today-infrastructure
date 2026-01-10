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

package infra.web.handler;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.stereotype.Component;
import infra.web.DispatcherHandler;
import infra.web.HandlerAdapter;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerMapping;
import infra.web.NotFoundHandler;
import infra.web.RequestCompletedListener;
import infra.web.async.WebAsyncManagerFactory;
import infra.web.bind.resolver.ParameterResolvingRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/11 17:53
 */
class DispatcherHandlerTests {

  @Configuration
  static class Config {

    @Component
    ParameterResolvingRegistry parameterResolvingRegistry() {
      return new ParameterResolvingRegistry();
    }

    @Component
    ReturnValueHandlerManager returnValueHandlerManager() {
      return new ReturnValueHandlerManager();
    }

  }

  @Test
  void init() {

    var context = new AnnotationConfigApplicationContext(Config.class);

    DispatcherHandler handler = new DispatcherHandler(context);
    assertThat(handler).extracting("handlerMapping").isNull();
    assertThat(handler).extracting("handlerAdapter").isNull();
    assertThat(handler).extracting("returnValueHandler").isNull();
    assertThat(handler).extracting("exceptionHandler").isNull();
    assertThat(handler).extracting("notFoundHandler").isNull();
    assertThat(handler).extracting("webAsyncManagerFactory").isNull();

    handler.start();

    assertThat(handler).extracting("handlerMapping").isNotNull();
    assertThat(handler).extracting("handlerAdapter").isNotNull();
    assertThat(handler).extracting("exceptionHandler").isNotNull();
    assertThat(handler).extracting("returnValueHandler").isNotNull();
    assertThat(handler).extracting("notFoundHandler").isNotNull();
    assertThat(handler).extracting("webAsyncManagerFactory").isNotNull();
  }

  @Test
  void constructorWithApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);

    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    assertThat(dispatcherHandler).isNotNull();
  }

  @Test
  void setHandlerMapping() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    HandlerMapping mockHandlerMapping = mock(HandlerMapping.class);
    dispatcherHandler.setHandlerMapping(mockHandlerMapping);

    // Using reflection to access private field
    assertThat(dispatcherHandler).extracting("handlerMapping").isEqualTo(mockHandlerMapping);
  }

  @Test
  void setHandlerAdapter() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    HandlerAdapter mockHandlerAdapter = mock(HandlerAdapter.class);
    dispatcherHandler.setHandlerAdapter(mockHandlerAdapter);

    assertThat(dispatcherHandler).extracting("handlerAdapter").isEqualTo(mockHandlerAdapter);
  }

  @Test
  void setExceptionHandler() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    HandlerExceptionHandler mockExceptionHandler = mock(HandlerExceptionHandler.class);
    dispatcherHandler.setExceptionHandler(mockExceptionHandler);

    assertThat(dispatcherHandler).extracting("exceptionHandler").isEqualTo(mockExceptionHandler);
  }

  @Test
  void setReturnValueHandler() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    ReturnValueHandlerManager mockReturnValueHandler = mock(ReturnValueHandlerManager.class);
    dispatcherHandler.setReturnValueHandler(mockReturnValueHandler);

    assertThat(dispatcherHandler).extracting("returnValueHandler").isEqualTo(mockReturnValueHandler);
  }

  @Test
  void setNotFoundHandler() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    NotFoundHandler mockNotFoundHandler = mock(NotFoundHandler.class);
    dispatcherHandler.setNotFoundHandler(mockNotFoundHandler);

    assertThat(dispatcherHandler).extracting("notFoundHandler").isEqualTo(mockNotFoundHandler);
  }

  @Test
  void setWebAsyncManagerFactory() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    WebAsyncManagerFactory mockFactory = mock(WebAsyncManagerFactory.class);
    dispatcherHandler.setWebAsyncManagerFactory(mockFactory);

    assertThat(dispatcherHandler).extracting("webAsyncManagerFactory").isEqualTo(mockFactory);
  }

  @Test
  void setDetectAllHandlerMapping() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    dispatcherHandler.setDetectAllHandlerMapping(false);
    assertThat(dispatcherHandler).extracting("detectAllHandlerMapping").isEqualTo(false);
  }

  @Test
  void setDetectAllHandlerAdapters() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    dispatcherHandler.setDetectAllHandlerAdapters(false);
    assertThat(dispatcherHandler).extracting("detectAllHandlerAdapters").isEqualTo(false);
  }

  @Test
  void setDetectAllHandlerExceptionHandlers() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    dispatcherHandler.setDetectAllHandlerExceptionHandlers(false);
    assertThat(dispatcherHandler).extracting("detectAllHandlerExceptionHandlers").isEqualTo(false);
  }

  @Test
  void setThrowExceptionIfNoHandlerFound() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    dispatcherHandler.setThrowExceptionIfNoHandlerFound(true);
    assertThat(dispatcherHandler).extracting("throwExceptionIfNoHandlerFound").isEqualTo(true);
  }

  @Test
  void addRequestCompletedActionsWithArray() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    RequestCompletedListener mockListener1 = mock(RequestCompletedListener.class);
    RequestCompletedListener mockListener2 = mock(RequestCompletedListener.class);

    dispatcherHandler.addRequestCompletedActions(mockListener1, mockListener2);
    // Simply testing that method exists and is callable
  }

  @Test
  void addRequestCompletedActionsWithCollection() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    RequestCompletedListener mockListener1 = mock(RequestCompletedListener.class);
    RequestCompletedListener mockListener2 = mock(RequestCompletedListener.class);
    Collection<RequestCompletedListener> listeners = List.of(mockListener1, mockListener2);

    dispatcherHandler.addRequestCompletedActions(listeners);
  }

  @Test
  void setRequestCompletedActions() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DispatcherHandlerTests.Config.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(context);

    RequestCompletedListener mockListener1 = mock(RequestCompletedListener.class);
    RequestCompletedListener mockListener2 = mock(RequestCompletedListener.class);
    Collection<RequestCompletedListener> listeners = List.of(mockListener1, mockListener2);

    dispatcherHandler.setRequestCompletedActions(listeners);
  }

}