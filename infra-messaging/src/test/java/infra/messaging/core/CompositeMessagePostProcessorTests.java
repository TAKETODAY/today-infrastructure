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

package infra.messaging.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.messaging.Message;
import infra.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeMessagePostProcessorTests {

  @Test
  void appliesProcessorsInOrderAndReturnsLastMessage() {
    Message<String> first = new GenericMessage<>("first");
    Message<String> second = new GenericMessage<>("second");
    Message<String> third = new GenericMessage<>("third");
    MessagePostProcessor processor = new CompositeMessagePostProcessor(List.of(
            message -> {
              assertThat(message).isSameAs(first);
              return second;
            },
            message -> {
              assertThat(message).isSameAs(second);
              return third;
            }));

    assertThat(processor.postProcessMessage(first)).isSameAs(third);
  }

  @Test
  void emptyCompositeReturnsInput() {
    Message<String> message = new GenericMessage<>("payload");
    assertThat(new CompositeMessagePostProcessor(List.of()).postProcessMessage(message)).isSameAs(message);
  }

}
