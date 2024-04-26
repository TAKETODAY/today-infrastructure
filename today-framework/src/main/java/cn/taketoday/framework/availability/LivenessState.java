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

package cn.taketoday.framework.availability;

/**
 * "Liveness" state of the application.
 * <p>
 * An application is considered live when it's running with a correct internal state.
 * "Liveness" failure means that the internal state of the application is broken and we
 * cannot recover from it. As a result, the platform should restart the application.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public enum LivenessState implements AvailabilityState {

  /**
   * The application is running and its internal state is correct.
   */
  CORRECT,

  /**
   * The application is running but its internal state is broken.
   */
  BROKEN

}
