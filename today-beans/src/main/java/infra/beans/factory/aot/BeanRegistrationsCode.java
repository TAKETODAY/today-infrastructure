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

package infra.beans.factory.aot;

import infra.javapoet.ClassName;
import infra.aot.generate.GeneratedMethods;

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
