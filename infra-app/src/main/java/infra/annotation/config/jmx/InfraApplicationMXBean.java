/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.annotation.config.jmx;

import javax.management.MXBean;

import infra.app.context.event.ApplicationReadyEvent;
import infra.core.env.Environment;

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
   * @see infra.context.ConfigurableApplicationContext#close()
   */
  void shutdown();

}
