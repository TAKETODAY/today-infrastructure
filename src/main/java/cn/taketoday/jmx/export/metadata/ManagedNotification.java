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

package cn.taketoday.jmx.export.metadata;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Metadata that indicates a JMX notification emitted by a bean.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class ManagedNotification {

  @Nullable
  private String[] notificationTypes;

  @Nullable
  private String name;

  @Nullable
  private String description;

  /**
   * Set a single notification type, or a list of notification types
   * as comma-delimited String.
   */
  public void setNotificationType(String notificationType) {
    this.notificationTypes = StringUtils.commaDelimitedListToStringArray(notificationType);
  }

  /**
   * Set a list of notification types.
   */
  public void setNotificationTypes(@Nullable String... notificationTypes) {
    this.notificationTypes = notificationTypes;
  }

  /**
   * Return the list of notification types.
   */
  @Nullable
  public String[] getNotificationTypes() {
    return this.notificationTypes;
  }

  /**
   * Set the name of this notification.
   */
  public void setName(@Nullable String name) {
    this.name = name;
  }

  /**
   * Return the name of this notification.
   */
  @Nullable
  public String getName() {
    return this.name;
  }

  /**
   * Set a description for this notification.
   */
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  /**
   * Return a description for this notification.
   */
  @Nullable
  public String getDescription() {
    return this.description;
  }

}
