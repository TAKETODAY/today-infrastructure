/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.ResolvableTypeProvider;

/**
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public abstract class AbstractApplicationEventListenerTests {

  protected ResolvableType getGenericApplicationEventType(String fieldName) {
    try {
      return ResolvableType.forField(TestEvents.class.getField(fieldName));
    }
    catch (NoSuchFieldException ex) {
      throw new IllegalStateException("No such field on Events '" + fieldName + "'");
    }
  }

  protected static class GenericTestEvent<T> extends ApplicationEvent {

    private final T payload;

    public GenericTestEvent(Object source, T payload) {
      super(source);
      this.payload = payload;
    }

    public T getPayload() {
      return this.payload;
    }
  }

  protected static class SmartGenericTestEvent<T> extends GenericTestEvent<T> implements ResolvableTypeProvider {

    private final ResolvableType resolvableType;

    public SmartGenericTestEvent(Object source, T payload) {
      super(source, payload);
      this.resolvableType = ResolvableType.forClassWithGenerics(
              getClass(), payload.getClass());
    }

    @Override
    public ResolvableType getResolvableType() {
      return this.resolvableType;
    }
  }

  protected static class StringEvent extends GenericTestEvent<String> {

    public StringEvent(Object source, String payload) {
      super(source, payload);
    }
  }

  protected static class LongEvent extends GenericTestEvent<Long> {

    public LongEvent(Object source, Long payload) {
      super(source, payload);
    }
  }

  protected <T> GenericTestEvent<T> createGenericTestEvent(T payload) {
    return new GenericTestEvent<>(this, payload);
  }

  static class GenericEventListener implements ApplicationListener<GenericTestEvent<?>> {
    @Override
    public void onApplicationEvent(GenericTestEvent<?> event) {
    }
  }

  static class ObjectEventListener implements ApplicationListener<GenericTestEvent<Object>> {
    @Override
    public void onApplicationEvent(GenericTestEvent<Object> event) {
    }
  }

  static class UpperBoundEventListener
          implements ApplicationListener<GenericTestEvent<? extends RuntimeException>> {

    @Override
    public void onApplicationEvent(GenericTestEvent<? extends RuntimeException> event) {
    }
  }

  static class StringEventListener implements ApplicationListener<GenericTestEvent<String>> {

    @Override
    public void onApplicationEvent(GenericTestEvent<String> event) {
    }
  }

  @SuppressWarnings("rawtypes")
  static class RawApplicationListener implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
  }

  static class TestEvents {

    public GenericTestEvent<?> wildcardEvent;

  }

}
