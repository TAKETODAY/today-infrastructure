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

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;

import cn.taketoday.lang.Assert;

/**
 * {@link NotificationPublisher} implementation that uses the infrastructure
 * provided by the {@link ModelMBean} interface to track
 * {@link javax.management.NotificationListener javax.management.NotificationListeners}
 * and send {@link Notification Notifications} to those listeners.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see ModelMBeanNotificationBroadcaster
 * @see NotificationPublisherAware
 * @since 4.0
 */
public class ModelMBeanNotificationPublisher implements NotificationPublisher {

  /**
   * The {@link ModelMBean} instance wrapping the managed resource into which this
   * {@code NotificationPublisher} will be injected.
   */
  private final ModelMBeanNotificationBroadcaster modelMBean;

  /**
   * The {@link ObjectName} associated with the {@link ModelMBean modelMBean}.
   */
  private final ObjectName objectName;

  /**
   * The managed resource associated with the {@link ModelMBean modelMBean}.
   */
  private final Object managedResource;

  /**
   * Create a new instance of the {@link ModelMBeanNotificationPublisher} class
   * that will publish all {@link Notification Notifications}
   * to the supplied {@link ModelMBean}.
   *
   * @param modelMBean the target {@link ModelMBean}; must not be {@code null}
   * @param objectName the {@link ObjectName} of the source {@link ModelMBean}
   * @param managedResource the managed resource exposed by the supplied {@link ModelMBean}
   * @throws IllegalArgumentException if any of the parameters is {@code null}
   */
  public ModelMBeanNotificationPublisher(
          ModelMBeanNotificationBroadcaster modelMBean, ObjectName objectName, Object managedResource) {

    Assert.notNull(modelMBean, "'modelMBean' must not be null");
    Assert.notNull(objectName, "'objectName' must not be null");
    Assert.notNull(managedResource, "'managedResource' must not be null");
    this.modelMBean = modelMBean;
    this.objectName = objectName;
    this.managedResource = managedResource;
  }

  /**
   * Send the supplied {@link Notification} using the wrapped
   * {@link ModelMBean} instance.
   *
   * @param notification the {@link Notification} to be sent
   * @throws IllegalArgumentException if the supplied {@code notification} is {@code null}
   * @throws UnableToSendNotificationException if the supplied {@code notification} could not be sent
   */
  @Override
  public void sendNotification(Notification notification) {
    Assert.notNull(notification, "Notification must not be null");
    replaceNotificationSourceIfNecessary(notification);
    try {
      if (notification instanceof AttributeChangeNotification) {
        this.modelMBean.sendAttributeChangeNotification((AttributeChangeNotification) notification);
      }
      else {
        this.modelMBean.sendNotification(notification);
      }
    }
    catch (MBeanException ex) {
      throw new UnableToSendNotificationException("Unable to send notification [" + notification + "]", ex);
    }
  }

  /**
   * Replaces the notification source if necessary to do so.
   * From the {@link Notification javadoc}:
   * <i>"It is strongly recommended that notification senders use the object name
   * rather than a reference to the MBean object as the source."</i>
   *
   * @param notification the {@link Notification} whose
   * {@link Notification#getSource()} might need massaging
   */
  private void replaceNotificationSourceIfNecessary(Notification notification) {
    if (notification.getSource() == null || notification.getSource().equals(this.managedResource)) {
      notification.setSource(this.objectName);
    }
  }

}
