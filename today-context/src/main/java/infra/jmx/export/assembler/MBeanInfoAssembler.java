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

package infra.jmx.export.assembler;

import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanInfo;

import infra.jmx.export.MBeanExporter;

/**
 * Interface to be implemented by all classes that can
 * create management interface metadata for a managed resource.
 *
 * <p>Used by the {@code MBeanExporter} to generate the management
 * interface for any bean that is not an MBean.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see MBeanExporter
 * @since 4.0
 */
public interface MBeanInfoAssembler {

  /**
   * Create the ModelMBeanInfo for the given managed resource.
   *
   * @param managedBean the bean that will be exposed (might be an AOP proxy)
   * @param beanKey the key associated with the managed bean
   * @return the ModelMBeanInfo metadata object
   * @throws JMException in case of errors
   */
  ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException;

}
