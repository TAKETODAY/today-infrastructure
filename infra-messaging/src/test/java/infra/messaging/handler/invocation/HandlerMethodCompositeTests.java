/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.messaging.handler.invocation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import infra.core.MethodParameter;
import infra.logging.Logger;
import infra.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class HandlerMethodCompositeTests {

  private final GenericMessage<String> message = new GenericMessage<>("payload");

  @Test
  void cachesTheFirstSupportingArgumentResolverUntilTheCompositeIsCleared() throws Exception {
    var type = parameter(0);
    HandlerMethodArgumentResolver unsupported = mock();
    HandlerMethodArgumentResolver first = mock();
    HandlerMethodArgumentResolver next = mock();
    given(first.supportsParameter(type)).willReturn(true);
    given(first.resolveArgument(type, this.message)).willReturn("first");
    var composite = new HandlerMethodArgumentResolverComposite().addResolvers(unsupported, first).addResolver(next);

    assertThat(composite.getResolvers()).containsExactly(unsupported, first, next);
    assertThat(composite.supportsParameter(type)).isTrue();
    assertThat(composite.resolveArgument(type, this.message)).isEqualTo("first");
    verify(first).supportsParameter(type);
    verifyNoInteractions(next);

    composite.clear();
    assertThat(composite.supportsParameter(type)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> composite.resolveArgument(type, this.message))
            .withMessageContaining("Unsupported parameter type [java.lang.String]");
  }

  @Test
  void handlesOrdinaryReturnsWithoutTreatingThemAsAsync() throws Exception {
    var type = parameter(-1);
    HandlerMethodReturnValueHandler handler = mock();
    given(handler.supportsReturnType(type)).willReturn(true);
    Logger logger = mock();
    given(logger.isTraceEnabled()).willReturn(true);
    var composite = new HandlerMethodReturnValueHandlerComposite().addHandlers(List.of(handler));
    composite.setLogger(logger);

    assertThat(composite.getLogger()).isSameAs(logger);
    assertThat(composite.getReturnValueHandlers()).containsExactly(handler);
    composite.handleReturnValue("response", type, this.message);
    verify(handler).handleReturnValue("response", type, this.message);
    assertThat(composite.isAsyncReturnValue("response", type)).isFalse();
    assertThat(composite.toCompletableFuture("response", type)).isNull();

    composite.clear();
    assertThat(composite.supportsReturnType(type)).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> composite.handleReturnValue("response", type, this.message))
            .withMessageContaining("No handler for return value type");
  }

  @Test
  void delegatesAsyncDetectionAndFutureAdaptationToTheSelectedHandler() throws Exception {
    var type = parameter(-1);
    AsyncHandlerMethodReturnValueHandler handler = mock();
    var stage = CompletableFuture.completedFuture("response");
    given(handler.supportsReturnType(type)).willReturn(true);
    given(handler.isAsyncReturnValue(stage, type)).willReturn(true);
    given(handler.toCompletableFuture(stage, type)).willAnswer(invocation -> stage);
    var composite = new HandlerMethodReturnValueHandlerComposite().addHandlers(null).addHandler(handler);

    assertThat(composite.isAsyncReturnValue(stage, type)).isTrue();
    assertThat(composite.isAsyncReturnValue("response", type)).isFalse();
    assertThat(composite.toCompletableFuture(stage, type)).isSameAs(stage);
  }

  private static MethodParameter parameter(int index) throws NoSuchMethodException {
    return new MethodParameter(HandlerMethodCompositeTests.class.getDeclaredMethod("echo", String.class), index);
  }

  private String echo(String value) {
    return value;
  }

}
