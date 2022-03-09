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

package cn.taketoday.jmx.export;

import javax.management.ObjectName;

/**
 * Interface that defines the set of MBean export operations that are intended to be
 * accessed by application developers during application runtime.
 *
 * <p>This interface should be used to export application resources to JMX using Framework's
 * management interface generation capabilities and, optionally, it's {@link ObjectName}
 * generation capabilities.
 *
 * @author Rob Harrop
 * @see MBeanExporter
 * @since 4.0
 */
public interface MBeanExportOperations {

  /**
   * Register the supplied resource with JMX. If the resource is not a valid MBean already,
   * Framework will generate a management interface for it. The exact interface generated will
   * depend on the implementation and its configuration. This call also generates an
   * {@link ObjectName} for the managed resource and returns this to the caller.
   *
   * @param managedResource the resource to expose via JMX
   * @return the {@link ObjectName} under which the resource was exposed
   * @throws MBeanExportException if Framework is unable to generate an {@link ObjectName}
   * or register the MBean
   */
  ObjectName registerManagedResource(Object managedResource) throws MBeanExportException;

  /**
   * Register the supplied resource with JMX. If the resource is not a valid MBean already,
   * Framework will generate a management interface for it. The exact interface generated will
   * depend on the implementation and its configuration.
   *
   * @param managedResource the resource to expose via JMX
   * @param objectName the {@link ObjectName} under which to expose the resource
   * @throws MBeanExportException if Framework is unable to register the MBean
   */
  void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException;

  /**
   * Remove the specified MBean from the underlying MBeanServer registry.
   *
   * @param objectName the {@link ObjectName} of the resource to remove
   */
  void unregisterManagedResource(ObjectName objectName);

}
