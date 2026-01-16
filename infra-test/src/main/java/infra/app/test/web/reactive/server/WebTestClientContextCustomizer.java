/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.web.reactive.server;

import java.util.Collection;

import infra.aot.AotDetector;
import infra.app.ApplicationType;
import infra.app.test.context.InfraTest;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.ConfigurationClassPostProcessor;
import infra.core.Ordered;
import infra.http.codec.CodecCustomizer;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContextAnnotationUtils;
import infra.test.web.reactive.server.WebTestClient;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.client.reactive.ExchangeStrategies;

/**
 * {@link ContextCustomizer} for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class WebTestClientContextCustomizer implements ContextCustomizer {

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
    if (AotDetector.useGeneratedArtifacts()) {
      return;
    }
    InfraTest infraTest = TestContextAnnotationUtils.findMergedAnnotation(mergedConfig.getTestClass(), InfraTest.class);
    if (infraTest.webEnvironment().isEmbedded()) {
      registerWebTestClient(context);
    }
  }

  private void registerWebTestClient(ConfigurableApplicationContext context) {
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory instanceof BeanDefinitionRegistry registry) {
      registerWebTestClient(registry);
    }
  }

  private void registerWebTestClient(BeanDefinitionRegistry registry) {
    RootBeanDefinition definition = new RootBeanDefinition(WebTestClientRegistrar.class);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(WebTestClientRegistrar.class.getName(), definition);
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
   * {@link ConfigurationClassPostProcessor} and add a {@link WebTestClientFactory} bean
   * definition when a {@link WebTestClient} hasn't already been registered.
   */
  static class WebTestClientRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

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
      if (AotDetector.useGeneratedArtifacts()) {
        return;
      }
      if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory,
              WebTestClient.class, false, false).length == 0) {
        registry.registerBeanDefinition(WebTestClient.class.getName(),
                new RootBeanDefinition(WebTestClientFactory.class));
      }

    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }

  }

  /**
   * {@link FactoryBean} used to create and configure a {@link WebTestClient}.
   */
  public static class WebTestClientFactory implements FactoryBean<WebTestClient>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private WebTestClient object;

    private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "infra.framework.web.reactive.context.ReactiveWebApplicationContext";

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

    @Override
    public Class<?> getObjectType() {
      return WebTestClient.class;
    }

    @Override
    public WebTestClient getObject() throws Exception {
      if (this.object == null) {
        this.object = createWebTestClient();
      }
      return this.object;
    }

    private WebTestClient createWebTestClient() {
      boolean sslEnabled = isSslEnabled(this.applicationContext);
      String port = this.applicationContext.getEnvironment().getProperty("local.server.port", "8080");
      String baseUrl = getBaseUrl(sslEnabled, port);
      WebTestClient.Builder builder = WebTestClient.bindToServer();
      customizeWebTestClientBuilder(builder, this.applicationContext);
      customizeWebTestClientCodecs(builder, this.applicationContext);
      return builder.baseUrl(baseUrl).build();
    }

    private String getBaseUrl(boolean sslEnabled, String port) {
      String basePath = deduceBasePath();
      String pathSegment = (StringUtils.hasText(basePath)) ? basePath : "";
      return (sslEnabled ? "https" : "http") + "://localhost:" + port + pathSegment;
    }

    private String deduceBasePath() {
      ApplicationType webApplicationType = deduceFromApplicationContext(this.applicationContext.getClass());
      if (webApplicationType == ApplicationType.REACTIVE_WEB) {
        return this.applicationContext.getEnvironment().getProperty("infra.webflux.base-path");
      }
      return null;
    }

    static ApplicationType deduceFromApplicationContext(Class<?> applicationContextClass) {
      if (isAssignable(REACTIVE_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
        return ApplicationType.REACTIVE_WEB;
      }
      return ApplicationType.NORMAL;
    }

    private static boolean isAssignable(String target, Class<?> type) {
      try {
        return ClassUtils.resolveClassName(target, null).isAssignableFrom(type);
      }
      catch (Throwable ex) {
        return false;
      }
    }

    private boolean isSslEnabled(ApplicationContext context) {
      try {

        return false;
        // todo
//        var webServerFactory = context.getBean(WebServerFactory.class);
//        return Ssl.isEnabled(webServerFactory.getSsl());
      }
      catch (NoSuchBeanDefinitionException ex) {
        return false;
      }
    }

    private void customizeWebTestClientBuilder(WebTestClient.Builder clientBuilder, ApplicationContext context) {
      for (WebTestClientBuilderCustomizer customizer : context
              .getBeansOfType(WebTestClientBuilderCustomizer.class)
              .values()) {
        customizer.customize(clientBuilder);
      }
    }

    private void customizeWebTestClientCodecs(WebTestClient.Builder clientBuilder, ApplicationContext context) {
      Collection<CodecCustomizer> codecCustomizers = context.getBeansOfType(CodecCustomizer.class).values();
      if (CollectionUtils.isNotEmpty(codecCustomizers)) {
        clientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                .codecs(codecs -> codecCustomizers.forEach(codecCustomizer -> codecCustomizer.customize(codecs)))
                .build());
      }
    }

  }

}
