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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.beans.factory.support.RegisteredBean;
import infra.lang.Assert;

/**
 * {@link BeanRegistrationCode} implementation with code generation support.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanRegistrationCodeGenerator implements BeanRegistrationCode {

  private static final Predicate<String> REJECT_ALL_ATTRIBUTES_FILTER = attribute -> false;

  private final ClassName className;

  private final GeneratedMethods generatedMethods;

  private final List<MethodReference> instancePostProcessors = new ArrayList<>();

  private final RegisteredBean registeredBean;

  private final BeanRegistrationCodeFragments codeFragments;

  BeanRegistrationCodeGenerator(ClassName className, GeneratedMethods generatedMethods,
          RegisteredBean registeredBean, BeanRegistrationCodeFragments codeFragments) {

    this.className = className;
    this.generatedMethods = generatedMethods;
    this.registeredBean = registeredBean;
    this.codeFragments = codeFragments;
  }

  @Override
  public ClassName getClassName() {
    return this.className;
  }

  @Override
  public GeneratedMethods getMethods() {
    return this.generatedMethods;
  }

  @Override
  public void addInstancePostProcessor(MethodReference methodReference) {
    Assert.notNull(methodReference, "'methodReference' is required");
    this.instancePostProcessors.add(methodReference);
  }

  CodeBlock generateCode(GenerationContext generationContext) {
    CodeBlock.Builder code = CodeBlock.builder();
    code.add(this.codeFragments.generateNewBeanDefinitionCode(generationContext,
            this.registeredBean.getBeanType(), this));
    code.add(this.codeFragments.generateSetBeanDefinitionPropertiesCode(
            generationContext, this, this.registeredBean.getMergedBeanDefinition(),
            REJECT_ALL_ATTRIBUTES_FILTER));
    CodeBlock instanceSupplierCode = this.codeFragments.generateInstanceSupplierCode(
            generationContext, this, this.instancePostProcessors.isEmpty());
    code.add(this.codeFragments.generateSetBeanInstanceSupplierCode(generationContext,
            this, instanceSupplierCode, this.instancePostProcessors));
    code.add(this.codeFragments.generateReturnCode(generationContext, this));
    return code.build();
  }

}
