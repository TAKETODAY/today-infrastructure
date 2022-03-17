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

package cn.taketoday.context.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class GenericApplicationListenerAdapterTests extends AbstractApplicationEventListenerTests {

  @Test
  public void supportsEventTypeWithSmartApplicationListener() {
    SmartApplicationListener smartListener = mock(SmartApplicationListener.class);
    GenericApplicationListenerAdapter listener = new GenericApplicationListenerAdapter(smartListener);
    ResolvableType type = ResolvableType.fromClass(ApplicationEvent.class);
    listener.supportsEventType(type);
    verify(smartListener, times(1)).supportsEventType(ApplicationEvent.class);
  }

  @Test
  public void supportsSourceTypeWithSmartApplicationListener() {
    SmartApplicationListener smartListener = mock(SmartApplicationListener.class);
    GenericApplicationListenerAdapter listener = new GenericApplicationListenerAdapter(smartListener);
    listener.supportsSourceType(Object.class);
    verify(smartListener, times(1)).supportsSourceType(Object.class);
  }

  @Test
  public void genericListenerStrictType() {
    supportsEventType(true, StringEventListener.class, ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class));
  }

  @Test // Demonstrates we can't inject that event because the generic type is lost
  public void genericListenerStrictTypeTypeErasure() {
    GenericTestEvent<String> stringEvent = createGenericTestEvent("test");
    ResolvableType eventType = ResolvableType.fromType(stringEvent.getClass());
    supportsEventType(false, StringEventListener.class, eventType);
  }

  @Test // But it works if we specify the type properly
  public void genericListenerStrictTypeAndResolvableType() {
    ResolvableType eventType = ResolvableType
            .fromClassWithGenerics(GenericTestEvent.class, String.class);
    supportsEventType(true, StringEventListener.class, eventType);
  }

  @Test // or if the event provides its precise type
  public void genericListenerStrictTypeAndResolvableTypeProvider() {
    ResolvableType eventType = new SmartGenericTestEvent<>(this, "foo").getResolvableType();
    supportsEventType(true, StringEventListener.class, eventType);
  }

  @Test // Demonstrates it works if we actually use the subtype
  public void genericListenerStrictTypeEventSubType() {
    StringEvent stringEvent = new StringEvent(this, "test");
    ResolvableType eventType = ResolvableType.fromType(stringEvent.getClass());
    supportsEventType(true, StringEventListener.class, eventType);
  }

  @Test
  public void genericListenerStrictTypeNotMatching() {
    supportsEventType(false, StringEventListener.class, ResolvableType.fromClassWithGenerics(GenericTestEvent.class, Long.class));
  }

  @Test
  public void genericListenerStrictTypeEventSubTypeNotMatching() {
    LongEvent stringEvent = new LongEvent(this, 123L);
    ResolvableType eventType = ResolvableType.fromType(stringEvent.getClass());
    supportsEventType(false, StringEventListener.class, eventType);
  }

  @Test
  public void genericListenerStrictTypeNotMatchTypeErasure() {
    GenericTestEvent<Long> longEvent = createGenericTestEvent(123L);
    ResolvableType eventType = ResolvableType.fromType(longEvent.getClass());
    supportsEventType(false, StringEventListener.class, eventType);
  }

  @Test
  public void genericListenerStrictTypeSubClass() {
    supportsEventType(false, ObjectEventListener.class, ResolvableType.fromClassWithGenerics(GenericTestEvent.class, Long.class));
  }

  @Test
  public void genericListenerUpperBoundType() {
    supportsEventType(true, UpperBoundEventListener.class,
            ResolvableType.fromClassWithGenerics(GenericTestEvent.class, IllegalStateException.class));
  }

  @Test
  public void genericListenerUpperBoundTypeNotMatching() {
    supportsEventType(false, UpperBoundEventListener.class,
            ResolvableType.fromClassWithGenerics(GenericTestEvent.class, IOException.class));
  }

  @Test
  public void genericListenerWildcardType() {
    supportsEventType(true, GenericEventListener.class,
            ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class));
  }

  @Test  // Demonstrates we cant inject that event because the listener has a wildcard
  public void genericListenerWildcardTypeTypeErasure() {
    GenericTestEvent<String> stringEvent = createGenericTestEvent("test");
    ResolvableType eventType = ResolvableType.fromType(stringEvent.getClass());
    supportsEventType(true, GenericEventListener.class, eventType);
  }

  @Test
  public void genericListenerRawType() {
    supportsEventType(true, RawApplicationListener.class,
            ResolvableType.fromClassWithGenerics(GenericTestEvent.class, String.class));
  }

  @Test  // Demonstrates we cant inject that event because the listener has a raw type
  public void genericListenerRawTypeTypeErasure() {
    GenericTestEvent<String> stringEvent = createGenericTestEvent("test");
    ResolvableType eventType = ResolvableType.fromType(stringEvent.getClass());
    supportsEventType(true, RawApplicationListener.class, eventType);
  }

  @SuppressWarnings("rawtypes")
  private void supportsEventType(
          boolean match, Class<? extends ApplicationListener> listenerType, ResolvableType eventType) {

    ApplicationListener<?> listener = mock(listenerType);
    GenericApplicationListenerAdapter adapter = new GenericApplicationListenerAdapter(listener);
    assertThat(adapter.supportsEventType(eventType)).as("Wrong match for event '" + eventType + "' on " + listenerType.getClass().getName()).isEqualTo(match);
  }

}
