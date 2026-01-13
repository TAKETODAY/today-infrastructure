package infra.web.server.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.BootstrapContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.core.ssl.SslBundles;
import infra.core.type.AnnotationMetadata;
import infra.stereotype.Component;
import infra.util.ObjectUtils;
import infra.web.server.ServerProperties;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 14:51
 */
@Import(WebServerConfiguration.BeanPostProcessorsRegistrar.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerProperties.class)
public class WebServerConfiguration {

  @Component
  static DefaultWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(ServerProperties serverProperties,
          @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    return new DefaultWebServerFactoryCustomizer(serverProperties, sslBundles, applicationTemp);
  }

  /**
   * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
   * {@link ImportBeanDefinitionRegistrar} for early registration.
   */
  @DisableDependencyInjection
  public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      ConfigurableBeanFactory beanFactory = context.getBeanFactory();
      if (ObjectUtils.isEmpty(beanFactory.getBeanNamesForType(
              WebServerFactoryCustomizerBeanPostProcessor.class, true, false))) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(WebServerFactoryCustomizerBeanPostProcessor.class);
        beanDefinition.setSynthetic(true);
        beanDefinition.setEnableDependencyInjection(false);
        context.registerBeanDefinition("webServerFactoryCustomizerBeanPostProcessor", beanDefinition);
      }
    }

  }

}
