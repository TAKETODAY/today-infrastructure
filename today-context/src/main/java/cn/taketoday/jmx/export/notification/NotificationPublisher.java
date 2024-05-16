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

package cn.taketoday.jmx.export.notification;

import javax.management.Notification;

import cn.taketoday.jmx.export.MBeanExporter;

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
