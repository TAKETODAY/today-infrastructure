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

package infra.context.aot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import infra.aot.generate.GeneratedFiles.Kind;
import infra.aot.generate.GenerationContext;
import infra.aot.hint.JdkProxyHint;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeReference;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.BeansException;
import infra.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import infra.beans.factory.aot.AotProcessingException;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.Employee;
import infra.beans.testfixture.beans.Pet;
import infra.beans.testfixture.beans.factory.aot.TestHierarchy;
import infra.beans.testfixture.beans.factory.aot.TestHierarchy.Implementation;
import infra.beans.testfixture.beans.factory.aot.TestHierarchy.One;
import infra.beans.testfixture.beans.factory.aot.TestHierarchy.Two;
import infra.context.ApplicationContextInitializer;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.CommonAnnotationBeanPostProcessor;
import infra.context.annotation.ContextAnnotationAutowireCandidateResolver;
import infra.context.support.GenericApplicationContext;
import infra.context.support.GenericXmlApplicationContext;
import infra.context.support.MockEnvironment;
import infra.context.testfixture.context.annotation.AutowiredCglibConfiguration;
import infra.context.testfixture.context.annotation.AutowiredComponent;
import infra.context.testfixture.context.annotation.AutowiredGenericTemplate;
import infra.context.testfixture.context.annotation.AutowiredMixedCglibConfiguration;
import infra.context.testfixture.context.annotation.CglibConfiguration;
import infra.context.testfixture.context.annotation.ConfigurableCglibConfiguration;
import infra.context.testfixture.context.annotation.GenericTemplateConfiguration;
import infra.context.testfixture.context.annotation.InitDestroyComponent;
import infra.context.testfixture.context.annotation.InjectionPointConfiguration;
import infra.context.testfixture.context.annotation.LazyAutowiredFieldComponent;
import infra.context.testfixture.context.annotation.LazyAutowiredMethodComponent;
import infra.context.testfixture.context.annotation.LazyConstructorArgumentComponent;
import infra.context.testfixture.context.annotation.LazyFactoryMethodArgumentComponent;
import infra.context.testfixture.context.annotation.LazyResourceFieldComponent;
import infra.context.testfixture.context.annotation.LazyResourceMethodComponent;
import infra.context.testfixture.context.annotation.PropertySourceConfiguration;
import infra.context.testfixture.context.annotation.QualifierConfiguration;
import infra.context.testfixture.context.annotation.ResourceComponent;
import infra.context.testfixture.context.annotation.ValueCglibConfiguration;
import infra.context.testfixture.context.generator.SimpleComponent;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.PropertySource;
import infra.core.io.ClassPathResource;
import infra.core.io.ResourceLoader;
import infra.core.test.tools.CompileWithForkedClassLoader;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ApplicationContextAotGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ApplicationContextAotGeneratorTests {

  @Test
  void processAheadOfTimeWhenHasSimpleBean() {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    applicationContext.registerBeanDefinition("test", new RootBeanDefinition(SimpleComponent.class));
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("test");
      assertThat(freshApplicationContext.getBean("test")).isInstanceOf(SimpleComponent.class);
    });
  }

  @Nested
  class Autowiring {

    @Test
    void processAheadOfTimeWhenHasAutowiring() {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      registerBeanPostProcessor(applicationContext,
              AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, AutowiredAnnotationBeanPostProcessor.class);
      applicationContext.registerBeanDefinition("autowiredComponent", new RootBeanDefinition(AutowiredComponent.class));
      registerIntegerBean(applicationContext, "number", 42);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("autowiredComponent", "number");
        AutowiredComponent bean = freshApplicationContext.getBean(AutowiredComponent.class);
        assertThat(bean.getEnvironment()).isSameAs(freshApplicationContext.getEnvironment());
        assertThat(bean.getCounter()).isEqualTo(42);
      });
    }

    @Test
    void processAheadOfTimeWhenHasAutowiringOnUnresolvedGeneric() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(GenericTemplateConfiguration.class);
      applicationContext.registerBean("autowiredComponent", AutowiredGenericTemplate.class);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        AutowiredGenericTemplate bean = freshApplicationContext.getBean(AutowiredGenericTemplate.class);
        assertThat(bean).hasFieldOrPropertyWithValue("genericTemplate", applicationContext.getBean("genericTemplate"));
      });
    }

    @Test
    void processAheadOfTimeWhenHasLazyAutowiringOnField() {
      testAutowiredComponent(LazyAutowiredFieldComponent.class, (bean, generationContext) -> {
        Environment environment = bean.getEnvironment();
        assertThat(environment).isInstanceOf(Proxy.class);
        ResourceLoader resourceLoader = bean.getResourceLoader();
        assertThat(resourceLoader).isNotInstanceOf(Proxy.class);
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        assertThat(runtimeHints.proxies().jdkProxyHints()).satisfies(doesNotHaveProxyFor(ResourceLoader.class));
        assertThat(runtimeHints.proxies().jdkProxyHints()).anySatisfy(proxyHint ->
                assertThat(proxyHint.getProxiedInterfaces()).isEqualTo(TypeReference.listOf(
                        environment.getClass().getInterfaces())));

      });
    }

    @Test
    void processAheadOfTimeWhenHasLazyAutowiringOnMethod() {
      testAutowiredComponent(LazyAutowiredMethodComponent.class, (bean, generationContext) -> {
        Environment environment = bean.getEnvironment();
        assertThat(environment).isNotInstanceOf(Proxy.class);
        ResourceLoader resourceLoader = bean.getResourceLoader();
        assertThat(resourceLoader).isInstanceOf(Proxy.class);
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        assertThat(runtimeHints.proxies().jdkProxyHints()).satisfies(doesNotHaveProxyFor(Environment.class));
        assertThat(runtimeHints.proxies().jdkProxyHints()).anySatisfy(proxyHint ->
                assertThat(proxyHint.getProxiedInterfaces()).isEqualTo(TypeReference.listOf(
                        resourceLoader.getClass().getInterfaces())));
      });
    }

    @Test
    void processAheadOfTimeWhenHasLazyAutowiringOnConstructor() {
      testAutowiredComponent(LazyConstructorArgumentComponent.class, (bean, generationContext) -> {
        Environment environment = bean.getEnvironment();
        assertThat(environment).isInstanceOf(Proxy.class);
        ResourceLoader resourceLoader = bean.getResourceLoader();
        assertThat(resourceLoader).isNotInstanceOf(Proxy.class);
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        assertThat(runtimeHints.proxies().jdkProxyHints()).satisfies(doesNotHaveProxyFor(ResourceLoader.class));
        assertThat(runtimeHints.proxies().jdkProxyHints()).anySatisfy(proxyHint ->
                assertThat(proxyHint.getProxiedInterfaces()).isEqualTo(TypeReference.listOf(
                        environment.getClass().getInterfaces())));
      });
    }

    @Test
    void processAheadOfTimeWhenHasLazyAutowiringOnFactoryMethod() {
      RootBeanDefinition bd = new RootBeanDefinition(LazyFactoryMethodArgumentComponent.class);
      bd.setFactoryMethodName("of");
      testAutowiredComponent(LazyFactoryMethodArgumentComponent.class, bd, (bean, generationContext) -> {
        Environment environment = bean.getEnvironment();
        assertThat(environment).isInstanceOf(Proxy.class);
        ResourceLoader resourceLoader = bean.getResourceLoader();
        assertThat(resourceLoader).isNotInstanceOf(Proxy.class);
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        assertThat(runtimeHints.proxies().jdkProxyHints()).satisfies(doesNotHaveProxyFor(ResourceLoader.class));
        assertThat(runtimeHints.proxies().jdkProxyHints()).anySatisfy(proxyHint ->
                assertThat(proxyHint.getProxiedInterfaces()).isEqualTo(TypeReference.listOf(
                        environment.getClass().getInterfaces())));
      });
    }

    private <T> void testAutowiredComponent(Class<T> type, BiConsumer<T, GenerationContext> assertions) {
      testAutowiredComponent(type, new RootBeanDefinition(type), assertions);
    }

    private <T> void testAutowiredComponent(Class<T> type, RootBeanDefinition beanDefinition,
            BiConsumer<T, GenerationContext> assertions) {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      applicationContext.getBeanFactory().setAutowireCandidateResolver(
              new ContextAnnotationAutowireCandidateResolver());
      registerBeanPostProcessor(applicationContext,
              AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, AutowiredAnnotationBeanPostProcessor.class);
      applicationContext.registerBeanDefinition("testComponent", beanDefinition);
      TestGenerationContext generationContext = processAheadOfTime(applicationContext);
      testCompiledResult(generationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("testComponent");
        assertions.accept(freshApplicationContext.getBean("testComponent", type), generationContext);
      });
    }

  }

  @Nested
  class ResourceAutowiring {

    @Test
    void processAheadOfTimeWhenHasResourceAutowiring() {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      registerBeanPostProcessor(applicationContext,
              AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME, CommonAnnotationBeanPostProcessor.class);
      registerStringBean(applicationContext, "text", "hello");
      registerStringBean(applicationContext, "text2", "hello2");
      registerIntegerBean(applicationContext, "number", 42);
      applicationContext.registerBeanDefinition("resourceComponent", new RootBeanDefinition(ResourceComponent.class));
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("resourceComponent", "text", "text2", "number");
        ResourceComponent bean = freshApplicationContext.getBean(ResourceComponent.class);
        assertThat(bean.getText()).isEqualTo("hello");
        assertThat(bean.getCounter()).isEqualTo(42);
      });
    }

    @Test
    void processAheadOfTimeWhenHasLazyResourceAutowiringOnField() {
      testResourceAutowiringComponent(LazyResourceFieldComponent.class, (bean, generationContext) -> {
        Environment environment = bean.getEnvironment();
        assertThat(environment).isInstanceOf(Proxy.class);
        ResourceLoader resourceLoader = bean.getResourceLoader();
        assertThat(resourceLoader).isNotInstanceOf(Proxy.class);
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        assertThat(runtimeHints.proxies().jdkProxyHints()).satisfies(doesNotHaveProxyFor(ResourceLoader.class));
        assertThat(runtimeHints.proxies().jdkProxyHints()).anySatisfy(proxyHint ->
                assertThat(proxyHint.getProxiedInterfaces()).isEqualTo(TypeReference.listOf(
                        environment.getClass().getInterfaces())));

      });
    }

    @Test
    void processAheadOfTimeWhenHasLazyResourceAutowiringOnMethod() {
      testResourceAutowiringComponent(LazyResourceMethodComponent.class, (bean, generationContext) -> {
        Environment environment = bean.getEnvironment();
        assertThat(environment).isNotInstanceOf(Proxy.class);
        ResourceLoader resourceLoader = bean.getResourceLoader();
        assertThat(resourceLoader).isInstanceOf(Proxy.class);
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        assertThat(runtimeHints.proxies().jdkProxyHints()).satisfies(doesNotHaveProxyFor(Environment.class));
        assertThat(runtimeHints.proxies().jdkProxyHints()).anySatisfy(proxyHint ->
                assertThat(proxyHint.getProxiedInterfaces()).isEqualTo(TypeReference.listOf(
                        resourceLoader.getClass().getInterfaces())));
      });
    }

    private <T> void testResourceAutowiringComponent(Class<T> type, BiConsumer<T, GenerationContext> assertions) {
      testResourceAutowiringComponent(type, new RootBeanDefinition(type), assertions);
    }

    private <T> void testResourceAutowiringComponent(Class<T> type, RootBeanDefinition beanDefinition,
            BiConsumer<T, GenerationContext> assertions) {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      applicationContext.getBeanFactory().setAutowireCandidateResolver(
              new ContextAnnotationAutowireCandidateResolver());
      registerBeanPostProcessor(applicationContext,
              AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME, CommonAnnotationBeanPostProcessor.class);
      applicationContext.registerBeanDefinition("testComponent", beanDefinition);
      TestGenerationContext generationContext = processAheadOfTime(applicationContext);
      testCompiledResult(generationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("testComponent");
        assertions.accept(freshApplicationContext.getBean("testComponent", type), generationContext);
      });
    }
  }

  @Nested
  class InitDestroy {

    @Test
    void processAheadOfTimeWhenHasInitDestroyMethods() {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      registerBeanPostProcessor(applicationContext,
              AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME, CommonAnnotationBeanPostProcessor.class);
      applicationContext.registerBeanDefinition("initDestroyComponent",
              new RootBeanDefinition(InitDestroyComponent.class));
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("initDestroyComponent");
        InitDestroyComponent bean = freshApplicationContext.getBean(InitDestroyComponent.class);
        assertThat(bean.events).containsExactly("init");
        freshApplicationContext.close();
        assertThat(bean.events).containsExactly("init", "destroy");
      });
    }

    @Test
    void processAheadOfTimeWhenHasMultipleInitDestroyMethods() {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      registerBeanPostProcessor(applicationContext,
              AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME, CommonAnnotationBeanPostProcessor.class);
      RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyComponent.class);
      beanDefinition.setInitMethodName("customInit");
      beanDefinition.setDestroyMethodName("customDestroy");
      applicationContext.registerBeanDefinition("initDestroyComponent", beanDefinition);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBeanDefinitionNames()).containsOnly("initDestroyComponent");
        InitDestroyComponent bean = freshApplicationContext.getBean(InitDestroyComponent.class);
        assertThat(bean.events).containsExactly("init", "customInit");
        freshApplicationContext.close();
        assertThat(bean.events).containsExactly("init", "customInit", "destroy", "customDestroy");
      });
    }

  }

  @Test
  void processAheadOfTimeWhenHasNoAotContributions() {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      assertThat(freshApplicationContext.getBeanDefinitionNames()).isEmpty();
      assertThat(compiled.getSourceFile())
              .contains("beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver())")
              .contains("beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE)");
    });
  }

  @Test
  void processAheadOfTimeWhenHasBeanFactoryInitializationAotProcessorExcludesProcessor() {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    applicationContext.registerBeanDefinition("test",
            new RootBeanDefinition(NoOpBeanFactoryInitializationAotProcessor.class));
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      assertThat(freshApplicationContext.getBeanDefinitionNames()).isEmpty();
    });
  }

  @Test
  void processAheadOfTimeWhenHasBeanRegistrationAotProcessorExcludesProcessor() {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    applicationContext.registerBeanDefinition("test",
            new RootBeanDefinition(NoOpBeanRegistrationAotProcessor.class));
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      assertThat(freshApplicationContext.getBeanDefinitionNames()).isEmpty();
    });
  }

  @Test
  void processAheadOfTimeWithPropertySource() {
    GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.registerBean(PropertySourceConfiguration.class);
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      ConfigurableEnvironment environment = freshApplicationContext.getEnvironment();
      PropertySource<?> propertySource = environment.getPropertySources().get("testp1");
      assertThat(propertySource).isNotNull();
      assertThat(propertySource.getProperty("from.p1")).isEqualTo("p1Value");
    });
  }

  @Test
  void processAheadOfTimeWithQualifier() {
    GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.registerBean(QualifierConfiguration.class);
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      QualifierConfiguration configuration = freshApplicationContext.getBean(QualifierConfiguration.class);
      assertThat(configuration).hasFieldOrPropertyWithValue("bean", "one");
    });
  }

  @Test
  void processAheadOfTimeWithInjectionPoint() {
    GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.registerBean(InjectionPointConfiguration.class);
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      assertThat(freshApplicationContext.getBean("classToString"))
              .isEqualTo(InjectionPointConfiguration.class.getName());
    });
  }

  @Test
  void processAheadOfTimeWithExplicitResolvableType() {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    StandardBeanFactory beanFactory = applicationContext.getBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(One.class);
    beanDefinition.setResolvedFactoryMethod(ReflectionUtils.findMethod(TestHierarchy.class, "oneBean"));
    // Override target type
    beanDefinition.setTargetType(Two.class);
    beanFactory.registerBeanDefinition("hierarchyBean", beanDefinition);
    testCompiledResult(applicationContext, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      assertThat(freshApplicationContext.getBean(Two.class))
              .isInstanceOf(Implementation.class);
    });
  }

  @Nested
  @CompileWithForkedClassLoader
  class ConfigurationClassCglibProxy {

    private static final String CGLIB_CONFIGURATION_CLASS_SUFFIX = "$$Infra$$0";

    @Test
    void processAheadOfTimeWhenHasCglibProxyWriteProxyAndGenerateReflectionHints() throws IOException {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(CglibConfiguration.class);
      TestGenerationContext context = processAheadOfTime(applicationContext);
      isRegisteredCglibClass(context, CglibConfiguration.class.getName() + CGLIB_CONFIGURATION_CLASS_SUFFIX);
      isRegisteredCglibClass(context, CglibConfiguration.class.getName() + "$$Infra$$MethodAccess$$0");
      isRegisteredCglibClass(context, CglibConfiguration.class.getName() + "$$Infra$$MethodAccess$$1");
    }

    private void isRegisteredCglibClass(TestGenerationContext context, String cglibClassName) throws IOException {
      assertThat(context.getGeneratedFiles()
              .getGeneratedFileContent(Kind.CLASS, cglibClassName.replace('.', '/') + ".class")).isNotNull();
      assertThat(RuntimeHintsPredicates.reflection().onType(TypeReference.of(cglibClassName))
              .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(context.getRuntimeHints());
    }

    @Test
    void processAheadOfTimeExposeUserClassForCglibProxy() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean("config", ValueCglibConfiguration.class);

      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext).satisfies(hasBeanDefinitionOfBeanClass("config", ValueCglibConfiguration.class));
        assertThat(compiled.getSourceFile(".*ValueCglibConfiguration__BeanDefinitions"))
                .contains("new RootBeanDefinition(ValueCglibConfiguration.class)")
                .contains("new %s(".formatted(toCglibClassSimpleName(ValueCglibConfiguration.class)));
      });
    }

    @Test
    void processAheadOfTimeUsesCglibClassForFactoryMethod() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean("config", CglibConfiguration.class);

      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext).satisfies(hasBeanDefinitionOfBeanClass("config", CglibConfiguration.class));
        assertThat(compiled.getSourceFile(".*CglibConfiguration__BeanDefinitions"))
                .contains("new RootBeanDefinition(CglibConfiguration.class)")
                .contains(">forFactoryMethod(%s.class,".formatted(toCglibClassSimpleName(CglibConfiguration.class)))
                .doesNotContain(">forFactoryMethod(%s.class,".formatted(CglibConfiguration.class));
      });
    }

    private Consumer<GenericApplicationContext> hasBeanDefinitionOfBeanClass(String name, Class<?> beanClass) {
      return context -> {
        assertThat(context.containsBean(name)).isTrue();
        assertThat(context.getBeanDefinition(name)).isInstanceOfSatisfying(RootBeanDefinition.class,
                rbd -> assertThat(rbd.getBeanClass()).isEqualTo(beanClass));
      };
    }

    @Test
    void processAheadOfTimeWhenHasCglibProxyUseProxy() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(CglibConfiguration.class);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBean("prefix", String.class)).isEqualTo("Hello0");
        assertThat(freshApplicationContext.getBean("text", String.class)).isEqualTo("Hello0 World");
      });
    }

    @Test
    void processAheadOfTimeWhenHasCglibProxyAndAutowiring() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(AutowiredCglibConfiguration.class);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(context -> {
          context.setEnvironment(new MockEnvironment().withProperty("hello", "Hi"));
          initializer.initialize(context);
        });
        assertThat(freshApplicationContext.getBean("text", String.class)).isEqualTo("Hi World");
      });
    }

    @Test
    void processAheadOfTimeWhenHasCglibProxyAndMixedAutowiring() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(AutowiredMixedCglibConfiguration.class);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(context -> {
          context.setEnvironment(new MockEnvironment().withProperty("hello", "Hi")
                  .withProperty("world", "AOT World"));
          initializer.initialize(context);
        });
        assertThat(freshApplicationContext.getBean("text", String.class)).isEqualTo("Hi AOT World");
      });
    }

    @Test
    void processAheadOfTimeWhenHasCglibProxyWithAnnotationsOnTheUserClasConstructor() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean("config", ValueCglibConfiguration.class);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(context -> {
          context.setEnvironment(new MockEnvironment().withProperty("name", "AOT World"));
          initializer.initialize(context);
        });
        assertThat(freshApplicationContext.getBean(ValueCglibConfiguration.class)
                .getName()).isEqualTo("AOT World");
      });
    }

    @Test
    void processAheadOfTimeWhenHasCglibProxyWithArgumentsUseProxy() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(ConfigurableCglibConfiguration.class);
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = createFreshApplicationContext(initializer);
        freshApplicationContext.setEnvironment(new MockEnvironment().withProperty("test.prefix", "Hi"));
        freshApplicationContext.refresh();
        assertThat(freshApplicationContext.getBean("prefix", String.class)).isEqualTo("Hi0");
        assertThat(freshApplicationContext.getBean("text", String.class)).isEqualTo("Hi0 World");
      });
    }

    @Test
    @SuppressWarnings("removal")
    void processAheadOfTimeWhenHasCglibProxyWithArgumentsRegisterIntrospectionHintsOnUserClass() {
      GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
      applicationContext.registerBean(ConfigurableCglibConfiguration.class);
      TestGenerationContext generationContext = processAheadOfTime(applicationContext);
      Constructor<?> userConstructor = ConfigurableCglibConfiguration.class.getDeclaredConstructors()[0];
      assertThat(RuntimeHintsPredicates.reflection().onConstructor(userConstructor).introspect())
              .accepts(generationContext.getRuntimeHints());
    }

    private String toCglibClassSimpleName(Class<?> configClass) {
      return configClass.getSimpleName() + CGLIB_CONFIGURATION_CLASS_SUFFIX;
    }

  }

  @Nested
  class ActiveProfile {

    @ParameterizedTest
    @MethodSource("activeProfilesParameters")
    void processAheadOfTimeWhenHasActiveProfiles(String[] aotProfiles, String[] runtimeProfiles, String[] expectedActiveProfiles) {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      if (aotProfiles.length != 0) {
        applicationContext.getEnvironment().setActiveProfiles(aotProfiles);
      }
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
        if (runtimeProfiles.length != 0) {
          freshApplicationContext.getEnvironment().setActiveProfiles(runtimeProfiles);
        }
        initializer.initialize(freshApplicationContext);
        freshApplicationContext.refresh();
        assertThat(freshApplicationContext.getEnvironment().getActiveProfiles()).containsExactly(expectedActiveProfiles);
      });
    }

    static Stream<Arguments> activeProfilesParameters() {
      return Stream.of(Arguments.of(new String[] { "aot", "prod" }, new String[] {}, new String[] { "aot", "prod" }),
              Arguments.of(new String[] {}, new String[] { "aot", "prod" }, new String[] { "aot", "prod" }),
              Arguments.of(new String[] { "aot" }, new String[] { "prod" }, new String[] { "prod", "aot" }),
              Arguments.of(new String[] { "aot", "prod" }, new String[] { "aot", "prod" }, new String[] { "aot", "prod" }),
              Arguments.of(new String[] { "default" }, new String[] {}, new String[] {}));
    }

  }

  @Nested
  class XmlSupport {

    @Test
    void processAheadOfTimeWhenHasTypedStringValue() {
      GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
      applicationContext
              .load(new ClassPathResource("applicationContextAotGeneratorTests-values.xml", getClass()));
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        Employee employee = freshApplicationContext.getBean(Employee.class);
        assertThat(employee.getName()).isEqualTo("John Smith");
        assertThat(employee.getAge()).isEqualTo(42);
        assertThat(employee.getCompany()).isEqualTo("Acme Widgets, Inc.");
        assertThat(freshApplicationContext.getBean("petIndexed", Pet.class)
                .getName()).isEqualTo("Fido");
        assertThat(freshApplicationContext.getBean("petGeneric", Pet.class)
                .getName()).isEqualTo("Dofi");
      });
    }

    @Test
    void processAheadOfTimeWhenHasTypedStringValueWithType() {
      GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
      applicationContext
              .load(new ClassPathResource("applicationContextAotGeneratorTests-values-types.xml", getClass()));
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        Employee employee = freshApplicationContext.getBean(Employee.class);
        assertThat(employee.getName()).isEqualTo("John Smith");
        assertThat(employee.getAge()).isEqualTo(42);
        assertThat(employee.getCompany()).isEqualTo("Acme Widgets, Inc.");
        assertThat(compiled.getSourceFile(".*Employee__BeanDefinitions"))
                .contains("new TypedStringValue(\"42\", Integer.class");
      });
    }

    @Test
    void processAheadOfTimeWhenHasTypedStringValueWithExpression() {
      GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
      applicationContext
              .load(new ClassPathResource("applicationContextAotGeneratorTests-values-expressions.xml", getClass()));
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        Employee employee = freshApplicationContext.getBean(Employee.class);
        assertThat(employee.getName()).isEqualTo("John Smith");
        assertThat(employee.getAge()).isEqualTo(42);
        assertThat(employee.getCompany()).isEqualTo("Acme Widgets, Inc.");
        assertThat(freshApplicationContext.getBean("pet", Pet.class)
                .getName()).isEqualTo("Fido");
      });
    }

    @Test
    void processAheadOfTimeWhenXmlHasBeanReferences() {
      GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
      applicationContext
              .load(new ClassPathResource("applicationContextAotGeneratorTests-references.xml", getClass()));
      testCompiledResult(applicationContext, (initializer, compiled) -> {
        GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
        assertThat(freshApplicationContext.getBean("petInnerBean", Pet.class)
                .getName()).isEqualTo("Fido");
        assertThat(freshApplicationContext.getBean("petRefBean", Pet.class)
                .getName()).isEqualTo("Dofi");
      });
    }

  }

  @Nested
  class ExceptionHanding {

    @Test
    void failureProcessingBeanFactoryAotContribution() {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      applicationContext.registerBeanDefinition("test",
              new RootBeanDefinition(FailingBeanFactoryInitializationAotContribution.class));
      assertThatExceptionOfType(AotProcessingException.class)
              .isThrownBy(() -> processAheadOfTime(applicationContext))
              .withMessageStartingWith("Error executing '")
              .withMessageContaining(FailingBeanFactoryInitializationAotContribution.class.getName())
              .withMessageContaining("Test exception");
    }
  }

  private static void registerBeanPostProcessor(GenericApplicationContext applicationContext,
          String beanName, Class<?> beanPostProcessorClass) {

    applicationContext.registerBeanDefinition(beanName, BeanDefinitionBuilder
            .rootBeanDefinition(beanPostProcessorClass).setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
            .getBeanDefinition());
  }

  private static void registerStringBean(GenericApplicationContext applicationContext,
          String beanName, String value) {

    applicationContext.registerBeanDefinition(beanName, BeanDefinitionBuilder
            .rootBeanDefinition(String.class).addConstructorArgValue(value)
            .getBeanDefinition());
  }

  private static void registerIntegerBean(GenericApplicationContext applicationContext,
          String beanName, int value) {

    applicationContext.registerBeanDefinition(beanName, BeanDefinitionBuilder
            .rootBeanDefinition(Integer.class, "valueOf").addConstructorArgValue(value)
            .getBeanDefinition());
  }

  private Consumer<List<? extends JdkProxyHint>> doesNotHaveProxyFor(Class<?> target) {
    return hints -> assertThat(hints).noneMatch(hint ->
            hint.getProxiedInterfaces().get(0).equals(TypeReference.of(target)));
  }

  private static TestGenerationContext processAheadOfTime(GenericApplicationContext applicationContext) {
    ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
    TestGenerationContext generationContext = new TestGenerationContext();
    generator.processAheadOfTime(applicationContext, generationContext);
    generationContext.writeGeneratedContent();
    return generationContext;
  }

  private static void testCompiledResult(GenericApplicationContext applicationContext,
          BiConsumer<ApplicationContextInitializer, Compiled> result) {

    testCompiledResult(processAheadOfTime(applicationContext), result);
  }

  private static void testCompiledResult(TestGenerationContext generationContext,
          BiConsumer<ApplicationContextInitializer, Compiled> result) {

    TestCompiler.forSystem().with(generationContext).compile(compiled ->
            result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
  }

  private static GenericApplicationContext toFreshApplicationContext(
          ApplicationContextInitializer initializer) {

    GenericApplicationContext freshApplicationContext = createFreshApplicationContext(initializer);
    freshApplicationContext.refresh();
    return freshApplicationContext;
  }

  private static GenericApplicationContext createFreshApplicationContext(
          ApplicationContextInitializer initializer) {

    GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
    initializer.initialize(freshApplicationContext);
    return freshApplicationContext;
  }

  static class NoOpBeanFactoryInitializationAotProcessor
          implements BeanFactoryPostProcessor, BeanFactoryInitializationAotProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      return null;
    }

  }

  static class NoOpBeanRegistrationAotProcessor
          implements BeanPostProcessor, BeanRegistrationAotProcessor {

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
      return null;
    }

  }

  static class FailingBeanFactoryInitializationAotContribution implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      throw new IllegalStateException("Test exception");
    }
  }

}
