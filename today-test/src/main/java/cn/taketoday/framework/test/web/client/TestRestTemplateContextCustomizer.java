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

package cn.taketoday.framework.test.web.client;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.ConfigurationClassPostProcessor;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.framework.test.web.client.TestRestTemplate.HttpClientOption;
import cn.taketoday.framework.web.server.AbstractConfigurableWebServerFactory;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.web.client.config.RestTemplateBuilder;

/**
 * {@link ContextCustomizer} for {@link TestRestTemplate}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class TestRestTemplateContextCustomizer implements ContextCustomizer {

  @Override
  public void customizeContext(ConfigurableApplicationContext context,
          MergedContextConfiguration mergedContextConfiguration) {
    InfraTest infraTest = TestContextAnnotationUtils
            .findMergedAnnotation(mergedContextConfiguration.getTestClass(), InfraTest.class);
    if (infraTest != null && infraTest.webEnvironment().isEmbedded()) {
      registerTestRestTemplate(context);
    }
  }

  private void registerTestRestTemplate(ConfigurableApplicationContext context) {
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory instanceof BeanDefinitionRegistry) {
      registerTestRestTemplate((BeanDefinitionRegistry) beanFactory);
    }
  }

  private void registerTestRestTemplate(BeanDefinitionRegistry registry) {
    RootBeanDefinition definition = new RootBeanDefinition(TestRestTemplateRegistrar.class);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(TestRestTemplateRegistrar.class.getName(), definition);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null) && (obj.getClass() == getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * {@link BeanDefinitionRegistryPostProcessor} that runs after the
   * {@link ConfigurationClassPostProcessor} and add a {@link TestRestTemplateFactory}
   * bean definition when a {@link TestRestTemplate} hasn't already been registered.
   */
  static class TestRestTemplateRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      this.beanFactory = beanFactory;
    }

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory,
              TestRestTemplate.class, false, false).size() == 0) {
        registry.registerBeanDefinition(TestRestTemplate.class.getName(),
                new RootBeanDefinition(TestRestTemplateFactory.class));
      }

    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }

  }

  /**
   * {@link FactoryBean} used to create and configure a {@link TestRestTemplate}.
   */
  public static class TestRestTemplateFactory implements FactoryBean<TestRestTemplate>, ApplicationContextAware {

    private static final HttpClientOption[] DEFAULT_OPTIONS = {};

    private static final HttpClientOption[] SSL_OPTIONS = { HttpClientOption.SSL };

    private TestRestTemplate template;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      RestTemplateBuilder builder = getRestTemplateBuilder(applicationContext);
      boolean sslEnabled = isSslEnabled(applicationContext);
      TestRestTemplate template = new TestRestTemplate(builder, null, null,
              sslEnabled ? SSL_OPTIONS : DEFAULT_OPTIONS);
      LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(applicationContext.getEnvironment(),
              sslEnabled ? "https" : "http");
      template.setUriTemplateHandler(handler);
      this.template = template;
    }

    private boolean isSslEnabled(ApplicationContext context) {
      try {
        var webServerFactory = context.getBean(AbstractConfigurableWebServerFactory.class);
        return Ssl.isEnabled(webServerFactory.getSsl());
      }
      catch (NoSuchBeanDefinitionException ex) {
        return false;
      }
    }

    private RestTemplateBuilder getRestTemplateBuilder(ApplicationContext applicationContext) {
      try {
        return applicationContext.getBean(RestTemplateBuilder.class);
      }
      catch (NoSuchBeanDefinitionException ex) {
        return new RestTemplateBuilder();
      }
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

    @Override
    public Class<?> getObjectType() {
      return TestRestTemplate.class;
    }

    @Override
    public TestRestTemplate getObject() throws Exception {
      return this.template;
    }

  }

}
