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
package cn.taketoday.framework.availability;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.event.ApplicationEvent;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;

/**
 * {@link ApplicationEvent} sent when the {@link AvailabilityState} of the application
 * changes.
 * <p>
 * Any application component can send such events to update the state of the application.
 *
 * @param <S> the availability state type
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 4.0
 */
public class AvailabilityChangeEvent<S extends AvailabilityState> extends ApplicationEvent {

  private final S state;

  /**
   * Create a new {@link AvailabilityChangeEvent} instance.
   *
   * @param source the source of the event
   * @param state the availability state (never {@code null})
   */
  public AvailabilityChangeEvent(Object source, S state) {
    super(source);
    Assert.notNull(state, "state must not be null");
    this.state = state;
  }

  /**
   * Return the changed availability state.
   *
   * @return the availability state
   */
  public S getState() {
    return state;
  }

  public ResolvableType getResolvableType() {
    return ResolvableType.fromClassWithGenerics(getClass(), getStateType());
  }

  private Class<?> getStateType() {
    S state = getState();
    if (state instanceof Enum) {
      return ((Enum<?>) state).getDeclaringClass();
    }
    return state.getClass();
  }

  /**
   * Convenience method that can be used to publish an {@link AvailabilityChangeEvent}
   * to the given application context.
   *
   * @param <S> the availability state type
   * @param context the context used to publish the event
   * @param state the changed availability state
   */
  public static <S extends AvailabilityState> void publish(ApplicationContext context, S state) {
    Assert.notNull(context, "Context must not be null");
    publish(context, context, state);
  }

  /**
   * Convenience method that can be used to publish an {@link AvailabilityChangeEvent}
   * to the given application context.
   *
   * @param <S> the availability state type
   * @param publisher the publisher used to publish the event
   * @param source the source of the event
   * @param state the changed availability state
   */
  public static <S extends AvailabilityState> void publish(
          ApplicationEventPublisher publisher, Object source, S state) {
    Assert.notNull(publisher, "Publisher must not be null");
    publisher.publishEvent(new AvailabilityChangeEvent<>(source, state));
  }

}
