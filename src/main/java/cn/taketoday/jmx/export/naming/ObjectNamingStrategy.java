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

package cn.taketoday.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface that encapsulates the creation of {@code ObjectName} instances.
 *
 * <p>Used by the {@code MBeanExporter} to obtain {@code ObjectName}s
 * when registering beans.
 *
 * @author Rob Harrop
 * @see MBeanExporter
 * @see ObjectName
 * @since 4.0
 */
@FunctionalInterface
public interface ObjectNamingStrategy {

  /**
   * Obtain an {@code ObjectName} for the supplied bean.
   *
   * @param managedBean the bean that will be exposed under the
   * returned {@code ObjectName}
   * @param beanKey the key associated with this bean in the beans map
   * passed to the {@code MBeanExporter}
   * @return the {@code ObjectName} instance
   * @throws MalformedObjectNameException if the resulting {@code ObjectName} is invalid
   */
  ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException;

}
