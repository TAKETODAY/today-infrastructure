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

package infra.messaging.handler;

import org.junit.jupiter.api.Test;

import infra.messaging.Message;
import infra.messaging.simp.SimpMessageType;
import infra.messaging.simp.SimpMessageTypeMessageCondition;
import infra.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class CompositeMessageConditionTests {

  @Test
  void combinesMatchesAndComparesConditions() {
    CompositeMessageCondition condition = condition(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE);
    CompositeMessageCondition other = condition(SimpMessageType.SUBSCRIBE, SimpMessageType.MESSAGE);
    Message<?> message = message(SimpMessageType.MESSAGE);

    assertThat(condition.getMessageConditions()).hasSize(2);
    assertThat(condition.getCondition(SimpMessageTypeMessageCondition.class).getMessageType())
            .isEqualTo(SimpMessageType.MESSAGE);
    assertThat(condition.combine(other).getMessageConditions()).extracting("messageType")
            .containsExactly(SimpMessageType.SUBSCRIBE, SimpMessageType.MESSAGE);
    assertThat(condition.getMatchingCondition(message)).isNull();
    assertThat(condition.compareTo(other, message)).isEqualTo(-1);
    assertThat(condition.toString()).isEqualTo("{[MESSAGE],[SUBSCRIBE]}");
    assertThat(condition).isEqualTo(condition(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE));
    assertThat(condition).isNotEqualTo(other);
    assertThat(condition).isNotEqualTo(null);
    assertThat(condition).hasSameHashCodeAs(condition(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE));
  }

  @Test
  void rejectsEmptyUnknownAndMismatchedConditions() {
    assertThatIllegalArgumentException().isThrownBy(() -> new CompositeMessageCondition());
    CompositeMessageCondition condition = condition(SimpMessageType.MESSAGE);
    assertThatIllegalStateException().isThrownBy(() -> condition.getCondition(DummyCondition.class));
    CompositeMessageCondition mismatched = new CompositeMessageCondition(new DummyCondition());
    assertThatIllegalArgumentException().isThrownBy(() -> condition.combine(mismatched));
    assertThatIllegalArgumentException().isThrownBy(() -> condition.compareTo(mismatched, message(SimpMessageType.MESSAGE)));
  }

  private CompositeMessageCondition condition(SimpMessageType... types) {
    SimpMessageTypeMessageCondition[] conditions = new SimpMessageTypeMessageCondition[types.length];
    for (int i = 0; i < types.length; i++) {
      conditions[i] = new SimpMessageTypeMessageCondition(types[i]);
    }
    return new CompositeMessageCondition(conditions);
  }

  private Message<?> message(SimpMessageType type) {
    return MessageBuilder.withPayload("payload")
            .setHeader("simpMessageType", type)
            .build();
  }

  private static final class DummyCondition extends AbstractMessageCondition<DummyCondition>
          implements MessageCondition<DummyCondition> {

    @Override
    protected java.util.Collection<?> getContent() {
      return java.util.List.of("dummy");
    }

    @Override
    protected String getToStringInfix() {
      return " && ";
    }

    @Override
    public DummyCondition combine(DummyCondition other) {
      return this;
    }

    @Override
    public DummyCondition getMatchingCondition(Message<?> message) {
      return this;
    }

    @Override
    public int compareTo(DummyCondition other, Message<?> message) {
      return 0;
    }
  }

}
