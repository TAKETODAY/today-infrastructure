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

package infra.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import infra.jmx.export.MBeanExporter;
import infra.jmx.support.ObjectNameManager;

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
