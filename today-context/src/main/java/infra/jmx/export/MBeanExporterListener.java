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

import javax.management.ObjectName;

/**
 * A listener that allows application code to be notified when an MBean is
 * registered and unregistered via an {@link MBeanExporter}.
 *
 * @author Rob Harrop
 * @see MBeanExporter#setListeners
 * @since 4.0
 */
public interface MBeanExporterListener {

  /**
   * Called by {@link MBeanExporter} after an MBean has been <i>successfully</i>
   * registered with an {@link javax.management.MBeanServer}.
   *
   * @param objectName the {@code ObjectName} of the registered MBean
   */
  void mbeanRegistered(ObjectName objectName);

  /**
   * Called by {@link MBeanExporter} after an MBean has been <i>successfully</i>
   * unregistered from an {@link javax.management.MBeanServer}.
   *
   * @param objectName the {@code ObjectName} of the unregistered MBean
   */
  void mbeanUnregistered(ObjectName objectName);

}
