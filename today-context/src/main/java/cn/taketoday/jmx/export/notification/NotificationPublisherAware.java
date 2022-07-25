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

package cn.taketoday.jmx.export.notification;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.jmx.export.MBeanExporter;

/**
 * Interface to be implemented by any Framework-managed resource that is to be
 * registered with an {@link javax.management.MBeanServer} and wishes to send
 * JMX {@link javax.management.Notification javax.management.Notifications}.
 *
 * <p>Provides Framework-created managed resources with a {@link NotificationPublisher}
 * as soon as they are registered with the {@link javax.management.MBeanServer}.
 *
 * <p><b>NOTE:</b> This interface only applies to simple Framework-managed
 * beans which happen to get exported through Framework's
 * {@link MBeanExporter}.
 * It does not apply to any non-exported beans; neither does it apply
 * to standard MBeans exported by Framework. For standard JMX MBeans,
 * consider implementing the {@link javax.management.modelmbean.ModelMBeanNotificationBroadcaster}
 * interface (or implementing a full {@link javax.management.modelmbean.ModelMBean}).
 *
 * @author Rob Harrop
 * @author Chris Beams
 * @see NotificationPublisher
 * @since 4.0
 */
public interface NotificationPublisherAware extends Aware {

  /**
   * Set the {@link NotificationPublisher} instance for the current managed resource instance.
   */
  void setNotificationPublisher(NotificationPublisher notificationPublisher);

}
