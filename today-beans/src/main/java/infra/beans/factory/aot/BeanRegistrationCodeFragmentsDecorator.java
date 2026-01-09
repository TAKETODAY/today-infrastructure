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

package infra.beans.factory.aot;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.ResolvableType;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
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
