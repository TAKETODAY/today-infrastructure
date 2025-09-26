/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ResourcePatternHint;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.BeanRegistry;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.support.GenericApplicationContext;
import infra.context.testfixture.context.annotation.CglibConfiguration;
import infra.context.testfixture.context.annotation.ImportAwareConfiguration;
import infra.context.testfixture.context.annotation.ImportConfiguration;
import infra.context.testfixture.context.annotation.SimpleConfiguration;
import infra.context.testfixture.context.generator.SimpleComponent;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.DefaultPropertySourceFactory;
import infra.core.io.ResourceLoader;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.core.type.AnnotationMetadata;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.ParameterizedTypeName;
import infra.lang.Assert;
import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationClassPostProcessor} AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ConfigurationClassPostProcessorAotContributionTests {

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
                              "infra",
                              "infra/context",
                              "infra/context/testfixture",
                              "infra/context/testfixture/context",
                              "infra/context/testfixture/context/annotation",
                              "infra/context/testfixture/context/annotation/ImportConfiguration.class"
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
      assertThat(resource("infra/context/annotation/p1.properties"))
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
    void propertySourceWithClassPathStarLocationPattern() {
      BeanFactoryInitializationAotContribution contribution =
              getContribution(PropertySourceWithClassPathStarLocationPatternConfiguration.class);

      // We can effectively only assert that an exception is not thrown; however,
      // a WARN-level log message similar to the following should be logged.
      //
      // Runtime hint registration is not supported for the 'classpath*:' prefix or wildcards
      // in @PropertySource locations. Please manually register a resource hint for each property
      // source location represented by 'classpath*:infra/context/annotation/*.properties'.
      assertThatNoException().isThrownBy(() -> contribution.applyTo(generationContext, beanFactoryInitializationCode));

      // But we can also ensure that a resource hint was not registered.
      assertThat(resource("infra/context/annotation/p1.properties"))
              .rejects(generationContext.getRuntimeHints());
    }

    @Test
    void propertySourceWithWildcardLocationPattern() {
      BeanFactoryInitializationAotContribution contribution =
              getContribution(PropertySourceWithWildcardLocationPatternConfiguration.class);

      // We can effectively only assert that an exception is not thrown; however,
      // a WARN-level log message similar to the following should be logged.
      //
      // Runtime hint registration is not supported for the 'classpath*:' prefix or wildcards
      // in @PropertySource locations. Please manually register a resource hint for each property
      // source location represented by 'classpath:infra/context/annotation/p?.properties'.
      assertThatNoException().isThrownBy(() -> contribution.applyTo(generationContext, beanFactoryInitializationCode));

      // But we can also ensure that a resource hint was not registered.
      assertThat(resource("infra/context/annotation/p1.properties"))
              .rejects(generationContext.getRuntimeHints());
    }

    @Test
    void applyToWhenHasPropertySourcesInvokesPropertySourceProcessorInOrder() {
      BeanFactoryInitializationAotContribution contribution = getContribution(
              PropertySourceConfiguration.class, PropertySourceDependentConfiguration.class);
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      assertThat(resource("infra/context/annotation/p1.properties")
              .and(resource("infra/context/annotation/p2.properties")))
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
    @PropertySource("classpath:infra/context/annotation/p1.properties")
    static class PropertySourceConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:${base.package}/p2.properties")
    static class PropertySourceDependentConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource(name = "testp1", value = "classpath:infra/context/annotation/p1.properties",
            ignoreResourceNotFound = true)
    static class PropertySourceWithDetailsConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource(value = "classpath:infra/context/annotation/p1.properties",
            factory = CustomPropertySourcesFactory.class)
    static class PropertySourceWithCustomFactoryConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath*:infra/context/annotation/*.properties")
    static class PropertySourceWithClassPathStarLocationPatternConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:infra/context/annotation/p?.properties")
    static class PropertySourceWithWildcardLocationPatternConfiguration {
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

  @Nested
  public class BeanRegistrarTests {

    @Test
    void applyToWhenHasDefaultConstructor() throws NoSuchMethodException {
      BeanFactoryInitializationAotContribution contribution = getContribution(DefaultConstructorConfiguration.class);
      assertThat(contribution).isNotNull();
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      Constructor<Foo> fooConstructor = Foo.class.getDeclaredConstructor();
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        initializer.accept(freshContext);
        freshContext.refresh();
        assertThat(freshContext.getBean(Foo.class)).isNotNull();
        assertThat(RuntimeHintsPredicates.reflection().onConstructorInvocation(fooConstructor))
                .accepts(generationContext.getRuntimeHints());
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasInstanceSupplier() {
      BeanFactoryInitializationAotContribution contribution = getContribution(InstanceSupplierConfiguration.class);
      assertThat(contribution).isNotNull();
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        initializer.accept(freshContext);
        freshContext.refresh();
        assertThat(freshContext.getBean(Foo.class)).isNotNull();
        assertThat(generationContext.getRuntimeHints().reflection().getTypeHint(Foo.class)).isNull();
        freshContext.close();
      });
    }

    @Test
    void applyToWhenHasPostConstructAnnotationPostProcessed() {
      BeanFactoryInitializationAotContribution contribution = getContribution(CommonAnnotationBeanPostProcessor.class,
              PostConstructConfiguration.class);
      assertThat(contribution).isNotNull();
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        initializer.accept(freshContext);
        freshContext.refresh();
        Init init = freshContext.getBean(Init.class);
        assertThat(init).isNotNull();
        assertThat(init.initialized).isTrue();
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(Init.class, "postConstruct"))
                .accepts(generationContext.getRuntimeHints());
        freshContext.close();
      });
    }

    @Test
    void applyToWhenIsImportAware() {
      BeanFactoryInitializationAotContribution contribution = getContribution(CommonAnnotationBeanPostProcessor.class,
              ImportAwareBeanRegistrarConfiguration.class);
      assertThat(contribution).isNotNull();
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
      compile((initializer, compiled) -> {
        GenericApplicationContext freshContext = new GenericApplicationContext();
        initializer.accept(freshContext);
        freshContext.refresh();
        assertThat(freshContext.getBean(ClassNameHolder.class).className())
                .isEqualTo(ImportAwareBeanRegistrarConfiguration.class.getName());
        freshContext.close();
      });
    }

    private void compile(BiConsumer<Consumer<GenericApplicationContext>, Compiled> result) {
      MethodReference methodReference = beanFactoryInitializationCode.getInitializers().get(0);
      beanFactoryInitializationCode.getTypeBuilder().set(type -> {
        ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator
                .of(BeanFactory.class, "applicationContext.getBeanFactory()")
                .and(ArgumentCodeGenerator.of(Environment.class, "applicationContext.getEnvironment()"));
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

    @Configuration
    @Import(DefaultConstructorBeanRegistrar.class)
    public static class DefaultConstructorConfiguration {
    }

    public static class DefaultConstructorBeanRegistrar implements BeanRegistrar {

      @Override
      public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(Foo.class);
      }
    }

    @Configuration
    @Import(InstanceSupplierBeanRegistrar.class)
    public static class InstanceSupplierConfiguration {
    }

    public static class InstanceSupplierBeanRegistrar implements BeanRegistrar {

      @Override
      public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(Foo.class, spec -> spec.supplier(context -> new Foo()));
      }
    }

    @Configuration
    @Import(PostConstructBeanRegistrar.class)
    public static class PostConstructConfiguration {
    }

    public static class PostConstructBeanRegistrar implements BeanRegistrar {

      @Override
      public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(Init.class);
      }
    }

    @Import(ImportAwareBeanRegistrar.class)
    public static class ImportAwareBeanRegistrarConfiguration {
    }

    public static class ImportAwareBeanRegistrar implements BeanRegistrar, ImportAware {

      @Nullable
      private AnnotationMetadata importMetadata;

      @Override
      public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(ClassNameHolder.class, spec -> spec.supplier(context ->
                new ClassNameHolder(this.importMetadata == null ? null : this.importMetadata.getClassName())));
      }

      @Override
      public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.importMetadata = importMetadata;
      }

      public @Nullable AnnotationMetadata getImportMetadata() {
        return this.importMetadata;
      }
    }

    static class Foo {
    }

    static class Init {

      boolean initialized = false;

      @PostConstruct
      void postConstruct() {
        initialized = true;
      }
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

  public record ClassNameHolder(@Nullable String className) { }

  static class CustomPropertySourcesFactory extends DefaultPropertySourceFactory {

  }

}
