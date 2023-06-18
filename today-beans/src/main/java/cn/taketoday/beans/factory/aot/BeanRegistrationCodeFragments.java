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

import java.lang.reflect.Executable;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;

/**
 * Generate the various fragments of code needed to register a bean.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public interface BeanRegistrationCodeFragments {

  /**
   * The variable name to used when creating the bean definition.
   */
  String BEAN_DEFINITION_VARIABLE = "beanDefinition";

  /**
   * The variable name to used when creating the bean definition.
   */
  String INSTANCE_SUPPLIER_VARIABLE = "instanceSupplier";

  /**
   * Return the target for the registration. Used to determine where to write
   * the code.
   *
   * @param registeredBean the registered bean
   * @param constructorOrFactoryMethod the constructor or factory method
   * @return the target {@link ClassName}
   */
  ClassName getTarget(RegisteredBean registeredBean,
          Executable constructorOrFactoryMethod);

  /**
   * Generate the code that defines the new bean definition instance.
   *
   * @param generationContext the generation context
   * @param beanType the bean type
   * @param beanRegistrationCode the bean registration code
   * @return the generated code
   */
  CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
          ResolvableType beanType, BeanRegistrationCode beanRegistrationCode);

  /**
   * Generate the code that sets the properties of the bean definition.
   *
   * @param generationContext the generation context
   * @param beanRegistrationCode the bean registration code
   * @param attributeFilter any attribute filtering that should be applied
   * @return the generated code
   */
  CodeBlock generateSetBeanDefinitionPropertiesCode(
          GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
          RootBeanDefinition beanDefinition, Predicate<String> attributeFilter);

  /**
   * Generate the code that sets the instance supplier on the bean definition.
   *
   * @param generationContext the generation context
   * @param beanRegistrationCode the bean registration code
   * @param instanceSupplierCode the instance supplier code supplier code
   * @param postProcessors any instance post processors that should be applied
   * @return the generated code
   * @see #generateInstanceSupplierCode
   */
  CodeBlock generateSetBeanInstanceSupplierCode(
          GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
          CodeBlock instanceSupplierCode, List<MethodReference> postProcessors);

  /**
   * Generate the instance supplier code.
   *
   * @param generationContext the generation context
   * @param beanRegistrationCode the bean registration code
   * @param constructorOrFactoryMethod the constructor or factory method for
   * the bean
   * @param allowDirectSupplierShortcut if direct suppliers may be used rather
   * than always needing an {@link InstanceSupplier}
   * @return the generated code
   */
  CodeBlock generateInstanceSupplierCode(
          GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
          Executable constructorOrFactoryMethod, boolean allowDirectSupplierShortcut);

  /**
   * Generate the return statement.
   *
   * @param generationContext the generation context
   * @param beanRegistrationCode the bean registration code
   * @return the generated code
   */
  CodeBlock generateReturnCode(
          GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

}
