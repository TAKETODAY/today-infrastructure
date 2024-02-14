/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.context.event;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GenericApplicationListener}.
 *
 * @author Stephane Nicoll
 */
class GenericApplicationListenerTests extends AbstractApplicationEventListenerTests {

  @Test
  void forEventTypeWithStrictTypeMatching() {
    GenericApplicationListener listener = GenericApplicationListener
            .forEventType(StringEvent.class, event -> { });
    assertThat(listener.supportsEventType(ResolvableType.forClass(StringEvent.class))).isTrue();
  }

  @Test
  void forEventTypeWithSubClass() {
    GenericApplicationListener listener = GenericApplicationListener
            .forEventType(GenericTestEvent.class, event -> { });
    assertThat(listener.supportsEventType(ResolvableType.forClass(StringEvent.class))).isTrue();
  }

  @Test
  void forEventTypeWithSuperClass() {
    GenericApplicationListener listener = GenericApplicationListener
            .forEventType(StringEvent.class, event -> { });
    assertThat(listener.supportsEventType(ResolvableType.forClass(GenericTestEvent.class))).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void forEventTypeInvokesConsumer() {
    Consumer<StringEvent> consumer = mock(Consumer.class);
    GenericApplicationListener listener = GenericApplicationListener
            .forEventType(StringEvent.class, consumer);
    StringEvent event = new StringEvent(this, "one");
    StringEvent event2 = new StringEvent(this, "two");
    listener.onApplicationEvent(event);
    listener.onApplicationEvent(event2);
    InOrder ordered = inOrder(consumer);
    ordered.verify(consumer).accept(event);
    ordered.verify(consumer).accept(event2);
  }

}
