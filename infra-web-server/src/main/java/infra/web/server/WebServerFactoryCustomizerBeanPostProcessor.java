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

package infra.web.server;

import org.jspecify.annotations.Nullable;

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

  @SuppressWarnings("NullAway")
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
