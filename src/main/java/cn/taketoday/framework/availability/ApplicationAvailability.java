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

/**
 * Provides {@link AvailabilityState availability state} information for the application.
 * <p>
 * Components can inject this class to get the current state information. To update the
 * state of the application an {@link AvailabilityChangeEvent} should be
 * {@link ApplicationContext#publishEvent published} to the application context with
 * directly or via {@link AvailabilityChangeEvent#publish}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 4.0
 */
public interface ApplicationAvailability {

  /**
   * Return the {@link LivenessState} of the application.
   *
   * @return the liveness state
   */
  default LivenessState getLivenessState() {
    return getState(LivenessState.class, LivenessState.BROKEN);
  }

  /**
   * Return the {@link ReadinessState} of the application.
   *
   * @return the readiness state
   */
  default ReadinessState getReadinessState() {
    return getState(ReadinessState.class, ReadinessState.REFUSING_TRAFFIC);
  }

  /**
   * Return {@link AvailabilityState} information for the application.
   *
   * @param <S> the state type
   * @param stateType the state type
   * @param defaultState the default state to return if no event of the given type has
   * been published yet (must not be {@code null}).
   * @return the readiness state
   * @see #getState(Class)
   */
  <S extends AvailabilityState> S getState(Class<S> stateType, S defaultState);

  /**
   * Return {@link AvailabilityState} information for the application.
   *
   * @param <S> the state type
   * @param stateType the state type
   * @return the readiness state or {@code null} if no event of the given type has been
   * published yet
   * @see #getState(Class, AvailabilityState)
   */
  <S extends AvailabilityState> S getState(Class<S> stateType);

  /**
   * Return the last {@link AvailabilityChangeEvent} received for a given state type.
   *
   * @param <S> the state type
   * @param stateType the state type
   * @return the readiness state or {@code null} if no event of the given type has been
   * published yet
   */
  <S extends AvailabilityState> AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType);

}
