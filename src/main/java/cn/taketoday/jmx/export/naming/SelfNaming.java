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
import cn.taketoday.jmx.support.ObjectNameManager;

/**
 * Interface that allows infrastructure components to provide their own
 * {@code ObjectName}s to the {@code MBeanExporter}.
 *
 * <p><b>Note:</b> This interface is mainly intended for internal usage.
 *
 * @author Rob Harrop
 * @see MBeanExporter
 * @since 4.0
 */
public interface SelfNaming {

  /**
   * Return the {@code ObjectName} for the implementing object.
   *
   * @throws MalformedObjectNameException if thrown by the ObjectName constructor
   * @see ObjectName#ObjectName(String)
   * @see ObjectName#getInstance(String)
   * @see ObjectNameManager#getInstance(String)
   */
  ObjectName getObjectName() throws MalformedObjectNameException;

}
