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

package infra.app.availability;

import infra.context.ApplicationContext;
import infra.context.ApplicationEvent;
import infra.context.ApplicationEventPublisher;
import infra.context.PayloadApplicationEvent;
import infra.core.ResolvableType;
import infra.lang.Assert;

/**
 * {@link ApplicationEvent} sent when the {@link AvailabilityState} of the application
 * changes.
 * <p>
 * Any application component can send such events to update the state of the application.
 *
 * @param <S> the availability state type
 * @author Brian Clozel
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class AvailabilityChangeEvent<S extends AvailabilityState> extends PayloadApplicationEvent<S> {

  /**
   * Create a new {@link AvailabilityChangeEvent} instance.
   *
   * @param source the source of the event
   * @param state the availability state (never {@code null})
   */
  public AvailabilityChangeEvent(Object source, S state) {
    super(source, state);
  }

  /**
   * Return the changed availability state.
   *
   * @return the availability state
   */
  public S getState() {
    return getPayload();
  }

  @Override
  public ResolvableType getResolvableType() {
    return ResolvableType.forClassWithGenerics(getClass(), getStateType());
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
    Assert.notNull(context, "Context is required");
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
    Assert.notNull(publisher, "Publisher is required");
    publisher.publishEvent(new AvailabilityChangeEvent<>(source, state));
  }

}
