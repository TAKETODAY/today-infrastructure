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

package infra.jmx.export;

import javax.management.NotificationListener;

import infra.beans.factory.InitializingBean;
import infra.jmx.support.NotificationListenerHolder;
import infra.lang.Assert;

/**
 * Helper class that aggregates a {@link NotificationListener},
 * a {@link javax.management.NotificationFilter}, and an arbitrary handback object.
 *
 * <p>Also provides support for associating the encapsulated
 * {@link NotificationListener} with any number of
 * MBeans from which it wishes to receive
 * {@link javax.management.Notification Notifications} via the
 * {@link #setMappedObjectNames mappedObjectNames} property.
 *
 * <p>Note: This class supports Framework bean names as
 * {@link #setMappedObjectNames "mappedObjectNames"} as well, as alternative
 * to specifying JMX object names. Note that only beans exported by the
 * same {@link MBeanExporter} are supported for such bean names.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see MBeanExporter#setNotificationListeners
 * @since 4.0
 */
public class NotificationListenerBean extends NotificationListenerHolder implements InitializingBean {

  /**
   * Create a new instance of the {@link NotificationListenerBean} class.
   */
  public NotificationListenerBean() {
  }

  /**
   * Create a new instance of the {@link NotificationListenerBean} class.
   *
   * @param notificationListener the encapsulated listener
   */
  public NotificationListenerBean(NotificationListener notificationListener) {
    Assert.notNull(notificationListener, "NotificationListener is required");
    setNotificationListener(notificationListener);
  }

  @Override
  public void afterPropertiesSet() {
    if (getNotificationListener() == null) {
      throw new IllegalArgumentException("Property 'notificationListener' is required");
    }
  }

  void replaceObjectName(Object originalName, Object newName) {
    if (this.mappedObjectNames != null && this.mappedObjectNames.contains(originalName)) {
      this.mappedObjectNames.remove(originalName);
      this.mappedObjectNames.add(newName);
    }
  }

}
