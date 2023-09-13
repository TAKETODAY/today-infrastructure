/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

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
 * <p>
 * A default implementation is provided that suits most needs and custom code
 * fragments are only expected to be used by library authors having built custom
 * arrangement on top of the core container.
 * <p>
 * Users are not expected to implement this interface directly, but rather extends
 * from {@link BeanRegistrationCodeFragmentsDecorator} and only override the
 * necessary method(s).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanRegistrationCodeFragmentsDecorator
 * @see BeanRegistrationAotContribution#withCustomCodeFragments(UnaryOperator)
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
   * the code. This should take into account visibility issue, such as
   * package access of an element of the bean to register.
   *
   * @param registeredBean the registered bean
   * @return the target {@link ClassName}
   */
  ClassName getTarget(RegisteredBean registeredBean);

  /**
   * Generate the code that defines the new bean definition instance.
   * <p>
   * This should declare a variable named {@value BEAN_DEFINITION_VARIABLE}
   * so that further fragments can refer to the variable to further tune
   * the bean definition.
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
   * <p>
   * The {@code postProcessors} represent methods to be exposed once the
   * instance has been created to further configure it. Each method should
   * accept two parameters, the {@link RegisteredBean} and the bean
   * instance, and should return the modified bean instance.
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
   * @param allowDirectSupplierShortcut if direct suppliers may be used rather
   * than always needing an {@link InstanceSupplier}
   * @return the generated code
   */
  CodeBlock generateInstanceSupplierCode(
          GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
          boolean allowDirectSupplierShortcut);

  /**
   * Generate the return statement.
   *
   * @param generationContext the generation context
   * @param beanRegistrationCode the bean registration code
   * @return the generated code
   */
  CodeBlock generateReturnCode(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

}
