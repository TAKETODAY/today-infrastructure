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

package infra.messaging.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CompositeMessageConverterTests {

  private final MessageConverter ordinary = mock();

  private final SmartMessageConverter smart = mock();

  private final MessageConverter fallback = mock();

  private final CompositeMessageConverter converter = new CompositeMessageConverter(List.of(this.ordinary, this.smart, this.fallback));

  private final GenericMessage<String> message = new GenericMessage<>("42");

  @Test
  void passesTheHintOnlyToSmartConvertersAndStopsAtTheFirstResult() {
    Object hint = new Object();
    var headers = this.message.getHeaders();
    given(this.smart.fromMessage(this.message, Integer.class, hint)).willReturn(42);
    given(this.smart.toMessage(42, headers, hint)).willAnswer(invocation -> this.message);

    assertThat(this.converter.fromMessage(this.message, Integer.class, hint)).isEqualTo(42);
    assertThat(this.converter.toMessage(42, headers, hint)).isSameAs(this.message);
    verify(this.ordinary).fromMessage(this.message, Integer.class);
    verify(this.ordinary).toMessage(42, headers);
    verifyNoInteractions(this.fallback);
  }

  @Test
  void usesHintlessOverloadsWhenNoHintIsSupplied() {
    var headers = this.message.getHeaders();
    given(this.smart.fromMessage(this.message, Integer.class)).willReturn(42);
    given(this.smart.toMessage(42, headers)).willAnswer(invocation -> this.message);

    assertThat(this.converter.fromMessage(this.message, Integer.class)).isEqualTo(42);
    assertThat(this.converter.toMessage(42, headers)).isSameAs(this.message);
    verifyNoInteractions(this.fallback);
  }

  @Test
  void returnsNullWhenNoDelegateCanConvertInEitherDirection() {
    Object hint = new Object();
    assertThat(this.converter.fromMessage(this.message, Integer.class)).isNull();
    assertThat(this.converter.fromMessage(this.message, Integer.class, hint)).isNull();
    assertThat(this.converter.toMessage(42, null)).isNull();
    assertThat(this.converter.toMessage(42, null, hint)).isNull();
    assertThatIllegalArgumentException().isThrownBy(() -> new CompositeMessageConverter(List.of()))
            .withMessageContaining("Converters must not be empty");
  }

}
