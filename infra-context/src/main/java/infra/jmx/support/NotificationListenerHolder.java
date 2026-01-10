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

package infra.jmx.support;

import org.jspecify.annotations.Nullable;

import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import infra.jmx.access.NotificationListenerRegistrar;
import infra.jmx.export.NotificationListenerBean;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;

/**
 * Helper class that aggregates a {@link NotificationListener},
 * a {@link NotificationFilter}, and an arbitrary handback
 * object, as well as the names of MBeans from which the listener wishes
 * to receive {@link javax.management.Notification Notifications}.
 *
 * @author Juergen Hoeller
 * @see NotificationListenerBean
 * @see NotificationListenerRegistrar
 * @since 4.0
 */
public class NotificationListenerHolder {

  @Nullable
  private NotificationListener notificationListener;

  @Nullable
  private NotificationFilter notificationFilter;

  @Nullable
  private Object handback;

  @Nullable
  protected Set<Object> mappedObjectNames;

  /**
   * Set the {@link NotificationListener}.
   */
  public void setNotificationListener(@Nullable NotificationListener notificationListener) {
    this.notificationListener = notificationListener;
  }

  /**
   * Get the {@link NotificationListener}.
   */
  @Nullable
  public NotificationListener getNotificationListener() {
    return this.notificationListener;
  }

  /**
   * Set the {@link NotificationFilter} associated
   * with the encapsulated {@link #getNotificationFilter() NotificationFilter}.
   * <p>May be {@code null}.
   */
  public void setNotificationFilter(@Nullable NotificationFilter notificationFilter) {
    this.notificationFilter = notificationFilter;
  }

  /**
   * Return the {@link NotificationFilter} associated
   * with the encapsulated {@link #getNotificationListener() NotificationListener}.
   * <p>May be {@code null}.
   */
  @Nullable
  public NotificationFilter getNotificationFilter() {
    return this.notificationFilter;
  }

  /**
   * Set the (arbitrary) object that will be 'handed back' as-is by an
   * {@link javax.management.NotificationBroadcaster} when notifying
   * any {@link NotificationListener}.
   *
   * @param handback the handback object (can be {@code null})
   * @see NotificationListener#handleNotification(javax.management.Notification, Object)
   */
  public void setHandback(@Nullable Object handback) {
    this.handback = handback;
  }

  /**
   * Return the (arbitrary) object that will be 'handed back' as-is by an
   * {@link javax.management.NotificationBroadcaster} when notifying
   * any {@link NotificationListener}.
   *
   * @return the handback object (may be {@code null})
   * @see NotificationListener#handleNotification(javax.management.Notification, Object)
   */
  @Nullable
  public Object getHandback() {
    return this.handback;
  }

  /**
   * Set the {@link ObjectName}-style name of the single MBean
   * that the encapsulated {@link #getNotificationFilter() NotificationFilter}
   * will be registered with to listen for {@link javax.management.Notification Notifications}.
   * Can be specified as {@code ObjectName} instance or as {@code String}.
   *
   * @see #setMappedObjectNames
   */
  public void setMappedObjectName(@Nullable Object mappedObjectName) {
    this.mappedObjectNames = mappedObjectName != null ?
            CollectionUtils.newLinkedHashSet(mappedObjectName) : null;
  }

  /**
   * Set an array of {@link ObjectName}-style names of the MBeans
   * that the encapsulated {@link #getNotificationFilter() NotificationFilter}
   * will be registered with to listen for {@link javax.management.Notification Notifications}.
   * Can be specified as {@code ObjectName} instances or as {@code String}s.
   *
   * @see #setMappedObjectName
   */
  public void setMappedObjectNames(Object... mappedObjectNames) {
    this.mappedObjectNames = CollectionUtils.newLinkedHashSet(mappedObjectNames);
  }

  /**
   * Return the list of {@link ObjectName} String representations for
   * which the encapsulated {@link #getNotificationFilter() NotificationFilter} will
   * be registered as a listener for {@link javax.management.Notification Notifications}.
   *
   * @throws MalformedObjectNameException if an {@code ObjectName} is malformed
   */
  public ObjectName @Nullable [] getResolvedObjectNames() throws MalformedObjectNameException {
    if (this.mappedObjectNames == null) {
      return null;
    }
    ObjectName[] resolved = new ObjectName[this.mappedObjectNames.size()];
    int i = 0;
    for (Object objectName : this.mappedObjectNames) {
      resolved[i] = ObjectNameManager.getInstance(objectName);
      i++;
    }
    return resolved;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NotificationListenerHolder otherNlh)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(this.notificationListener, otherNlh.notificationListener) &&
            ObjectUtils.nullSafeEquals(this.notificationFilter, otherNlh.notificationFilter) &&
            ObjectUtils.nullSafeEquals(this.handback, otherNlh.handback) &&
            ObjectUtils.nullSafeEquals(this.mappedObjectNames, otherNlh.mappedObjectNames));
  }

  @Override
  public int hashCode() {
    int hashCode = ObjectUtils.nullSafeHashCode(this.notificationListener);
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.notificationFilter);
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.handback);
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.mappedObjectNames);
    return hashCode;
  }

}
