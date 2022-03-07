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

import java.lang.reflect.Method;

/**
 * Simple subclass of {@code AbstractReflectiveMBeanInfoAssembler}
 * that always votes yes for method and property inclusion, effectively exposing
 * all public methods and properties as operations and attributes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleReflectiveMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

  /**
   * Always returns {@code true}.
   */
  @Override
  protected boolean includeReadAttribute(Method method, String beanKey) {
    return true;
  }

  /**
   * Always returns {@code true}.
   */
  @Override
  protected boolean includeWriteAttribute(Method method, String beanKey) {
    return true;
  }

  /**
   * Always returns {@code true}.
   */
  @Override
  protected boolean includeOperation(Method method, String beanKey) {
    return true;
  }

}
