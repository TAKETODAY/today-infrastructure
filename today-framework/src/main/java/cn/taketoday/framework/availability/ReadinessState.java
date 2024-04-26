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
 * "Readiness" state of the application.
 * <p>
 * An application is considered ready when it's {@link LivenessState live} and willing to
 * accept traffic. "Readiness" failure means that the application is not able to accept
 * traffic and that the infrastructure should stop routing requests to it.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public enum ReadinessState implements AvailabilityState {

  /**
   * The application is ready to receive traffic.
   */
  ACCEPTING_TRAFFIC,

  /**
   * The application is not willing to receive traffic.
   */
  REFUSING_TRAFFIC

}
