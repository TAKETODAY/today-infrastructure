/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.availability;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

import infra.context.ApplicationEventPublisher;
import infra.context.ApplicationListener;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Bean that provides an {@link ApplicationAvailability} implementation by listening for
 * {@link AvailabilityChangeEvent change events}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationAvailability
 * @since 4.0
 */
public class ApplicationAvailabilityBean implements ApplicationAvailability, ApplicationListener<AvailabilityChangeEvent<?>> {

  private final ConcurrentHashMap<Class<? extends AvailabilityState>, AvailabilityChangeEvent<?>>
          events = new ConcurrentHashMap<>();

  private final Logger logger;

  public ApplicationAvailabilityBean() {
    this(LoggerFactory.getLogger(ApplicationAvailabilityBean.class));
  }

  ApplicationAvailabilityBean(Logger logger) {
    this.logger = logger;
  }

  @Override
  public <S extends AvailabilityState> S getState(Class<S> stateType, S defaultState) {
    Assert.notNull(stateType, "StateType is required");
    Assert.notNull(defaultState, "DefaultState is required");
    S state = getState(stateType);
    return (state != null) ? state : defaultState;
  }

  @Nullable
  @Override
  public <S extends AvailabilityState> S getState(Class<S> stateType) {
    AvailabilityChangeEvent<S> event = getLastChangeEvent(stateType);
    return (event != null) ? event.getState() : null;
  }

  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public <S extends AvailabilityState> AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType) {
    return (AvailabilityChangeEvent<S>) this.events.get(stateType);
  }

  @Override
  public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
    Class<? extends AvailabilityState> type = getStateType(event.getState());
    if (this.logger.isDebugEnabled()) {
      this.logger.debug(getLogMessage(type, event));
    }
    this.events.put(type, event);
  }

  private <S extends AvailabilityState> Object getLogMessage(Class<S> type, AvailabilityChangeEvent<?> event) {
    AvailabilityChangeEvent<S> lastChangeEvent = getLastChangeEvent(type);
    StringBuilder message = new StringBuilder(
            "Application availability state %s changed".formatted(type.getSimpleName()));
    message.append((lastChangeEvent != null) ? " from " + lastChangeEvent.getState() : "");
    message.append(" to ").append(event.getState());
    message.append(getSourceDescription(event.getSource()));
    return message;
  }

  private String getSourceDescription(@Nullable Object source) {
    if (source == null || source instanceof ApplicationEventPublisher) {
      return "";
    }
    return ": " + ((source instanceof Throwable) ? source : source.getClass().getName());
  }

  @SuppressWarnings("unchecked")
  private Class<? extends AvailabilityState> getStateType(AvailabilityState state) {
    Class<?> type = (state instanceof Enum) ? ((Enum<?>) state).getDeclaringClass() : state.getClass();
    return (Class<? extends AvailabilityState>) type;
  }

}
