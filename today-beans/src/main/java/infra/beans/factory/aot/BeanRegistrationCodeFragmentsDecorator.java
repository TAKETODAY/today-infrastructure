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

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.ResolvableType;
import infra.lang.Assert;

/**
 * A {@link BeanRegistrationCodeFragments} decorator implementation. Typically
 * used when part of the default code fragments have to customized, by extending
 * this class and using it as part of
 * {@link BeanRegistrationAotContribution#withCustomCodeFragments(UnaryOperator)}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BeanRegistrationCodeFragmentsDecorator implements BeanRegistrationCodeFragments {

  private final BeanRegistrationCodeFragments delegate;

  protected BeanRegistrationCodeFragmentsDecorator(BeanRegistrationCodeFragments delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  @Override
  public ClassName getTarget(RegisteredBean registeredBean) {
    return this.delegate.getTarget(registeredBean);
  }

  @Override
  public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
          ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

    return this.delegate.generateNewBeanDefinitionCode(generationContext,
            beanType, beanRegistrationCode);
  }

  @Override
  public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
          Predicate<String> attributeFilter) {

    return this.delegate.generateSetBeanDefinitionPropertiesCode(
            generationContext, beanRegistrationCode, beanDefinition, attributeFilter);
  }

  @Override
  public CodeBlock generateSetBeanInstanceSupplierCode(GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode, CodeBlock instanceSupplierCode,
          List<MethodReference> postProcessors) {

    return this.delegate.generateSetBeanInstanceSupplierCode(generationContext,
            beanRegistrationCode, instanceSupplierCode, postProcessors);
  }

  @Override
  public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {

    return this.delegate.generateInstanceSupplierCode(
            generationContext, beanRegistrationCode, allowDirectSupplierShortcut);
  }

  @Override
  public CodeBlock generateReturnCode(GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode) {

    return this.delegate.generateReturnCode(generationContext, beanRegistrationCode);
  }

}
