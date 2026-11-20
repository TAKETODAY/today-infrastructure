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

import infra.web.HttpContext;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.function.ServerResponse;
import infra.web.handler.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbstractHandlerMethodExceptionHandlerTests {

  private final TestExceptionHandler handler = new TestExceptionHandler();

  private final HttpContext context = mock(HttpContext.class);

  private final HandlerFunction<ServerResponse> function = request -> ServerResponse.ok().build();

  @Test
  void handlerFunctionIsNotSupportedByDefault() {
    assertThat(handler.shouldApplyTo(context, function)).isFalse();
  }

  @Test
  void globalHandlersAloneDoNotEnableHandlerFunctions() {
    handler.globalHandlers = true;

    assertThat(handler.shouldApplyTo(context, function)).isFalse();
  }

  @Test
  void explicitMappingAloneDoesNotEnableHandlerFunctions() {
    handler.setMappedHandlerPredicate(candidate -> candidate == function);

    assertThat(handler.shouldApplyTo(context, function)).isFalse();
  }

  @Test
  void globalHandlersRespectExplicitHandlerMapping() {
    handler.globalHandlers = true;
    handler.setMappedHandlerPredicate(candidate -> candidate == function);
    HandlerFunction<ServerResponse> other = request -> ServerResponse.ok().build();

    assertThat(handler.shouldApplyTo(context, function)).isTrue();
    assertThat(handler.shouldApplyTo(context, other)).isFalse();
  }

  @Test
  void handlerMethodMappingUsesControllerBean() throws Exception {
    var controller = new Controller();
    handler.setMappedHandlerPredicate(candidate -> candidate == controller);

    assertThat(handler.shouldApplyTo(context, new HandlerMethod(controller, "handle"))).isTrue();
    assertThat(handler.shouldApplyTo(context, new HandlerMethod(new Controller(), "handle"))).isFalse();
  }

  private static class TestExceptionHandler extends AbstractHandlerMethodExceptionHandler {

    boolean globalHandlers;

    @Override
    protected boolean hasGlobalExceptionHandlers() {
      return globalHandlers;
    }

    @Override
    protected @Nullable Object handleInternal(HttpContext request, @Nullable HandlerMethod handlerMethod, Throwable ex) {
      return null;
    }
  }

  private static class Controller {

    public void handle() { }
  }

}
