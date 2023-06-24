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

package cn.taketoday.context.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ResourcePatternHint;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.testfixture.context.annotation.CglibConfiguration;
import cn.taketoday.context.testfixture.context.annotation.ImportAwareConfiguration;
import cn.taketoday.context.testfixture.context.annotation.ImportConfiguration;
import cn.taketoday.context.testfixture.context.annotation.SimpleConfiguration;
import cn.taketoday.context.testfixture.context.generator.SimpleComponent;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.DefaultPropertySourceFactory;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationClassPostProcessor} AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ConfigurationClassPostProcessorAotContributionTests {

  private final TestGenerationContext generationContext = new TestGenerationContext();

  private final MockBeanFactoryInitializationCode beanFactoryInitializationCode =
          new MockBeanFactoryInitializationCode(this.generationContext);

  @Nested
  class ImportAwareTests {

    @Test
    void processAheadOfTimeWhenNoImportAwareConfigurationReturnsNull() {
      assertThat(getContribution(SimpleComponent.class)).isNull();
    }

    @Test
    void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorWithMapEntry() {
      BeanFactoryInitializationAotContribution contribution = getContribution(ImportConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        StandardBeanFactory freshBeanFactory = freshContext.getBeanFactory();
        initializer.accept(freshBeanFactory);
        freshContext.refresh();
        assertThat(freshBeanFactory.getBeanPostProcessors()).filteredOn(ImportAwareAotBeanPostProcessor.class::isInstance)
                .singleElement().satisfies(postProcessor -> assertPostProcessorEntry(postProcessor, ImportAwareConfiguration.class,
                        ImportConfiguration.class));
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorAfterApplicationContextAwareProcessor() {
      BeanFactoryInitializationAotContribution contribution = getContribution(TestAwareCallbackConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        StandardBeanFactory freshBeanFactory = freshContext.getBeanFactory();
        initializer.accept(freshBeanFactory);
        freshContext.registerBean(TestAwareCallbackBean.class);
        freshContext.refresh();
        TestAwareCallbackBean bean = freshContext.getBean(TestAwareCallbackBean.class);
        assertThat(bean.instances).hasSize(2);
        assertThat(bean.instances.get(0)).isEqualTo(freshContext);
        assertThat(bean.instances.get(1)).isInstanceOfSatisfying(AnnotationMetadata.class, metadata ->
                assertThat(metadata.getClassName()).isEqualTo(TestAwareCallbackConfiguration.class.getName()));
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorBeforeRegularBeanPostProcessor() {
      BeanFactoryInitializationAotContribution contribution = getContribution(
              TestImportAwareBeanPostProcessorConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        StandardBeanFactory freshBeanFactory = freshContext.getBeanFactory();
        initializer.accept(freshBeanFactory);
        freshBeanFactory.registerBeanDefinition(TestImportAwareBeanPostProcessor.class.getName(),
                new RootBeanDefinition(TestImportAwareBeanPostProcessor.class));
        RootBeanDefinition bd = new RootBeanDefinition(String.class);
        bd.setInstanceSupplier(() -> "test");
        freshBeanFactory.registerBeanDefinition("testProcessing", bd);
        freshContext.refresh();
        assertThat(freshContext.getBean("testProcessing")).isInstanceOfSatisfying(AnnotationMetadata.class, metadata ->
                assertThat(metadata.getClassName()).isEqualTo(TestImportAwareBeanPostProcessorConfiguration.class.getName())
        );
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasImportAwareConfigurationRegistersHints() {
      BeanFactoryInitializationAotContribution contribution = getContribution(ImportConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      assertThat(generationContext.getRuntimeHints().resources().resourcePatternHints())
              .singleElement()
              .satisfies(resourceHint -> assertThat(resourceHint.getIncludes())
                      .map(ResourcePatternHint::getPattern)
                      .containsExactlyInAnyOrder(
                              "/",
                              "cn",
                              "cn/taketoday",
                              "cn/taketoday/context",
                              "cn/taketoday/context/testfixture",
                              "cn/taketoday/context/testfixture/context",
                              "cn/taketoday/context/testfixture/context/annotation",
                              "cn/taketoday/context/testfixture/context/annotation/ImportConfiguration.class"
                      ));
    }

    @SuppressWarnings("unchecked")
    private void compile(BiConsumer<Consumer<StandardBeanFactory>, Compiled> result) {
      MethodReference methodReference = beanFactoryInitializationCode.getInitializers().get(0);
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
      TestCompiler.forSystem().with(generationContext).compile(compiled ->
              result.accept(compiled.getInstance(Consumer.class), compiled));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TestAwareCallbackBean.class)
    static class TestAwareCallbackConfiguration {
    }

    static class TestAwareCallbackBean implements ImportAware, ApplicationContextAware {

      private final List<Object> instances = new ArrayList<>();

      @Override
      public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.instances.add(applicationContext);
      }

      @Override
      public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.instances.add(importMetadata);
      }

    }

    @Configuration(proxyBeanMethods = false)
    @Import(TestImportAwareBeanPostProcessor.class)
    static class TestImportAwareBeanPostProcessorConfiguration {
    }

    static class TestImportAwareBeanPostProcessor implements InitializationBeanPostProcessor, ImportAware,
            Ordered, InitializingBean {

      private AnnotationMetadata metadata;

      @Override
      public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.metadata = importMetadata;
      }

      @Nullable
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals("testProcessing")) {
          return this.metadata;
        }
        return bean;
      }

      @Override
      public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
      }

      @Override
      public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.metadata, "Metadata was not injected");
      }

    }
  }

  @Nested
  class PropertySourceTests {

    @Test
    void applyToWhenHasPropertySourceInvokePropertySourceProcessor() {
      BeanFactoryInitializationAotContribution contribution = getContribution(
              PropertySourceConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      assertThat(resource("cn/taketoday/context/annotation/p1.properties"))
              .accepts(generationContext.getRuntimeHints());
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        ConfigurableEnvironment environment = freshContext.getEnvironment();
        assertThat(environment.containsProperty("from.p1")).isFalse();
        initializer.accept(freshContext);
        assertThat(environment.containsProperty("from.p1")).isTrue();
        assertThat(environment.getProperty("from.p1")).isEqualTo("p1Value");
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasPropertySourcesInvokesPropertySourceProcessorInOrder() {
      BeanFactoryInitializationAotContribution contribution = getContribution(
              PropertySourceConfiguration.class, PropertySourceDependentConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      assertThat(resource("cn/taketoday/context/annotation/p1.properties")
              .and(resource("cn/taketoday/context/annotation/p2.properties")))
              .accepts(generationContext.getRuntimeHints());
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        ConfigurableEnvironment environment = freshContext.getEnvironment();
        assertThat(environment.containsProperty("from.p1")).isFalse();
        assertThat(environment.containsProperty("from.p2")).isFalse();
        initializer.accept(freshContext);
        assertThat(environment.containsProperty("from.p1")).isTrue();
        assertThat(environment.getProperty("from.p1")).isEqualTo("p1Value");
        assertThat(environment.containsProperty("from.p2")).isTrue();
        assertThat(environment.getProperty("from.p2")).isEqualTo("p2Value");
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasPropertySourceWithDetailsRetainsThem() {
      BeanFactoryInitializationAotContribution contribution = getContribution(
              PropertySourceWithDetailsConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        ConfigurableEnvironment environment = freshContext.getEnvironment();
        assertThat(environment.getPropertySources().get("testp1")).isNull();
        initializer.accept(freshContext);
        assertThat(environment.getPropertySources().get("testp1")).isNotNull();
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasCustomFactoryRegistersHints() {
      BeanFactoryInitializationAotContribution contribution = getContribution(
              PropertySourceWithCustomFactoryConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      assertThat(RuntimeHintsPredicates.reflection().onType(CustomPropertySourcesFactory.class)
              .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
              .accepts(generationContext.getRuntimeHints());
    }

    private Predicate<RuntimeHints> resource(String location) {
      return RuntimeHintsPredicates.resource().forResource(location);
    }

    @SuppressWarnings("unchecked")
    private void compile(BiConsumer<Consumer<GenericApplicationContext>, Compiled> result) {
      MethodReference methodReference = beanFactoryInitializationCode.getInitializers().get(0);
      beanFactoryInitializationCode.getTypeBuilder().set(type -> {
        ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator
                .of(ConfigurableEnvironment.class, "applicationContext.getEnvironment()")
                .and(ArgumentCodeGenerator.of(ResourceLoader.class, "applicationContext"));
        CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(argCodeGenerator,
                beanFactoryInitializationCode.getClassName());
        type.addModifiers(Modifier.PUBLIC);
        type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, GenericApplicationContext.class));
        type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
                .addParameter(GenericApplicationContext.class, "applicationContext")
                .addStatement(methodInvocation)
                .build());
      });
      generationContext.writeGeneratedContent();
      TestCompiler.forSystem().with(generationContext).compile(compiled ->
              result.accept(compiled.getInstance(Consumer.class), compiled));
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties")
    static class PropertySourceConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:${base.package}/p2.properties")
    static class PropertySourceDependentConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource(name = "testp1", value = "classpath:cn/taketoday/context/annotation/p1.properties",
                    ignoreResourceNotFound = true)
    static class PropertySourceWithDetailsConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource(value = "classpath:cn/taketoday/context/annotation/p1.properties",
                    factory = CustomPropertySourcesFactory.class)
    static class PropertySourceWithCustomFactoryConfiguration {

    }

  }

  @Nested
  class ConfigurationClassProxyTests {

    private final StandardBeanFactory beanFactory = new StandardBeanFactory();

    private final ConfigurationClassPostProcessor processor = new ConfigurationClassPostProcessor();

    @Test
    void processAheadOfTimeRegularConfigurationClass() {
      assertThat(this.processor.processAheadOfTime(
              getRegisteredBean(SimpleConfiguration.class))).isNull();
    }

    @Test
    void processAheadOfTimeFullConfigurationClass() {
      assertThat(this.processor.processAheadOfTime(
              getRegisteredBean(CglibConfiguration.class))).isNotNull();
    }

    private RegisteredBean getRegisteredBean(Class<?> bean) {
      this.beanFactory.registerBeanDefinition("test", new RootBeanDefinition(bean));
      this.processor.postProcessBeanFactory(this.beanFactory);
      return RegisteredBean.of(this.beanFactory, "test");
    }

  }

  @Nullable
  private BeanFactoryInitializationAotContribution getContribution(Class<?>... types) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    for (Class<?> type : types) {
      beanFactory.registerBeanDefinition(type.getName(), new RootBeanDefinition(type));
    }
    ConfigurationClassPostProcessor postProcessor = new ConfigurationClassPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);
    return postProcessor.processAheadOfTime(beanFactory);
  }

  private void assertPostProcessorEntry(BeanPostProcessor postProcessor, Class<?> key, Class<?> value) {
    assertThat(postProcessor).extracting("importsMapping")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(entry(key.getName(), value.getName()));
  }

  static class CustomPropertySourcesFactory extends DefaultPropertySourceFactory {

  }

}
