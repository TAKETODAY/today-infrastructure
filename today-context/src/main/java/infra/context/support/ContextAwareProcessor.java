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
