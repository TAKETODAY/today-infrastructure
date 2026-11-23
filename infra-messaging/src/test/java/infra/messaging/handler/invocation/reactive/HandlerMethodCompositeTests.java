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

package infra.messaging.handler.invocation.reactive;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import infra.core.MethodParameter;
import infra.messaging.support.GenericMessage;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    given(first.resolveArgument(type, this.message)).willReturn(Mono.just("first"));
    var composite = new HandlerMethodArgumentResolverComposite().addResolvers(unsupported, first).addResolver(next);

    assertThat(composite.getResolvers()).containsExactly(unsupported, first, next);
    assertThat(composite.supportsParameter(type)).isTrue();
    StepVerifier.create(composite.resolveArgument(type, this.message)).expectNext("first").expectComplete().verify(Duration.ofSeconds(1));
    verify(first).supportsParameter(type);
    verifyNoInteractions(next);

    composite.clear();
    assertThat(composite.supportsParameter(type)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> composite.resolveArgument(type, this.message))
            .withMessageContaining("Unsupported parameter type [java.lang.String]");
  }

  @Test
  void selectsTheFirstSupportingReturnHandlerAndPropagatesItsFailureSignal() throws Exception {
    var type = parameter(-1);
    HandlerMethodReturnValueHandler unsupported = mock();
    HandlerMethodReturnValueHandler first = mock();
    HandlerMethodReturnValueHandler next = mock();
    var failure = new IllegalStateException("handler failed");
    given(first.supportsReturnType(type)).willReturn(true);
    given(first.handleReturnValue("response", type, this.message)).willReturn(Mono.error(failure));
    var composite = new HandlerMethodReturnValueHandlerComposite().addHandlers(null)
            .addHandler(unsupported).addHandlers(List.of(first, next));

    assertThat(composite.getReturnValueHandlers()).containsExactly(unsupported, first, next);
    assertThat(composite.supportsReturnType(type)).isTrue();
    StepVerifier.create(composite.handleReturnValue("response", type, this.message))
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure)).verify(Duration.ofSeconds(1));
    verifyNoInteractions(next);

    composite.clear();
    assertThat(composite.supportsReturnType(type)).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> composite.handleReturnValue("response", type, this.message))
            .withMessageContaining("No handler for return value type");
  }

  private static MethodParameter parameter(int index) throws NoSuchMethodException {
    return new MethodParameter(HandlerMethodCompositeTests.class.getDeclaredMethod("echo", String.class), index);
  }

  private String echo(String value) {
    return value;
  }

}
