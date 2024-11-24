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

package infra.jmx.export.metadata;

import javax.management.modelmbean.ModelMBeanNotificationInfo;

import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * Utility methods for converting Framework JMX metadata into their plain JMX equivalents.
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
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("Must specify notification name");
    }

    String description = notificationInfo.getDescription();
    return new ModelMBeanNotificationInfo(notifTypes, name, description);
  }

}
