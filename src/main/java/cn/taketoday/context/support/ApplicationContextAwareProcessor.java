/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.support;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.MessageSourceAware;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.ApplicationEventPublisherAware;
import cn.taketoday.context.aware.BootstrapContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.context.expression.EmbeddedValueResolver;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.lang.Nullable;

/**
 * {@link BeanPostProcessor} implementation that supplies the {@code ApplicationContext},
 * {@link cn.taketoday.core.env.Environment Environment} for the {@code ApplicationContext}
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
final class ApplicationContextAwareProcessor implements InitializationBeanPostProcessor {
  private final ConfigurableApplicationContext context;
  private final EmbeddedValueResolver embeddedValueResolver;
  private final BootstrapContext bootstrapContext;

  /**
   * Create a new ApplicationContextAwareProcessor for the given context.
   */
  public ApplicationContextAwareProcessor(
          ConfigurableApplicationContext applicationContext, BootstrapContext bootstrapContext) {
    this.context = applicationContext;
    this.embeddedValueResolver = new EmbeddedValueResolver(applicationContext.getBeanFactory());
    this.bootstrapContext = bootstrapContext;
  }

  @Nullable
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

    if (bean instanceof ApplicationContextAware aware) {
      aware.setApplicationContext(context);
    }
    if (bean instanceof MessageSourceAware aware) {
      aware.setMessageSource(context);
    }
    if (bean instanceof EmbeddedValueResolverAware aware) {
      aware.setEmbeddedValueResolver(this.embeddedValueResolver);
    }
  }

}
