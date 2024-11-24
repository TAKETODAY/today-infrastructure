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
import infra.aot.generate.MethodReference;
import infra.beans.factory.support.InstanceSupplier;
import infra.util.function.ThrowingBiFunction;

/**
 * Interface that can be used to configure the code that will be generated to
 * perform registration of a single bean.
 *
 * @author Phillip Webb
 * @see BeanRegistrationCodeFragments
 * @since 4.0
 */
public interface BeanRegistrationCode {

  /**
   * Return the name of the class being used for registrations.
   *
   * @return the name of the class
   */
  ClassName getClassName();

  /**
   * Return a {@link GeneratedMethods} being used by the registrations code.
   *
   * @return the generated methods
   */
  GeneratedMethods getMethods();

  /**
   * Add an instance post processor method call to the registration code.
   *
   * @param methodReference a reference to the post-process method to call.
   * The referenced method must have a functional signature compatible with
   * {@link InstanceSupplier#andThen}.
   * @see InstanceSupplier#andThen(ThrowingBiFunction)
   */
  void addInstancePostProcessor(MethodReference methodReference);

}
