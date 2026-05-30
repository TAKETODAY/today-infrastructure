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

package infra.messaging.handler.annotation.reactive;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import infra.core.MethodParameter;
import infra.core.conversion.support.DefaultConversionService;
import infra.messaging.Message;
import infra.messaging.MessageHandlingException;
import infra.messaging.handler.annotation.DestinationVariable;
import infra.messaging.handler.invocation.ResolvableMethod;
import infra.messaging.support.MessageBuilder;

import static infra.messaging.handler.annotation.MessagingPredicates.destinationVar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link DestinationVariableMethodArgumentResolver} tests.
 *
 * @author Rossen Stoyanchev
 */
class DestinationVariableMethodArgumentResolverTests {

  private final DestinationVariableMethodArgumentResolver resolver =
          new DestinationVariableMethodArgumentResolver(new DefaultConversionService());

  private final ResolvableMethod resolvable =
          ResolvableMethod.on(getClass()).named("handleMessage").build();

  @Test
  void supportsParameter() {
    assertThat(resolver.supportsParameter(this.resolvable.annot(destinationVar().noValue()).arg())).isTrue();
    assertThat(resolver.supportsParameter(this.resolvable.annotNotPresent(DestinationVariable.class).arg())).isFalse();
  }

  @Test
  void resolveArgument() {

    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    vars.put("name", "value");

    Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeader(
            DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars).build();

    Object result = resolveArgument(this.resolvable.annot(destinationVar().noValue()).arg(), message);
    assertThat(result).isEqualTo("bar");

    result = resolveArgument(this.resolvable.annot(destinationVar("name")).arg(), message);
    assertThat(result).isEqualTo("value");
  }

  @Test
  void resolveArgumentNotFound() {
    Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
    assertThatExceptionOfType(MessageHandlingException.class).isThrownBy(() ->
            resolveArgument(this.resolvable.annot(destinationVar().noValue()).arg(), message));
  }

  @SuppressWarnings({ "unchecked", "ConstantConditions" })
  private <T> T resolveArgument(MethodParameter param, Message<?> message) {
    return (T) this.resolver.resolveArgument(param, message).block(Duration.ofSeconds(5));
  }

  @SuppressWarnings("unused")
  private void handleMessage(
          @DestinationVariable String foo,
          @DestinationVariable(value = "name") String param1,
          String param3) {
  }

}
