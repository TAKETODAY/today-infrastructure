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

package infra.jmx.export.naming;

import org.jspecify.annotations.Nullable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import infra.jmx.export.MBeanExporter;

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
