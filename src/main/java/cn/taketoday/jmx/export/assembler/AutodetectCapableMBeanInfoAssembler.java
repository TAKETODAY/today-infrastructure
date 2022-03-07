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

import cn.taketoday.jmx.export.MBeanExporter;

/**
 * Extends the {@code MBeanInfoAssembler} to add autodetection logic.
 * Implementations of this interface are given the opportunity by the
 * {@code MBeanExporter} to include additional beans in the registration process.
 *
 * <p>The exact mechanism for deciding which beans to include is left to
 * implementing classes.
 *
 * @author Rob Harrop
 * @see MBeanExporter
 * @since 4.0
 */
public interface AutodetectCapableMBeanInfoAssembler extends MBeanInfoAssembler {

  /**
   * Indicate whether a particular bean should be included in the registration
   * process, if it is not specified in the {@code beans} map of the
   * {@code MBeanExporter}.
   *
   * @param beanClass the class of the bean (might be a proxy class)
   * @param beanName the name of the bean in the bean factory
   */
  boolean includeBean(Class<?> beanClass, String beanName);

}
