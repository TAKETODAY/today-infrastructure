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

import javax.management.modelmbean.ModelMBeanNotificationInfo;

import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods for converting Spring JMX metadata into their plain JMX equivalents.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class JmxMetadataUtils {

  /**
   * Convert the supplied {@link ManagedNotification} into the corresponding
   * {@link ModelMBeanNotificationInfo}.
   */
  public static ModelMBeanNotificationInfo convertToModelMBeanNotificationInfo(ManagedNotification notificationInfo) {
    String[] notifTypes = notificationInfo.getNotificationTypes();
    if (ObjectUtils.isEmpty(notifTypes)) {
      throw new IllegalArgumentException("Must specify at least one notification type");
    }

    String name = notificationInfo.getName();
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("Must specify notification name");
    }

    String description = notificationInfo.getDescription();
    return new ModelMBeanNotificationInfo(notifTypes, name, description);
  }

}
