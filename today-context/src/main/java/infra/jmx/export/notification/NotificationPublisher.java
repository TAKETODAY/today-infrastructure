/*
 * Copyright 2002-present the original author or authors.
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

package infra.jmx.export.notification;

import javax.management.Notification;

import infra.jmx.export.MBeanExporter;

/**
 * Simple interface allowing Framework-managed MBeans to publish JMX notifications
 * without being aware of how those notifications are being transmitted to the
 * {@link javax.management.MBeanServer}.
 *
 * <p>Managed resources can access a {@code NotificationPublisher} by
 * implementing the {@link NotificationPublisherAware} interface. After a particular
 * managed resource instance is registered with the {@link javax.management.MBeanServer},
 * Framework will inject a {@code NotificationPublisher} instance into it if that
 * resource implements the {@link NotificationPublisherAware} interface.
 *
 * <p>Each managed resource instance will have a distinct instance of a
 * {@code NotificationPublisher} implementation. This instance will keep
 * track of all the {@link javax.management.NotificationListener NotificationListeners}
 * registered for a particular mananaged resource.
 *
 * <p>Any existing, user-defined MBeans should use standard JMX APIs for notification
 * publication; this interface is intended for use only by Framework-created MBeans.
 *
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NotificationPublisherAware
 * @see MBeanExporter
 * @since 4.0
 */
@FunctionalInterface
public interface NotificationPublisher {

  /**
   * Send the specified {@link Notification} to all registered
   * {@link javax.management.NotificationListener NotificationListeners}.
   * Managed resources are <strong>not</strong> responsible for managing the list
   * of registered {@link javax.management.NotificationListener NotificationListeners};
   * that is performed automatically.
   *
   * @param notification the JMX Notification to send
   * @throws UnableToSendNotificationException if sending failed
   */
  void sendNotification(Notification notification) throws UnableToSendNotificationException;

}
