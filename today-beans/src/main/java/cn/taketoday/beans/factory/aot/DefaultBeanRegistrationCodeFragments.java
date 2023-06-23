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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.aot.generate.AccessControl;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Internal {@link BeanRegistrationCodeFragments} implementation used by
 * default.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultBeanRegistrationCodeFragments implements BeanRegistrationCodeFragments {

  private final BeanRegistrationsCode beanRegistrationsCode;

  private final RegisteredBean registeredBean;

  private final BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory;

  DefaultBeanRegistrationCodeFragments(BeanRegistrationsCode beanRegistrationsCode,
          RegisteredBean registeredBean,
          BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory) {

    this.beanRegistrationsCode = beanRegistrationsCode;
    this.registeredBean = registeredBean;
    this.beanDefinitionMethodGeneratorFactory = beanDefinitionMethodGeneratorFactory;
  }

  @Override
  public ClassName getTarget(RegisteredBean registeredBean,
          Executable constructorOrFactoryMethod) {

    Class<?> target = extractDeclaringClass(registeredBean.getBeanType(), constructorOrFactoryMethod);
    while (target.getName().startsWith("java.") && registeredBean.isInnerBean()) {
      RegisteredBean parent = registeredBean.getParent();
      Assert.state(parent != null, "No parent available for inner bean");
      target = parent.getBeanClass();
    }
    return ClassName.get(target);
  }

  private Class<?> extractDeclaringClass(ResolvableType beanType, Executable executable) {
    Class<?> declaringClass = ClassUtils.getUserClass(executable.getDeclaringClass());
    if (executable instanceof Constructor<?>
            && AccessControl.forMember(executable).isPublic()
            && FactoryBean.class.isAssignableFrom(declaringClass)) {
      return extractTargetClassFromFactoryBean(declaringClass, beanType);
    }
    return executable.getDeclaringClass();
  }

  /**
   * Extract the target class of a public {@link FactoryBean} based on its
   * constructor. If the implementation does not resolve the target class
   * because it itself uses a generic, attempt to extract it from the
   * bean type.
   *
   * @param factoryBeanType the factory bean type
   * @param beanType the bean type
   * @return the target class to use
   */
  private Class<?> extractTargetClassFromFactoryBean(Class<?> factoryBeanType, ResolvableType beanType) {
    ResolvableType target = ResolvableType.forType(factoryBeanType).as(FactoryBean.class).getGeneric(0);
    if (target.getType().equals(Class.class)) {
      return target.toClass();
    }
    else if (factoryBeanType.isAssignableFrom(beanType.toClass())) {
      return beanType.as(FactoryBean.class).getGeneric(0).toClass();
    }
    return beanType.toClass();
  }

  @Override
  public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
          ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

    CodeBlock.Builder code = CodeBlock.builder();
    RootBeanDefinition mergedBeanDefinition = this.registeredBean.getMergedBeanDefinition();
    Class<?> beanClass = (mergedBeanDefinition.hasBeanClass()
                          ? ClassUtils.getUserClass(mergedBeanDefinition.getBeanClass()) : null);
    CodeBlock beanClassCode = generateBeanClassCode(
            beanRegistrationCode.getClassName().packageName(), beanClass);
    code.addStatement("$T $L = new $T($L)", RootBeanDefinition.class,
            BEAN_DEFINITION_VARIABLE, RootBeanDefinition.class, beanClassCode);
    if (targetTypeNecessary(beanType, beanClass)) {
      code.addStatement("$L.setTargetType($L)", BEAN_DEFINITION_VARIABLE,
              generateBeanTypeCode(beanType));
    }
    return code.build();
  }

  private CodeBlock generateBeanClassCode(String targetPackage, @Nullable Class<?> beanClass) {
    if (beanClass != null) {
      if (Modifier.isPublic(beanClass.getModifiers()) || targetPackage.equals(beanClass.getPackageName())) {
        return CodeBlock.of("$T.class", beanClass);
      }
      else {
        return CodeBlock.of("$S", beanClass.getName());
      }
    }
    return CodeBlock.of("");
  }

  private CodeBlock generateBeanTypeCode(ResolvableType beanType) {
    if (!beanType.hasGenerics()) {
      return CodeBlock.of("$T.class", ClassUtils.getUserClass(beanType.toClass()));
    }
    return ResolvableTypeCodeGenerator.generateCode(beanType);
  }

  private boolean targetTypeNecessary(ResolvableType beanType, @Nullable Class<?> beanClass) {
    if (beanType.hasGenerics() || beanClass == null) {
      return true;
    }
    return (!beanType.toClass().equals(beanClass)
            || this.registeredBean.getMergedBeanDefinition().getFactoryMethodName() != null);
  }

  @Override
  public CodeBlock generateSetBeanDefinitionPropertiesCode(
          GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
          Predicate<String> attributeFilter) {

    return new BeanDefinitionPropertiesCodeGenerator(
            generationContext.getRuntimeHints(), attributeFilter,
            beanRegistrationCode.getMethods(),
            (name, value) -> generateValueCode(generationContext, name, value))
            .generateCode(beanDefinition);
  }

  @Nullable
  protected CodeBlock generateValueCode(GenerationContext generationContext,
          String name, Object value) {

    RegisteredBean innerRegisteredBean = getInnerRegisteredBean(value);
    if (innerRegisteredBean != null) {
      BeanDefinitionMethodGenerator methodGenerator = this.beanDefinitionMethodGeneratorFactory
              .getBeanDefinitionMethodGenerator(innerRegisteredBean, name);
      Assert.state(methodGenerator != null, "Unexpected filtering of inner-bean");
      MethodReference generatedMethod = methodGenerator
              .generateBeanDefinitionMethod(generationContext, this.beanRegistrationsCode);
      return generatedMethod.toInvokeCodeBlock(ArgumentCodeGenerator.none());
    }
    return null;
  }

  @Nullable
  private RegisteredBean getInnerRegisteredBean(Object value) {
    if (value instanceof BeanDefinitionHolder beanDefinitionHolder) {
      return RegisteredBean.ofInnerBean(this.registeredBean, beanDefinitionHolder);
    }
    if (value instanceof BeanDefinition beanDefinition) {
      return RegisteredBean.ofInnerBean(this.registeredBean, beanDefinition);
    }
    return null;
  }

  @Override
  public CodeBlock generateSetBeanInstanceSupplierCode(
          GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode, CodeBlock instanceSupplierCode,
          List<MethodReference> postProcessors) {

    CodeBlock.Builder code = CodeBlock.builder();
    if (postProcessors.isEmpty()) {
      code.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE, instanceSupplierCode);
      return code.build();
    }
    code.addStatement("$T $L = $L",
            ParameterizedTypeName.get(InstanceSupplier.class, this.registeredBean.getBeanClass()),
            INSTANCE_SUPPLIER_VARIABLE, instanceSupplierCode);
    for (MethodReference postProcessor : postProcessors) {
      code.addStatement("$L = $L.andThen($L)", INSTANCE_SUPPLIER_VARIABLE,
              INSTANCE_SUPPLIER_VARIABLE, postProcessor.toCodeBlock());
    }
    code.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE,
            INSTANCE_SUPPLIER_VARIABLE);
    return code.build();
  }

  @Override
  public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode,
          Executable constructorOrFactoryMethod, boolean allowDirectSupplierShortcut) {

    return new InstanceSupplierCodeGenerator(generationContext,
            beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
            .generateCode(this.registeredBean, constructorOrFactoryMethod);
  }

  @Override
  public CodeBlock generateReturnCode(GenerationContext generationContext,
          BeanRegistrationCode beanRegistrationCode) {

    CodeBlock.Builder code = CodeBlock.builder();
    code.addStatement("return $L", BEAN_DEFINITION_VARIABLE);
    return code.build();
  }

}
