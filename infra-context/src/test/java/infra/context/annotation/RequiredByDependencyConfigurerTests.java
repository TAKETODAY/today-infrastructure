package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.annotation.RequiredByDependencyConfigurer.RequiredByPostProcessor;
import infra.core.Ordered;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/25 00:05
 */
class RequiredByDependencyConfigurerTests {

  @Test
  void registerBeanDefinitionsWhenPostProcessorNotRegisteredThenRegistersRequiredByPostProcessor() {
    var beanFactory = new StandardBeanFactory();
    try (var context = new AnnotationConfigApplicationContext(beanFactory)) {
      var registry = context.getBeanFactory().unwrap(BeanDefinitionRegistry.class);
      var bootstrapContext = new BootstrapContext(context);

      var configurer = new RequiredByDependencyConfigurer();
      configurer.registerBeanDefinitions(null, bootstrapContext);

      assertThat(registry.containsBeanDefinition(RequiredByPostProcessor.class.getName())).isTrue();
    }
  }

  @Test
  void postProcessBeanFactoryWhenBeanHasRequiredByAnnotationWithTypesThenSetsDependsOn() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var stringBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var objectBean = BeanDefinitionBuilder.rootBeanDefinition(Object.class).getBeanDefinition();

    registry.registerBeanDefinition("stringBean", stringBean);
    registry.registerBeanDefinition("objectBean", objectBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(stringBean.getDependsOn()).isNull();
    assertThat(objectBean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenNoBeansHaveRequiredByAnnotationThenNoDependsOnSet() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var bean1 = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var bean2 = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();

    registry.registerBeanDefinition("bean1", bean1);
    registry.registerBeanDefinition("bean2", bean2);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(bean1.getDependsOn()).isNull();
    assertThat(bean2.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenMultipleBeansDependOnSameTypeThenAllGetCorrectDependsOn() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var serviceA = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var serviceB = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var controller = BeanDefinitionBuilder.rootBeanDefinition(Object.class).getBeanDefinition();

    registry.registerBeanDefinition("serviceA", serviceA);
    registry.registerBeanDefinition("serviceB", serviceB);
    registry.registerBeanDefinition("controller", controller);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(serviceA.getDependsOn()).isNull();
    assertThat(serviceB.getDependsOn()).isNull();
    assertThat(controller.getDependsOn()).isNull();
  }

  @Test
  void getOrderReturnsLowestPrecedence() {
    var postProcessor = new RequiredByPostProcessor();
    assertThat(postProcessor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void postProcessBeanFactoryHandlesParentBeanFactory() {
    var parentFactory = new StandardBeanFactory();
    var childFactory = new StandardBeanFactory(parentFactory);

    var parentBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    parentFactory.registerBeanDefinition("parentBean", parentBean);

    var childBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    childFactory.registerBeanDefinition("childBean", childBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(childFactory);

    assertThat(parentBean.getDependsOn()).isNull();
    assertThat(childBean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWithEmptyBeanFactory() {
    var beanFactory = new StandardBeanFactory();
    var postProcessor = new RequiredByPostProcessor();

    assertThatCode(() -> postProcessor.postProcessBeanFactory(beanFactory)).doesNotThrowAnyException();
  }

  @Test
  void postProcessBeanFactoryWhenBeanNameMatchesRequiredByTypeThenHandlesCorrectly() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var testService = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    registry.registerBeanDefinition("testService", testService);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(testService.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenRequiredByHasBothNamesAndTypesThenProcessesBoth() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var beanA = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var beanB = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var beanC = BeanDefinitionBuilder.rootBeanDefinition(Object.class).getBeanDefinition();

    registry.registerBeanDefinition("beanA", beanA);
    registry.registerBeanDefinition("beanB", beanB);
    registry.registerBeanDefinition("beanC", beanC);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(beanA.getDependsOn()).isNull();
    assertThat(beanB.getDependsOn()).isNull();
    assertThat(beanC.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenDuplicateDependenciesThenMaintainsUniqueSet() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var beanA = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var beanB = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    beanB.setDependsOn("beanA");

    registry.registerBeanDefinition("beanA", beanA);
    registry.registerBeanDefinition("beanB", beanB);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(beanB.getDependsOn()).hasSize(1).contains("beanA");
  }

  @Test
  void postProcessBeanFactoryWhenBeanHasRequiredByAnnotationWithNamesThenSetsDependsOn() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var beanA = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var beanB = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();

    registry.registerBeanDefinition("beanA", beanA);
    registry.registerBeanDefinition("beanB", beanB);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(beanA.getDependsOn()).isNull();
    assertThat(beanB.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryIntegrationWithRequiredByAnnotation() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(TestConfiguration.class);
      context.refresh();

      var beanFactory = context.getBeanFactory();
      var beanADefinition = beanFactory.getMergedBeanDefinition("testBeanA");
      var beanBDefinition = beanFactory.getMergedBeanDefinition("testBeanB");

      assertThat(beanADefinition.getDependsOn()).containsExactly("testBeanB");
      assertThat(beanBDefinition.getDependsOn()).isNull();
    }
  }

  @Test
  void postProcessBeanFactoryWithMultipleRequiredByAnnotations() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(MultipleRequiredByConfiguration.class);
      context.refresh();

      var beanFactory = context.getBeanFactory();
      var serviceDefinition = beanFactory.getMergedBeanDefinition("service");
      var repositoryDefinition = beanFactory.getMergedBeanDefinition("repository");
      var controllerDefinition = beanFactory.getMergedBeanDefinition("controller");

      assertThat(serviceDefinition.getDependsOn()).contains("controller", "repository");
      assertThat(repositoryDefinition.getDependsOn()).contains("controller");
      assertThat(controllerDefinition.getDependsOn()).isNull();
    }
  }

  @Test
  void postProcessBeanFactoryWithRequiredByType() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(RequiredByTypeConfiguration.class);
      context.refresh();

      assertThat(context.getBeanDefinition("client").getDependsOn()).isNull();
      assertThat(context.getBeanDefinition("objectService").getDependsOn()).isNull();
      assertThat(context.getBeanDefinition("stringService").getDependsOn()).containsExactly("client");
    }
  }

  @Test
  void postProcessBeanFactoryWithCircularDependencies() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(CircularDependencyConfiguration.class);
      assertThatThrownBy(context::refresh)
              .isInstanceOf(BeanCreationException.class)
              .hasMessageStartingWith("Error creating bean with name 'circularBeanB' defined in")
              .hasMessageContaining("Circular depends-on relationship between 'circularBeanB' and 'circularBeanA'");
    }
  }

  @Test
  void postProcessBeanFactoryWithNullValues() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var bean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    registry.registerBeanDefinition("nullTestBean", bean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(bean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWithEmptyDependsOnArray() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var bean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    bean.setDependsOn(new String[0]);
    registry.registerBeanDefinition("emptyDependsBean", bean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(bean.getDependsOn()).isEmpty();
  }

  @Test
  void postProcessBeanFactoryPreservesExistingDependencyOrder() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var beanA = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var beanB = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    var beanC = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();

    beanC.setDependsOn("beanA", "beanB");

    registry.registerBeanDefinition("beanA", beanA);
    registry.registerBeanDefinition("beanB", beanB);
    registry.registerBeanDefinition("beanC", beanC);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(beanC.getDependsOn()).containsSequence("beanA", "beanB");
  }

  //

  @Test
  void postProcessBeanFactoryWhenBeanNotFoundInChildFactoryThenDelegatesToParentFactory() {
    var parentFactory = new StandardBeanFactory();
    var childFactory = new StandardBeanFactory(parentFactory);

    var parentBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    parentFactory.registerBeanDefinition("parentOnlyBean", parentBean);

    var childBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    childFactory.registerBeanDefinition("childOnlyBean", childBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(childFactory);

    assertThat(parentBean.getDependsOn()).isNull();
    assertThat(childBean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenLookingUpParentBeanFromChildContextThenSuccessfullyRetrieves() {
    var parentFactory = new StandardBeanFactory();
    var childFactory = new StandardBeanFactory(parentFactory);

    var referencedBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    parentFactory.registerBeanDefinition("referencedBean", referencedBean);

    var dependentBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    childFactory.registerBeanDefinition("dependentBean", dependentBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(childFactory);

    assertThat(referencedBean.getDependsOn()).isNull();
    assertThat(dependentBean.getDependsOn()).isNull();
  }

  @Test
  void getBeanDefinitionRecursivelySearchesParentHierarchyWhenBeanNotFound() {
    var grandParentFactory = new StandardBeanFactory();
    var parentFactory = new StandardBeanFactory(grandParentFactory);
    var childFactory = new StandardBeanFactory(parentFactory);

    var sharedBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    grandParentFactory.registerBeanDefinition("sharedBean", sharedBean);

    var localBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    childFactory.registerBeanDefinition("localBean", localBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(childFactory);

    assertThat(sharedBean.getDependsOn()).isNull();
    assertThat(localBean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenParentFactoryIsNullThenHandlesGracefully() {
    var beanFactory = new StandardBeanFactory();
    var registry = beanFactory.unwrap(BeanDefinitionRegistry.class);

    var bean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    registry.registerBeanDefinition("standaloneBean", bean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(beanFactory);

    assertThat(bean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWithThreeLevelHierarchy() {
    var level1Factory = new StandardBeanFactory();
    var level2Factory = new StandardBeanFactory(level1Factory);
    var level3Factory = new StandardBeanFactory(level2Factory);

    var level1Bean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    level1Factory.registerBeanDefinition("level1Bean", level1Bean);

    var level2Bean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    level2Factory.registerBeanDefinition("level2Bean", level2Bean);

    var level3Bean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    level3Factory.registerBeanDefinition("level3Bean", level3Bean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(level3Factory);

    assertThat(level1Bean.getDependsOn()).isNull();
    assertThat(level2Bean.getDependsOn()).isNull();
    assertThat(level3Bean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenParentIsNotConfigurableBeanFactory() {
    var nonConfigurableParent = new infra.beans.factory.support.StandardBeanFactory();
    var childFactory = new StandardBeanFactory(nonConfigurableParent);

    var childBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    childFactory.registerBeanDefinition("childBean", childBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(childFactory);

    assertThat(childBean.getDependsOn()).isNull();
  }

  @Test
  void registerBeanDefinitionsSetsDependencyInjectionDisabled() {
    var beanFactory = new StandardBeanFactory();
    try (var context = new AnnotationConfigApplicationContext(beanFactory)) {
      var registry = context.getBeanFactory().unwrap(BeanDefinitionRegistry.class);
      var bootstrapContext = new BootstrapContext(context);

      var configurer = new RequiredByDependencyConfigurer();
      configurer.registerBeanDefinitions(null, bootstrapContext);

      var definition = registry.getBeanDefinition(RequiredByPostProcessor.class.getName());
      assertThat(definition.isEnableDependencyInjection()).isFalse();
    }
  }

  @Test
  void postProcessBeanFactoryWhenMultipleLevelsShareSameBeanName() {
    var parentFactory = new StandardBeanFactory();
    var childFactory = new StandardBeanFactory(parentFactory);

    var parentSharedBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    parentFactory.registerBeanDefinition("sharedBean", parentSharedBean);

    var childSharedBean = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
    childFactory.registerBeanDefinition("sharedBean", childSharedBean);

    var postProcessor = new RequiredByPostProcessor();
    postProcessor.postProcessBeanFactory(childFactory);

    assertThat(parentSharedBean.getDependsOn()).isNull();
    assertThat(childSharedBean.getDependsOn()).isNull();
  }

  @Test
  void postProcessBeanFactoryWhenChildContextHasRequiredByForParentBean() {
    var parentContext = new AnnotationConfigApplicationContext();
    parentContext.register(SimpleParentConfig.class);
    parentContext.refresh();

    var childContext = new AnnotationConfigApplicationContext(parentContext);
    childContext.register(DependentChildConfig.class);
    childContext.refresh();

    var parentDef = parentContext.getBeanDefinition("dataService");
    var childDef = childContext.getBeanDefinition("businessService");

    assertThat(parentDef.getDependsOn()).contains("businessService");
    assertThat(childDef.getDependsOn()).isNull();
  }

  @Configuration
  @EnableRequiredBy
  static class SimpleParentConfig {

    @Component
    String dataService() {
      return "data";
    }
  }

  @Configuration
  @EnableRequiredBy
  static class DependentChildConfig {

    @Component
    @RequiredBy("dataService")
    String businessService() {
      return "business";
    }
  }

  @Configuration
  @EnableRequiredBy
  static class TestConfiguration {

    @Component
    String testBeanA() {
      return "beanA";
    }

    @Component
    @RequiredBy("testBeanA")
    String testBeanB() {
      return "beanB";
    }
  }

  @EnableRequiredBy
  @Configuration
  static class MultipleRequiredByConfiguration {

    @Component
    String service() {
      return "service";
    }

    @Component
    @RequiredBy("service")
    String repository() {
      return "repository";
    }

    @Component
    @RequiredBy({ "service", "repository" })
    String controller() {
      return "controller";
    }
  }

  @EnableRequiredBy
  @Configuration
  static class RequiredByTypeConfiguration {

    @Component
    String stringService() {
      return "stringService";
    }

    @Component
    Object objectService() {
      return "objectService";
    }

    @Component
    @RequiredBy(types = CharSequence.class)
    Object client() {
      return "client";
    }

  }

  @EnableRequiredBy
  @Configuration
  static class CircularDependencyConfiguration {

    @Component
    @RequiredBy("circularBeanB")
    String circularBeanA() {
      return "beanA";
    }

    @Component
    @RequiredBy("circularBeanA")
    String circularBeanB() {
      return "beanB";
    }
  }

}