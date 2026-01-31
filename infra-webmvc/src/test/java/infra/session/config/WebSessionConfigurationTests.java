package infra.session.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/31 11:30
 */
class WebSessionConfigurationTests {

  @Test
  void isEnableDependencyInjection() {
    try (var context = new AnnotationConfigApplicationContext(WebSessionConfiguration.class)) {
      BeanDefinition definition = context.getBeanDefinition(WebSessionConfiguration.class);
      assertThat(definition.isEnableDependencyInjection()).isFalse();
    }
  }

}