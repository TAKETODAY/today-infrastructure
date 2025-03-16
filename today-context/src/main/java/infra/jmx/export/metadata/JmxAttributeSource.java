/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.lang.reflect.Method;

import infra.jmx.export.MBeanExporter;
import infra.jmx.export.assembler.MetadataMBeanInfoAssembler;
import infra.lang.Nullable;

/**
 * Interface used by the {@code MetadataMBeanInfoAssembler} to
 * read source-level metadata from a managed resource's class.
 *
 * @author Rob Harrop
 * @author Jennifer Hickey
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see MetadataMBeanInfoAssembler#setAttributeSource
 * @see MBeanExporter#setAssembler
 * @since 4.0
 */
public interface JmxAttributeSource {

  /**
   * Implementations should return an instance of {@link ManagedResource}
   * if the supplied {@code Class} has the corresponding metadata.
   *
   * @param clazz the class to read the resource data from
   * @return the resource, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid metadata
   */
  @Nullable
  ManagedResource getManagedResource(Class<?> clazz) throws InvalidMetadataException;

  /**
   * Implementations should return an instance of {@link ManagedAttribute}
   * if the supplied {@code Method} has the corresponding metadata.
   *
   * @param method the method to read the attribute data from
   * @return the attribute, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid metadata
   */
  @Nullable
  ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an instance of {@link ManagedMetric}
   * if the supplied {@code Method} has the corresponding metadata.
   *
   * @param method the method to read the metric data from
   * @return the metric, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid metadata
   */
  @Nullable
  ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an instance of {@link ManagedOperation}
   * if the supplied {@code Method} has the corresponding metadata.
   *
   * @param method the method to read the operation data from
   * @return the operation, or {@code null} if not found
   * @throws InvalidMetadataException in case of invalid metadata
   */
  @Nullable
  ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an array of {@link ManagedOperationParameter
   * ManagedOperationParameters} if the supplied {@code Method} has the corresponding
   * metadata.
   *
   * @param method the {@code Method} to read the metadata from
   * @return the parameter information, or an empty array if no metadata is found
   * @throws InvalidMetadataException in case of invalid metadata
   */
  @Nullable
  ManagedOperationParameter[] getManagedOperationParameters(Method method) throws InvalidMetadataException;

  /**
   * Implementations should return an array of {@link ManagedNotification ManagedNotifications}
   * if the supplied {@code Class} has the corresponding metadata.
   *
   * @param clazz the {@code Class} to read the metadata from
   * @return the notification information, or an empty array if no metadata is found
   * @throws InvalidMetadataException in case of invalid metadata
   */
  @Nullable
  ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException;

}
