/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context;

import cn.taketoday.beans.factory.DisposableBean;

/**
 * A common interface defining methods for start/stop lifecycle control.
 * The typical use case for this is to control asynchronous processing.
 * <b>NOTE: This interface does not imply specific auto-startup semantics.
 * Consider implementing {@link SmartLifecycle} for that purpose.</b>
 *
 * <p>Can be implemented by both components (typically a bean defined in a IoC context)
 * and containers  (typically a {@link ApplicationContext} itself).
 * Containers will propagate start/stop signals to all components that
 * apply within each container, e.g. for a stop/restart scenario at runtime.
 *
 * <p>Note that the present {@code Lifecycle} interface is only supported on
 * <b>top-level singleton beans</b>. On any other component, the {@code Lifecycle}
 * interface will remain undetected and hence ignored. Also, note that the extended
 * {@link SmartLifecycle} interface provides sophisticated integration with the
 * application context's startup and shutdown phases.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/11/12 16:26
 * @see SmartLifecycle
 * @see ConfigurableApplicationContext
 * @since 4.0
 */
public interface Lifecycle {

  /**
   * Start this component.
   * <p>Should not throw an exception if the component is already running.
   * <p>In the case of a container, this will propagate the start signal to all
   * components that apply.
   *
   * @see SmartLifecycle#isAutoStartup()
   */
  void start();

  /**
   * Stop this component, typically in a synchronous fashion, such that the component is
   * fully stopped upon return of this method. Consider implementing {@link SmartLifecycle}
   * and its {@code stop(Runnable)} variant when asynchronous stop behavior is necessary.
   * <p>Note that this stop notification is not guaranteed to come before destruction:
   * On regular shutdown, {@code Lifecycle} beans will first receive a stop notification
   * before the general destruction callbacks are being propagated; however, on hot
   * refresh during a context's lifetime or on aborted refresh attempts, a given bean's
   * destroy method will be called without any consideration of stop signals upfront.
   * <p>Should not throw an exception if the component is not running (not started yet).
   * <p>In the case of a container, this will propagate the stop signal to all components
   * that apply.
   *
   * @see SmartLifecycle#stop(Runnable)
   * @see DisposableBean#destroy()
   */
  void stop();

  /**
   * Check whether this component is currently running.
   * <p>In the case of a container, this will return {@code true} only if <i>all</i>
   * components that apply are currently running.
   *
   * @return whether the component is currently running
   */
  boolean isRunning();

}
