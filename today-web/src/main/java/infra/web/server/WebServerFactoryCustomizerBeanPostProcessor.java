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

package infra.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.lang.Nullable;
import infra.util.LambdaSafe;

/**
 * {@link BeanPostProcessor} that applies all {@link WebServerFactoryCustomizer} beans
 * from the bean factory to {@link WebServerFactory} beans.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebServerFactoryCustomizerBeanPostProcessor
        implements InitializationBeanPostProcessor, BeanFactoryAware {

  private BeanFactory beanFactory;

  @Nullable
  private List<WebServerFactoryCustomizer<?>> customizers;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof WebServerFactory) {
      postProcessBeforeInitialization((WebServerFactory) bean);
    }
    return bean;
  }

  @SuppressWarnings("unchecked")
  private void postProcessBeforeInitialization(WebServerFactory webServerFactory) {
    LambdaSafe.callbacks(WebServerFactoryCustomizer.class, getCustomizers(), webServerFactory)
            .withLogger(WebServerFactoryCustomizerBeanPostProcessor.class)
            .invoke(customizer -> customizer.customize(webServerFactory));
  }

  private Collection<WebServerFactoryCustomizer<?>> getCustomizers() {
    if (this.customizers == null) {
      // Look up does not include the parent context
      this.customizers = new ArrayList<>(getWebServerFactoryCustomizerBeans());
      this.customizers.sort(AnnotationAwareOrderComparator.INSTANCE);
      this.customizers = Collections.unmodifiableList(this.customizers);
    }
    return this.customizers;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Collection<WebServerFactoryCustomizer<?>> getWebServerFactoryCustomizerBeans() {
    return (Collection) this.beanFactory.getBeansOfType(
            WebServerFactoryCustomizer.class, false, false).values();
  }

}
