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
