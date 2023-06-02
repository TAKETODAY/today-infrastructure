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

package cn.taketoday.annotation.config.jmx;

import javax.management.MXBean;

import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.context.event.ApplicationReadyEvent;

/**
 * An MBean contract to control and monitor a running {@code Application} via JMX.
 * Intended for internal use only.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@MXBean
public interface InfraApplicationMXBean {

  /**
   * Specify if the application has fully started and is now ready.
   *
   * @return {@code true} if the application is ready
   * @see ApplicationReadyEvent
   */
  boolean isReady();

  /**
   * Specify if the application runs in an embedded web container. Return {@code false}
   * on a web application that hasn't fully started yet, so it is preferable to wait for
   * the application to be {@link #isReady() ready}.
   *
   * @return {@code true} if the application runs in an embedded web container
   * @see #isReady()
   */
  boolean isEmbeddedWebApplication();

  /**
   * Return the value of the specified key from the application
   * {@link Environment Environment}.
   *
   * @param key the property key
   * @return the property value or {@code null} if it does not exist
   */
  String getProperty(String key);

  /**
   * Shutdown the application.
   *
   * @see cn.taketoday.context.ConfigurableApplicationContext#close()
   */
  void shutdown();

}
