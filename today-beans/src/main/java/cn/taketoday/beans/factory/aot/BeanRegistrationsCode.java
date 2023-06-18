/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.aot;

import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.javapoet.ClassName;

/**
 * Interface that can be used to configure the code that will be generated to
 * register beans.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public interface BeanRegistrationsCode {

  /**
   * Return the name of the class being used for registrations.
   *
   * @return the generated class name.
   */
  ClassName getClassName();

  /**
   * Return a {@link GeneratedMethods} being used by the registrations code.
   *
   * @return the method generator
   */
  GeneratedMethods getMethods();

}
