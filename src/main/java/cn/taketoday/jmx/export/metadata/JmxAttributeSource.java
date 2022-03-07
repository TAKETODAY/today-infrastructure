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

import java.lang.reflect.Method;

import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.jmx.export.assembler.MetadataMBeanInfoAssembler;
import cn.taketoday.lang.Nullable;

/**
 * Interface used by the {@code MetadataMBeanInfoAssembler} to
 * read source-level metadata from a managed resource's class.
 *
 * @author Rob Harrop
 * @author Jennifer Hickey
 * @see MetadataMBeanInfoAssembler#setAttributeSource
 * @see MBeanExporter#setAssembler
 * @since 4.0
 */
public interface JmxAttributeSource {

  /**
   * Implementations should return an instance of {@code ManagedResource}
   * if the supplied {@code Class} has the appropriate metadata.
   * Otherwise should return {@code null}.
   *
   * @param clazz the class to read the attribute data from
   * @return the attribute, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid attributes
   */
  @Nullable
  ManagedResource getManagedResource(Class<?> clazz) throws InvalidMetadataException;

  /**
   * Implementations should return an instance of {@code ManagedAttribute}
   * if the supplied {@code Method} has the corresponding metadata.
   * Otherwise should return {@code null}.
   *
   * @param method the method to read the attribute data from
   * @return the attribute, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid attributes
   */
  @Nullable
  ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an instance of {@code ManagedMetric}
   * if the supplied {@code Method} has the corresponding metadata.
   * Otherwise should return {@code null}.
   *
   * @param method the method to read the attribute data from
   * @return the metric, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid attributes
   */
  @Nullable
  ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an instance of {@code ManagedOperation}
   * if the supplied {@code Method} has the corresponding metadata.
   * Otherwise should return {@code null}.
   *
   * @param method the method to read the attribute data from
   * @return the attribute, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid attributes
   */
  @Nullable
  ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an array of {@code ManagedOperationParameter}
   * if the supplied {@code Method} has the corresponding metadata. Otherwise
   * should return an empty array if no metadata is found.
   *
   * @param method the {@code Method} to read the metadata from
   * @return the parameter information.
   * @throws InvalidMetadataException in the case of invalid attributes.
   */
  ManagedOperationParameter[] getManagedOperationParameters(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an array of {@link ManagedNotification ManagedNotifications}
   * if the supplied the {@code Class} has the corresponding metadata. Otherwise
   * should return an empty array.
   *
   * @param clazz the {@code Class} to read the metadata from
   * @return the notification information
   * @throws InvalidMetadataException in the case of invalid metadata
   */
  ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException;

}
