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

package cn.taketoday.jmx.export.assembler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.modelmbean.ModelMBeanNotificationInfo;

import cn.taketoday.jmx.export.metadata.JmxMetadataUtils;
import cn.taketoday.jmx.export.metadata.ManagedNotification;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Base class for MBeanInfoAssemblers that support configurable
 * JMX notification behavior.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractConfigurableMBeanInfoAssembler extends AbstractReflectiveMBeanInfoAssembler {

  @Nullable
  private ModelMBeanNotificationInfo[] notificationInfos;

  private final Map<String, ModelMBeanNotificationInfo[]> notificationInfoMappings = new HashMap<>();

  public void setNotificationInfos(ManagedNotification[] notificationInfos) {
    ModelMBeanNotificationInfo[] infos = new ModelMBeanNotificationInfo[notificationInfos.length];
    for (int i = 0; i < notificationInfos.length; i++) {
      ManagedNotification notificationInfo = notificationInfos[i];
      infos[i] = JmxMetadataUtils.convertToModelMBeanNotificationInfo(notificationInfo);
    }
    this.notificationInfos = infos;
  }

  public void setNotificationInfoMappings(Map<String, Object> notificationInfoMappings) {
    notificationInfoMappings.forEach((beanKey, result) ->
            this.notificationInfoMappings.put(beanKey, extractNotificationMetadata(result)));
  }

  @Override
  protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey) {
    ModelMBeanNotificationInfo[] result = null;
    if (StringUtils.hasText(beanKey)) {
      result = this.notificationInfoMappings.get(beanKey);
    }
    if (result == null) {
      result = this.notificationInfos;
    }
    return (result != null ? result : new ModelMBeanNotificationInfo[0]);
  }

  private ModelMBeanNotificationInfo[] extractNotificationMetadata(Object mapValue) {
    if (mapValue instanceof ManagedNotification mn) {
      return new ModelMBeanNotificationInfo[] { JmxMetadataUtils.convertToModelMBeanNotificationInfo(mn) };
    }
    else if (mapValue instanceof Collection<?> col) {
      List<ModelMBeanNotificationInfo> result = new ArrayList<>();
      for (Object colValue : col) {
        if (!(colValue instanceof ManagedNotification mn)) {
          throw new IllegalArgumentException(
                  "Property 'notificationInfoMappings' only accepts ManagedNotifications for Map values");
        }
        result.add(JmxMetadataUtils.convertToModelMBeanNotificationInfo(mn));
      }
      return result.toArray(new ModelMBeanNotificationInfo[0]);
    }
    else {
      throw new IllegalArgumentException(
              "Property 'notificationInfoMappings' only accepts ManagedNotifications for Map values");
    }
  }

}
