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

package cn.taketoday.beans.factory.aot;

import java.util.List;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Generates a method that returns a {@link BeanDefinition} to be registered.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinitionMethodGeneratorFactory
 * @since 4.0
 */
class BeanDefinitionMethodGenerator {

  private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

  private final RegisteredBean registeredBean;

  @Nullable
  private final String currentPropertyName;

  private final List<BeanRegistrationAotContribution> aotContributions;

  /**
   * Create a new {@link BeanDefinitionMethodGenerator} instance.
   *
   * @param methodGeneratorFactory the method generator factory
   * @param registeredBean the registered bean
   * @param currentPropertyName the current property name
   * @param aotContributions the AOT contributions
   * @throws IllegalArgumentException if the bean definition defines an instance supplier since this can't be supported for code generation
   */
  BeanDefinitionMethodGenerator(BeanDefinitionMethodGeneratorFactory methodGeneratorFactory,
          RegisteredBean registeredBean, @Nullable String currentPropertyName,
          List<BeanRegistrationAotContribution> aotContributions) {

    this.methodGeneratorFactory = methodGeneratorFactory;
    this.registeredBean = registeredBean;
    this.currentPropertyName = currentPropertyName;
    this.aotContributions = aotContributions;
  }

  /**
   * Generate the method that returns the {@link BeanDefinition} to be registered.
   *
   * @param generationContext the generation context
   * @param beanRegistrationsCode the bean registrations code
   * @return a reference to the generated method.
   */
  MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
          BeanRegistrationsCode beanRegistrationsCode) {

    BeanRegistrationCodeFragments codeFragments = getCodeFragments(generationContext, beanRegistrationsCode);
    ClassName target = codeFragments.getTarget(this.registeredBean);
    if (isWritablePackageName(target)) {
      GeneratedClass generatedClass = lookupGeneratedClass(generationContext, target);
      GeneratedMethods generatedMethods = generatedClass.getMethods().withPrefix(getName());
      GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext,
              generatedClass.getName(), generatedMethods, codeFragments, Modifier.PUBLIC);
      return generatedMethod.toMethodReference();
    }
    GeneratedMethods generatedMethods = beanRegistrationsCode.getMethods().withPrefix(getName());
    GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext,
            beanRegistrationsCode.getClassName(), generatedMethods, codeFragments, Modifier.PRIVATE);
    return generatedMethod.toMethodReference();
  }

  /**
   * Specify if the {@link ClassName} belongs to a writable package.
   *
   * @param target the target to check
   * @return {@code true} if generated code in that package is allowed
   */
  private boolean isWritablePackageName(ClassName target) {
    String packageName = target.packageName();
    return (!packageName.startsWith("java.") && !packageName.startsWith("javax."));
  }

  /**
   * Return the {@link GeneratedClass} to use for the specified {@code target}.
   * <p>If the target class is an inner class, a corresponding inner class in
   * the original structure is created.
   *
   * @param generationContext the generation context to use
   * @param target the chosen target class name for the bean definition
   * @return the generated class to use
   */
  private static GeneratedClass lookupGeneratedClass(GenerationContext generationContext, ClassName target) {
    ClassName topLevelClassName = target.topLevelClassName();
    GeneratedClass generatedClass = generationContext.getGeneratedClasses()
            .getOrAddForFeatureComponent("BeanDefinitions", topLevelClassName, type -> {
              type.addJavadoc("Bean definitions for {@link $T}.", topLevelClassName);
              type.addModifiers(Modifier.PUBLIC);
            });

    List<String> names = target.simpleNames();
    if (names.size() == 1) {
      return generatedClass;
    }

    List<String> namesToProcess = names.subList(1, names.size());
    ClassName currentTargetClassName = topLevelClassName;
    GeneratedClass tmp = generatedClass;
    for (String nameToProcess : namesToProcess) {
      currentTargetClassName = currentTargetClassName.nestedClass(nameToProcess);
      tmp = createInnerClass(tmp, nameToProcess, currentTargetClassName);
    }
    return tmp;
  }

  private static GeneratedClass createInnerClass(GeneratedClass generatedClass, String name, ClassName target) {
    return generatedClass.getOrAdd(name, type -> {
      type.addJavadoc("Bean definitions for {@link $T}.", target);
      type.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    });
  }

  private BeanRegistrationCodeFragments getCodeFragments(GenerationContext generationContext,
          BeanRegistrationsCode beanRegistrationsCode) {

    BeanRegistrationCodeFragments codeFragments = new DefaultBeanRegistrationCodeFragments(
            beanRegistrationsCode, this.registeredBean, this.methodGeneratorFactory);
    for (BeanRegistrationAotContribution aotContribution : this.aotContributions) {
      codeFragments = aotContribution.customizeBeanRegistrationCodeFragments(generationContext, codeFragments);
    }
    return codeFragments;
  }

  private GeneratedMethod generateBeanDefinitionMethod(GenerationContext generationContext,
          ClassName className, GeneratedMethods generatedMethods,
          BeanRegistrationCodeFragments codeFragments, Modifier modifier) {

    BeanRegistrationCodeGenerator codeGenerator = new BeanRegistrationCodeGenerator(
            className, generatedMethods, this.registeredBean, codeFragments);

    this.aotContributions.forEach(aotContribution -> aotContribution.applyTo(generationContext, codeGenerator));

    CodeWarnings codeWarnings = new CodeWarnings();
    codeWarnings.detectDeprecation(this.registeredBean.getBeanType());
    return generatedMethods.add("getBeanDefinition", method -> {
      method.addJavadoc("Get the $L definition for '$L'.",
              (this.registeredBean.isInnerBean() ? "inner-bean" : "bean"),
              getName());
      method.addModifiers(modifier, Modifier.STATIC);
      codeWarnings.suppress(method);
      method.returns(BeanDefinition.class);
      method.addCode(codeGenerator.generateCode(generationContext));
    });
  }

  private String getName() {
    if (this.currentPropertyName != null) {
      return this.currentPropertyName;
    }
    if (!this.registeredBean.isGeneratedBeanName()) {
      return getSimpleBeanName(this.registeredBean.getBeanName());
    }
    RegisteredBean nonGeneratedParent = this.registeredBean;
    while (nonGeneratedParent != null && nonGeneratedParent.isGeneratedBeanName()) {
      nonGeneratedParent = nonGeneratedParent.getParent();
    }
    if (nonGeneratedParent != null) {
      return getSimpleBeanName(nonGeneratedParent.getBeanName()) + "InnerBean";
    }
    return "innerBean";
  }

  private String getSimpleBeanName(String beanName) {
    int lastDot = beanName.lastIndexOf('.');
    beanName = (lastDot != -1 ? beanName.substring(lastDot + 1) : beanName);
    int lastDollar = beanName.lastIndexOf('$');
    beanName = (lastDollar != -1 ? beanName.substring(lastDollar + 1) : beanName);
    return StringUtils.uncapitalize(beanName);
  }

}
