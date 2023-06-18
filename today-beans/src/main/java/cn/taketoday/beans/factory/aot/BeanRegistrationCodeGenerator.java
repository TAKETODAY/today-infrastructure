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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.lang.Assert;

/**
 * {@link BeanRegistrationCode} implementation with code generation support.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class BeanRegistrationCodeGenerator implements BeanRegistrationCode {

  private static final Predicate<String> REJECT_ALL_ATTRIBUTES_FILTER = attribute -> false;

  private final ClassName className;

  private final GeneratedMethods generatedMethods;

  private final List<MethodReference> instancePostProcessors = new ArrayList<>();

  private final RegisteredBean registeredBean;

  private final Executable constructorOrFactoryMethod;

  private final BeanRegistrationCodeFragments codeFragments;

  BeanRegistrationCodeGenerator(ClassName className, GeneratedMethods generatedMethods,
          RegisteredBean registeredBean, Executable constructorOrFactoryMethod,
          BeanRegistrationCodeFragments codeFragments) {

    this.className = className;
    this.generatedMethods = generatedMethods;
    this.registeredBean = registeredBean;
    this.constructorOrFactoryMethod = constructorOrFactoryMethod;
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
    Assert.notNull(methodReference, "'methodReference' must not be null");
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
            generationContext, this, this.constructorOrFactoryMethod,
            this.instancePostProcessors.isEmpty());
    code.add(this.codeFragments.generateSetBeanInstanceSupplierCode(generationContext,
            this, instanceSupplierCode, this.instancePostProcessors));
    code.add(this.codeFragments.generateReturnCode(generationContext, this));
    return code.build();
  }

}
