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
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to
 * register bean definitions and aliases.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanRegistrationsAotProcessor
 * @since 4.0
 */
class BeanRegistrationsAotContribution implements BeanFactoryInitializationAotContribution {

  private static final String BEAN_FACTORY_PARAMETER_NAME = "beanFactory";

  private final List<Registration> registrations;

  BeanRegistrationsAotContribution(List<Registration> registrations) {
    this.registrations = registrations;
  }

  @Override
  public void applyTo(GenerationContext generationContext,
          BeanFactoryInitializationCode beanFactoryInitializationCode) {

    GeneratedClass generatedClass = generationContext.getGeneratedClasses()
            .addForFeature("BeanFactoryRegistrations", type -> {
              type.addJavadoc("Register bean definitions for the bean factory.");
              type.addModifiers(Modifier.PUBLIC);
            });
    BeanRegistrationsCodeGenerator codeGenerator = new BeanRegistrationsCodeGenerator(generatedClass);
    GeneratedMethod generatedBeanDefinitionsMethod = codeGenerator.getMethods().add("registerBeanDefinitions", method ->
            generateRegisterBeanDefinitionsMethod(method, generationContext, codeGenerator));
    beanFactoryInitializationCode.addInitializer(generatedBeanDefinitionsMethod.toMethodReference());
    GeneratedMethod generatedAliasesMethod = codeGenerator.getMethods().add("registerAliases",
            this::generateRegisterAliasesMethod);
    beanFactoryInitializationCode.addInitializer(generatedAliasesMethod.toMethodReference());
    generateRegisterHints(generationContext.getRuntimeHints(), this.registrations);
  }

  private void generateRegisterBeanDefinitionsMethod(MethodSpec.Builder method,
          GenerationContext generationContext, BeanRegistrationsCode beanRegistrationsCode) {

    method.addJavadoc("Register the bean definitions.");
    method.addModifiers(Modifier.PUBLIC);
    method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
    CodeBlock.Builder code = CodeBlock.builder();
    this.registrations.forEach(registration -> {
      try {
        MethodReference beanDefinitionMethod = registration.methodGenerator
                .generateBeanDefinitionMethod(generationContext, beanRegistrationsCode);
        CodeBlock methodInvocation = beanDefinitionMethod.toInvokeCodeBlock(
                ArgumentCodeGenerator.none(), beanRegistrationsCode.getClassName());
        code.addStatement("$L.registerBeanDefinition($S, $L)",
                BEAN_FACTORY_PARAMETER_NAME, registration.beanName(), methodInvocation);
      }
      catch (AotException ex) {
        throw ex;
      }
      catch (Exception ex) {
        throw new AotBeanProcessingException(registration.registeredBean,
                "failed to generate code for bean definition", ex);
      }
    });
    method.addCode(code.build());
  }

  private void generateRegisterAliasesMethod(MethodSpec.Builder method) {
    method.addJavadoc("Register the aliases.");
    method.addModifiers(Modifier.PUBLIC);
    method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
    CodeBlock.Builder code = CodeBlock.builder();
    this.registrations.forEach(registration -> {
      for (String alias : registration.aliases()) {
        code.addStatement("$L.registerAlias($S, $S)", BEAN_FACTORY_PARAMETER_NAME,
                registration.beanName(), alias);
      }
    });
    method.addCode(code.build());
  }

  private void generateRegisterHints(RuntimeHints runtimeHints, List<Registration> registrations) {
    registrations.forEach(registration -> {
      ReflectionHints hints = runtimeHints.reflection();
      Class<?> beanClass = registration.registeredBean.getBeanClass();
      hints.registerType(beanClass, MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INTROSPECT_DECLARED_METHODS);
      hints.registerForInterfaces(beanClass, typeHint -> typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
    });
  }

  /**
   * Gather the necessary information to register a particular bean.
   *
   * @param registeredBean the bean to register
   * @param methodGenerator the {@link BeanDefinitionMethodGenerator} to use
   * @param aliases the bean aliases, if any
   */
  record Registration(RegisteredBean registeredBean, BeanDefinitionMethodGenerator methodGenerator, String[] aliases) {

    String beanName() {
      return this.registeredBean.getBeanName();
    }

  }

  /**
   * {@link BeanRegistrationsCode} with generation support.
   */
  static class BeanRegistrationsCodeGenerator implements BeanRegistrationsCode {

    private final GeneratedClass generatedClass;

    public BeanRegistrationsCodeGenerator(GeneratedClass generatedClass) {
      this.generatedClass = generatedClass;
    }

    @Override
    public ClassName getClassName() {
      return this.generatedClass.getName();
    }

    @Override
    public GeneratedMethods getMethods() {
      return this.generatedClass.getMethods();
    }

  }

}
