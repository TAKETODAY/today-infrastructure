/*
 * Copyright 2002-present the original author or authors.
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

package infra.context.support;

import infra.beans.BeansException;
import infra.beans.factory.Aware;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.config.EmbeddedValueResolver;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationEventPublisherAware;
import infra.context.BootstrapContext;
import infra.context.BootstrapContextAware;
import infra.context.ConfigurableApplicationContext;
import infra.context.EnvironmentAware;
import infra.context.MessageSourceAware;
import infra.context.ResourceLoaderAware;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.env.Environment;

/**
 * {@link BeanPostProcessor} implementation that supplies the {@code ApplicationContext},
 * {@link Environment Environment} for the {@code ApplicationContext}
 * to beans that implement the {@link EnvironmentAware}, {@link ResourceLoaderAware},
 * {@link BootstrapContextAware},{@link ApplicationEventPublisherAware} and/or
 * {@link ApplicationContextAware} interfaces.
 *
 * <p>Implemented interfaces are satisfied in the order in which they are
 * mentioned above.
 *
 * <p>Application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @author TODAY 2021/10/7 17:02
 * @see EnvironmentAware
 * @see ResourceLoaderAware
 * @see ApplicationEventPublisherAware
 * @see ApplicationContextAware
 * @see AbstractApplicationContext#refresh()
 * @since 4.0
 */
final class ContextAwareProcessor implements InitializationBeanPostProcessor {
  private final ConfigurableApplicationContext context;
  private final EmbeddedValueResolver embeddedValueResolver;
  private final BootstrapContext bootstrapContext;

  /**
   * Create a new ApplicationContextAwareProcessor for the given context.
   */
  ContextAwareProcessor(ConfigurableApplicationContext applicationContext, BootstrapContext bootstrapContext) {
    this.context = applicationContext;
    this.embeddedValueResolver = new EmbeddedValueResolver(applicationContext.getBeanFactory());
    this.bootstrapContext = bootstrapContext;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof Aware) {
      awareInternal(bean);
    }
    return bean;
  }

  private void awareInternal(Object bean) {
    if (bean instanceof BootstrapContextAware aware) {
      aware.setBootstrapContext(bootstrapContext);
    }
    if (bean instanceof EnvironmentAware aware) {
      aware.setEnvironment(context.getEnvironment());
    }
    if (bean instanceof ResourceLoaderAware aware) {
      aware.setResourceLoader(context);
    }
    if (bean instanceof ApplicationEventPublisherAware aware) {
      aware.setApplicationEventPublisher(context);
    }
    if (bean instanceof MessageSourceAware aware) {
      aware.setMessageSource(context);
    }
    if (bean instanceof EmbeddedValueResolverAware aware) {
      aware.setEmbeddedValueResolver(this.embeddedValueResolver);
    }
    if (bean instanceof ApplicationContextAware aware) {
      aware.setApplicationContext(context);
    }
  }

}
