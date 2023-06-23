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

package cn.taketoday.aop.scope;

import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.TestBeanRegistrationsAotProcessor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.PropertiesFactoryBean;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import cn.taketoday.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 11:04
 */
class ScopedProxyBeanRegistrationAotProcessorTests {

  private final StandardBeanFactory beanFactory;

  private final TestBeanRegistrationsAotProcessor processor;

  private final TestGenerationContext generationContext;

  private final MockBeanFactoryInitializationCode beanFactoryInitializationCode;

  ScopedProxyBeanRegistrationAotProcessorTests() {
    this.beanFactory = new StandardBeanFactory();
    this.processor = new TestBeanRegistrationsAotProcessor();
    this.generationContext = new TestGenerationContext();
    this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);
  }

  @Test
  void scopedProxyBeanRegistrationAotProcessorIsRegistered() {
    assertThat(AotServices.factoriesAndBeans(this.beanFactory).load(BeanRegistrationAotProcessor.class))
            .anyMatch(ScopedProxyBeanRegistrationAotProcessor.class::isInstance);
  }

  @Test
  void getBeanRegistrationCodeGeneratorWhenNotScopedProxy() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(PropertiesFactoryBean.class).getBeanDefinition();
    this.beanFactory.registerBeanDefinition("test", beanDefinition);
    compile((freshBeanFactory, compiled) -> {
      Object bean = freshBeanFactory.getBean("test");
      assertThat(bean).isInstanceOf(Properties.class);
    });
  }

  @Test
  void getBeanRegistrationCodeGeneratorWhenScopedProxyWithoutTargetBeanName() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(ScopedProxyFactoryBean.class).getBeanDefinition();
    this.beanFactory.registerBeanDefinition("test", beanDefinition);
    compile((freshBeanFactory, compiled) ->
            assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    freshBeanFactory.getBean("test")).withMessageContaining("'targetBeanName' is required"));
  }

  @Test
  void getBeanRegistrationCodeGeneratorWhenScopedProxyWithInvalidTargetBeanName() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(ScopedProxyFactoryBean.class)
            .addPropertyValue("targetBeanName", "testDoesNotExist")
            .getBeanDefinition();
    this.beanFactory.registerBeanDefinition("test", beanDefinition);
    compile((freshBeanFactory, compiled) ->
            assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    freshBeanFactory.getBean("test")).withMessageContaining("No bean named 'testDoesNotExist'"));
  }

  @Test
  void getBeanRegistrationCodeGeneratorWhenScopedProxyWithTargetBeanName() {
    RootBeanDefinition targetBean = new RootBeanDefinition();
    targetBean.setTargetType(
            ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
    targetBean.setScope("custom");
    this.beanFactory.registerBeanDefinition("numberHolder", targetBean);
    BeanDefinition scopedBean = BeanDefinitionBuilder
            .rootBeanDefinition(ScopedProxyFactoryBean.class)
            .addPropertyValue("targetBeanName", "numberHolder").getBeanDefinition();
    this.beanFactory.registerBeanDefinition("test", scopedBean);
    compile((freshBeanFactory, compiled) -> {
      Object bean = freshBeanFactory.getBean("test");
      assertThat(bean).isInstanceOf(NumberHolder.class).isInstanceOf(AopInfrastructureBean.class);
    });
  }

  @SuppressWarnings("unchecked")
  private void compile(BiConsumer<StandardBeanFactory, Compiled> result) {
    BeanFactoryInitializationAotContribution contribution = processor.processAheadOfTime(beanFactory);
    assertThat(contribution).isNotNull();
    contribution.applyTo(generationContext, beanFactoryInitializationCode);
    MethodReference methodReference = beanFactoryInitializationCode
            .getInitializers().get(0);

    beanFactoryInitializationCode.getTypeBuilder().set(type -> {
      CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(
              ArgumentCodeGenerator.of(StandardBeanFactory.class, "beanFactory"),
              beanFactoryInitializationCode.getClassName());
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, StandardBeanFactory.class));
      type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
              .addParameter(StandardBeanFactory.class, "beanFactory")
              .addStatement(methodInvocation)
              .build());
    });

    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext).compile(compiled -> {
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      freshBeanFactory.setBeanClassLoader(compiled.getClassLoader());
      compiled.getInstance(Consumer.class).accept(freshBeanFactory);
      result.accept(freshBeanFactory, compiled);
    });
  }

}