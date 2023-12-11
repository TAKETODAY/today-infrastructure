/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.annotation.CustomAutowireConfigurer;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.context.annotation.ComponentScan.Filter;
import cn.taketoday.context.annotation.ComponentScanParserTests.KustomAnnotationAutowiredBean;
import cn.taketoday.context.annotation.componentscan.simple.ClassWithNestedComponents;
import cn.taketoday.context.annotation.componentscan.simple.SimpleComponent;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.TypeFilter;
import example.scannable.CustomComponent;
import example.scannable.CustomStereotype;
import example.scannable.DefaultNamedComponent;
import example.scannable.FooService;
import example.scannable.MessageBean;
import example.scannable.ScopedProxyTestBean;
import example.scannable_implicitbasepackage.ComponentScanAnnotatedConfigWithImplicitBasePackage;
import example.scannable_implicitbasepackage.ConfigurableComponent;
import example.scannable_scoped.CustomScopeAnnotationBean;
import example.scannable_scoped.MyScope;

import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for processing ComponentScan-annotated Configuration classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("resource")
class ComponentScanAnnotationIntegrationTests {

  @Test
  void controlScan() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.scan(example.scannable.PackageMarker.class.getPackage().getName());
    ctx.refresh();
    assertContextContainsBean(ctx, "fooServiceImpl");
  }

  @Test
  void viaContextRegistration() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ComponentScanAnnotatedConfig.class);
    ctx.getBean(ComponentScanAnnotatedConfig.class);
    ctx.getBean(TestBean.class);
    assertThat(ctx.containsBeanDefinition("componentScanAnnotatedConfig")).as("config class bean not found").isTrue();
    assertThat(ctx.containsBean("fooServiceImpl")).as("@ComponentScan annotated @Configuration class registered directly against " +
            "AnnotationConfigApplicationContext did not trigger component scanning as expected").isTrue();
  }

  @Test
  void viaContextRegistration_WithValueAttribute() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ComponentScanAnnotatedConfig_WithValueAttribute.class);
    ctx.getBean(ComponentScanAnnotatedConfig_WithValueAttribute.class);
    ctx.getBean(TestBean.class);
    assertThat(ctx.containsBeanDefinition("componentScanAnnotatedConfig_WithValueAttribute")).as("config class bean not found").isTrue();
    assertThat(ctx.containsBean("fooServiceImpl")).as("@ComponentScan annotated @Configuration class registered directly against " +
            "AnnotationConfigApplicationContext did not trigger component scanning as expected").isTrue();
  }

  @Test
  void viaContextRegistration_FromPackageOfConfigClass() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ComponentScanAnnotatedConfigWithImplicitBasePackage.class);
    ctx.getBean(ComponentScanAnnotatedConfigWithImplicitBasePackage.class);
    assertThat(ctx.containsBeanDefinition("componentScanAnnotatedConfigWithImplicitBasePackage")).as("config class bean not found").isTrue();
    assertThat(ctx.containsBean("scannedComponent")).as("@ComponentScan annotated @Configuration class registered directly against " +
            "AnnotationConfigApplicationContext did not trigger component scanning as expected").isTrue();
    assertThat(ctx.getBean(ConfigurableComponent.class).isFlag()).as("@Bean method overrides scanned class").isTrue();
  }

  @Test
  void viaContextRegistration_WithComposedAnnotation() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComposedAnnotationConfig.class);
    ctx.getBean(ComposedAnnotationConfig.class);
    ctx.getBean(SimpleComponent.class);
    ctx.getBean(ClassWithNestedComponents.NestedComponent.class);
    ctx.getBean(ClassWithNestedComponents.OtherNestedComponent.class);
    assertThat(ctx.containsBeanDefinition("componentScanAnnotationIntegrationTests.ComposedAnnotationConfig")).as("config class bean not found").isTrue();
    assertThat(ctx.containsBean("simpleComponent")).as("@ComponentScan annotated @Configuration class registered directly against " +
            "AnnotationConfigApplicationContext did not trigger component scanning as expected").isTrue();
  }

  @Test
  void multipleComposedComponentScanAnnotations() {  // gh-30941
    ApplicationContext ctx = new AnnotationConfigApplicationContext(MultipleComposedAnnotationsConfig.class);
    ctx.getBean(MultipleComposedAnnotationsConfig.class);
    assertContextContainsBean(ctx, "componentScanAnnotationIntegrationTests.MultipleComposedAnnotationsConfig");
    assertContextContainsBean(ctx, "simpleComponent");
    assertContextContainsBean(ctx, "barComponent");
  }

  @Test
  void localAnnotationOverridesMultipleMetaAnnotations() {  // gh-31704
    ApplicationContext ctx = new AnnotationConfigApplicationContext(LocalAnnotationOverridesMultipleMetaAnnotationsConfig.class);

    assertContextContainsBean(ctx, "componentScanAnnotationIntegrationTests.LocalAnnotationOverridesMultipleMetaAnnotationsConfig");
    assertContextContainsBean(ctx, "barComponent");

    assertContextDoesNotContainBean(ctx, "simpleComponent");
    assertContextDoesNotContainBean(ctx, "configurableComponent");
  }

  @Test
  void localAnnotationOverridesMultipleComposedAnnotations() {  // gh-31704
    ApplicationContext ctx = new AnnotationConfigApplicationContext(LocalAnnotationOverridesMultipleComposedAnnotationsConfig.class);

    assertContextContainsBean(ctx, "componentScanAnnotationIntegrationTests.LocalAnnotationOverridesMultipleComposedAnnotationsConfig");
    assertContextContainsBean(ctx, "barComponent");

    assertContextDoesNotContainBean(ctx, "simpleComponent");
    assertContextDoesNotContainBean(ctx, "configurableComponent");
  }

  @Test
  void localRepeatedAnnotationsOverrideComposedAnnotations() {  // gh-31704
    ApplicationContext ctx = new AnnotationConfigApplicationContext(LocalRepeatedAnnotationsOverrideComposedAnnotationsConfig.class);

    assertContextContainsBean(ctx, "componentScanAnnotationIntegrationTests.LocalRepeatedAnnotationsOverrideComposedAnnotationsConfig");
    assertContextContainsBean(ctx, "barComponent");
    assertContextContainsBean(ctx, "configurableComponent");

    assertContextDoesNotContainBean(ctx, "simpleComponent");
  }

  @Test
  void localRepeatedAnnotationsInContainerOverrideComposedAnnotations() {  // gh-31704
    ApplicationContext ctx = new AnnotationConfigApplicationContext(LocalRepeatedAnnotationsInContainerOverrideComposedAnnotationsConfig.class);

    assertContextContainsBean(ctx, "componentScanAnnotationIntegrationTests.LocalRepeatedAnnotationsInContainerOverrideComposedAnnotationsConfig");
    assertContextContainsBean(ctx, "barComponent");
    assertContextContainsBean(ctx, "configurableComponent");

    assertContextDoesNotContainBean(ctx, "simpleComponent");
  }

  @Test
  void viaBeanRegistration() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("componentScanAnnotatedConfig",
            genericBeanDefinition(ComponentScanAnnotatedConfig.class).getBeanDefinition());
    bf.registerBeanDefinition("configurationClassPostProcessor",
            genericBeanDefinition(ConfigurationClassPostProcessor.class).getBeanDefinition());
    GenericApplicationContext ctx = new GenericApplicationContext(bf);
    ctx.refresh();
    ctx.getBean(ComponentScanAnnotatedConfig.class);
    ctx.getBean(TestBean.class);
    assertThat(ctx.containsBeanDefinition("componentScanAnnotatedConfig")).as("config class bean not found").isTrue();
    assertThat(ctx.containsBean("fooServiceImpl")).as("@ComponentScan annotated @Configuration class registered as bean " +
            "definition did not trigger component scanning as expected").isTrue();
  }

  @Test
  void withCustomBeanNameGenerator() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithBeanNameGenerator.class);
    assertContextContainsBean(ctx, "custom_fooServiceImpl");
    assertContextDoesNotContainBean(ctx, "fooServiceImpl");
  }

  @Test
  void withScopeResolver() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithScopeResolver.class);
    // custom scope annotation makes the bean prototype scoped. subsequent calls
    // to getBean should return distinct instances.
    assertThat(ctx.getBean(CustomScopeAnnotationBean.class)).isNotSameAs(ctx.getBean(CustomScopeAnnotationBean.class));
    assertContextDoesNotContainBean(ctx, "scannedComponent");
  }

  @Test
  void multiComponentScan() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MultiComponentScan.class);
    assertThat(ctx.getBean(CustomScopeAnnotationBean.class)).isNotSameAs(ctx.getBean(CustomScopeAnnotationBean.class));
    assertContextContainsBean(ctx, "scannedComponent");
  }

  @Test
  void withCustomTypeFilter() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithCustomTypeFilter.class);
    assertThat(ctx.getBeanFactory().containsSingleton("componentScanParserTests.KustomAnnotationAutowiredBean")).isFalse();
    KustomAnnotationAutowiredBean testBean = ctx.getBean("componentScanParserTests.KustomAnnotationAutowiredBean", KustomAnnotationAutowiredBean.class);
    assertThat(testBean.getDependency()).isNotNull();
  }

  @Test
  void withAwareTypeFilter() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithAwareTypeFilter.class);
    assertThat(ctx.getEnvironment().matchesProfiles("the-filter-ran")).isTrue();
  }

  @Test
  void withScopedProxy() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ComponentScanWithScopedProxy.class);
    ctx.getBeanFactory().registerScope("myScope", new SimpleMapScope());
    ctx.refresh();
    // should cast to the interface
    FooService bean = (FooService) ctx.getBean("scopedProxyTestBean");
    // should be dynamic proxy
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    // test serializability
    assertThat(bean.foo(1)).isEqualTo("bar");
    FooService deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
    assertThat(deserialized).isNotNull();
    assertThat(deserialized.foo(1)).isEqualTo("bar");
  }

  @Test
  void withScopedProxyThroughRegex() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ComponentScanWithScopedProxyThroughRegex.class);
    ctx.getBeanFactory().registerScope("myScope", new SimpleMapScope());
    ctx.refresh();
    // should cast to the interface
    FooService bean = (FooService) ctx.getBean("scopedProxyTestBean");
    // should be dynamic proxy
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
  }

  @Test
  void withScopedProxyThroughAspectJPattern() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ComponentScanWithScopedProxyThroughAspectJPattern.class);
    ctx.getBeanFactory().registerScope("myScope", new SimpleMapScope());
    ctx.refresh();
    // should cast to the interface
    FooService bean = (FooService) ctx.getBean("scopedProxyTestBean");
    // should be dynamic proxy
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
  }

  @Test
  void withMultipleAnnotationIncludeFilters1() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ComponentScanWithMultipleAnnotationIncludeFilters1.class);
    ctx.getBean(DefaultNamedComponent.class); // @CustomStereotype-annotated
    ctx.getBean(MessageBean.class);           // @CustomComponent-annotated
  }

  @Test
  void withMultipleAnnotationIncludeFilters2() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ComponentScanWithMultipleAnnotationIncludeFilters2.class);
    ctx.getBean(DefaultNamedComponent.class); // @CustomStereotype-annotated
    ctx.getBean(MessageBean.class);           // @CustomComponent-annotated
  }

  @Test
  void withBeanMethodOverride() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ComponentScanWithMultipleAnnotationIncludeFilters3.class);
    ctx.refresh();
    assertThat(ctx.getBean(DefaultNamedComponent.class).toString()).isEqualTo("overridden");
  }

  @Test
  void withBeanMethodOverrideAndGeneralOverridingDisabled() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getBeanFactory().setAllowBeanDefinitionOverriding(false);
    ctx.register(ComponentScanWithMultipleAnnotationIncludeFilters3.class);
    ctx.refresh();
    assertThat(ctx.getBean(DefaultNamedComponent.class).toString()).isEqualTo("overridden");
  }

  @Test
  void withBasePackagesAndValueAlias() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ComponentScanWithBasePackagesAndValueAlias.class);
    assertContextContainsBean(ctx, "fooServiceImpl");
  }

  private static void assertContextContainsBean(ApplicationContext ctx, String beanName) {
    assertThat(ctx.containsBean(beanName)).as("context should contain bean " + beanName).isTrue();
  }

  private static void assertContextDoesNotContainBean(ApplicationContext ctx, String beanName) {
    assertThat(ctx.containsBean(beanName)).as("context should not contain bean " + beanName).isFalse();
  }

  @Configuration
  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface ComposedConfiguration {

    @AliasFor(annotation = ComponentScan.class)
    String[] basePackages() default {};
  }

  @Configuration
  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface ComposedConfiguration2 {

    @AliasFor(annotation = ComponentScan.class)
    String[] basePackages() default {};
  }

  @Configuration
  @ComponentScan("cn.taketoday.context.annotation.componentscan.simple")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaConfiguration1 {
  }

  @Configuration
  @ComponentScan("example.scannable_implicitbasepackage")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaConfiguration2 {
  }

  @ComposedConfiguration(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  static class ComposedAnnotationConfig {
  }

  @ComposedConfiguration(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  @ComposedConfiguration2(basePackages = "example.scannable.sub")
  static class MultipleComposedAnnotationsConfig {
  }

  @MetaConfiguration1
  @MetaConfiguration2
  @ComponentScan("example.scannable.sub")
  static class LocalAnnotationOverridesMultipleMetaAnnotationsConfig {
  }

  @ComposedConfiguration(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  @ComposedConfiguration2(basePackages = "example.scannable_implicitbasepackage")
  @ComponentScan("example.scannable.sub")
  static class LocalAnnotationOverridesMultipleComposedAnnotationsConfig {
  }

  @ComposedConfiguration(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  @ComponentScan("example.scannable_implicitbasepackage")
  @ComponentScan("example.scannable.sub")
  static class LocalRepeatedAnnotationsOverrideComposedAnnotationsConfig {
  }

  @ComposedConfiguration(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  @ComponentScans({
          @ComponentScan("example.scannable_implicitbasepackage"),
          @ComponentScan("example.scannable.sub")
  })
  static class LocalRepeatedAnnotationsInContainerOverrideComposedAnnotationsConfig {
  }

  static class AwareTypeFilter implements TypeFilter, EnvironmentAware,
          ResourceLoaderAware, BeanClassLoaderAware, BeanFactoryAware {

    private BeanFactory beanFactory;
    private ClassLoader classLoader;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      this.beanFactory = beanFactory;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
      this.classLoader = classLoader;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
      ((ConfigurableEnvironment) this.environment).addActiveProfile("the-filter-ran");
      assertThat(this.beanFactory).isNotNull();
      assertThat(this.classLoader).isNotNull();
      assertThat(this.resourceLoader).isNotNull();
      assertThat(this.environment).isNotNull();
      return false;
    }
  }

}

@Configuration
@ComponentScan(basePackageClasses = example.scannable.PackageMarker.class)
class ComponentScanAnnotatedConfig {

  @Bean
  public TestBean testBean() {
    return new TestBean();
  }
}

@Configuration
@ComponentScan("example.scannable")
class ComponentScanAnnotatedConfig_WithValueAttribute {

  @Bean
  public TestBean testBean() {
    return new TestBean();
  }
}

@Configuration
@ComponentScan
class ComponentScanWithNoPackagesConfig {
}

@Configuration
@ComponentScan(basePackages = "example.scannable", nameGenerator = MyBeanNameGenerator.class)
class ComponentScanWithBeanNameGenerator {
}

class MyBeanNameGenerator extends AnnotationBeanNameGenerator {

  @Override
  public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
    return "custom_" + super.generateBeanName(definition, registry);
  }
}

@Configuration
@ComponentScan(basePackages = "example.scannable_scoped", scopeResolver = MyScopeMetadataResolver.class)
class ComponentScanWithScopeResolver {
}

@Configuration
@ComponentScan(basePackages = "example.scannable_scoped", scopeResolver = MyScopeMetadataResolver.class)
@ComponentScan(basePackages = "example.scannable_implicitbasepackage")
class MultiComponentScan {
}

class MyScopeMetadataResolver extends AnnotationScopeMetadataResolver {

  MyScopeMetadataResolver() {
    this.scopeAnnotationType = MyScope.class;
  }
}

@Configuration
@ComponentScan(
        basePackages = "cn.taketoday.context.annotation",
        useDefaultFilters = false,
        includeFilters = @Filter(type = FilterType.CUSTOM, classes = ComponentScanParserTests.CustomTypeFilter.class),
        // exclude this class from scanning since it's in the scanned package
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ComponentScanWithCustomTypeFilter.class),
        lazyInit = true)
class ComponentScanWithCustomTypeFilter {

  @Bean
  @SuppressWarnings({ "rawtypes", "serial", "unchecked" })
  public static CustomAutowireConfigurer customAutowireConfigurer() {
    CustomAutowireConfigurer cac = new CustomAutowireConfigurer();
    cac.setCustomQualifierTypes(new HashSet() {{
      add(ComponentScanParserTests.CustomAnnotation.class);
    }});
    return cac;
  }

  public ComponentScanParserTests.KustomAnnotationAutowiredBean testBean() {
    return new ComponentScanParserTests.KustomAnnotationAutowiredBean();
  }
}

@Configuration
@ComponentScan(
        basePackages = "cn.taketoday.context.annotation",
        useDefaultFilters = false,
        includeFilters = @Filter(type = FilterType.CUSTOM, classes = ComponentScanAnnotationIntegrationTests.AwareTypeFilter.class),
        lazyInit = true)
class ComponentScanWithAwareTypeFilter { }

@Configuration
@ComponentScan(basePackages = "example.scannable",
               scopedProxy = ScopedProxyMode.INTERFACES,
               useDefaultFilters = false,
               includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScopedProxyTestBean.class))
class ComponentScanWithScopedProxy { }

@Configuration
@ComponentScan(basePackages = "example.scannable",
               scopedProxy = ScopedProxyMode.INTERFACES,
               useDefaultFilters = false,
               includeFilters = @Filter(type = FilterType.REGEX, pattern = "((?:[a-z.]+))ScopedProxyTestBean"))
class ComponentScanWithScopedProxyThroughRegex { }

@Configuration
@ComponentScan(basePackages = "example.scannable",
               scopedProxy = ScopedProxyMode.INTERFACES,
               useDefaultFilters = false,
               includeFilters = @Filter(type = FilterType.ASPECTJ, pattern = "*..ScopedProxyTestBean"))
class ComponentScanWithScopedProxyThroughAspectJPattern { }

@Configuration
@ComponentScan(basePackages = "example.scannable",
               useDefaultFilters = false,
               includeFilters = {
                       @Filter(CustomStereotype.class),
                       @Filter(CustomComponent.class)
               }
)
class ComponentScanWithMultipleAnnotationIncludeFilters1 { }

@Configuration
@ComponentScan(basePackages = "example.scannable",
               useDefaultFilters = false,
               includeFilters = @Filter({ CustomStereotype.class, CustomComponent.class })
)
class ComponentScanWithMultipleAnnotationIncludeFilters2 { }

@Configuration
@ComponentScan(
        useDefaultFilters = false,
        basePackages = "example.scannable",
        includeFilters = @Filter({ CustomStereotype.class, CustomComponent.class }))
class ComponentScanWithMultipleAnnotationIncludeFilters3 {

  @Bean
  public DefaultNamedComponent thoreau() {
    return new DefaultNamedComponent() {
      @Override
      public String toString() {
        return "overridden";
      }
    };
  }
}

@Configuration
@ComponentScan(
        value = "example.scannable",
        basePackages = "example.scannable",
        basePackageClasses = example.scannable.PackageMarker.class)
class ComponentScanWithBasePackagesAndValueAlias { }


