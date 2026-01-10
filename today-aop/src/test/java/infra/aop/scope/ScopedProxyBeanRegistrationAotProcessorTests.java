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

package infra.aop.scope;

import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import infra.aop.AopInfrastructureBean;
import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.aot.AotServices;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.TestBeanRegistrationsAotProcessor;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.PropertiesFactoryBean;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import infra.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import infra.core.ResolvableType;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.ParameterizedTypeName;

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