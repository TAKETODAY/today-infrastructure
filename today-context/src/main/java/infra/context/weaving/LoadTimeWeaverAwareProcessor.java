/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.weaving;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.context.ConfigurableApplicationContext;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * {@link BeanPostProcessor}
 * implementation that passes the context's default {@link LoadTimeWeaver}
 * to beans that implement the {@link LoadTimeWeaverAware} interface.
 *
 * <p>{@link infra.context.ApplicationContext Application contexts}
 * will automatically register this with their underlying {@link BeanFactory bean factory},
 * provided that a default {@code LoadTimeWeaver} is actually available.
 *
 * <p>Applications should not use this class directly.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LoadTimeWeaverAware
 * @see infra.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 * @since 4.0
 */
public class LoadTimeWeaverAwareProcessor implements InitializationBeanPostProcessor, BeanFactoryAware {

  @Nullable
  private LoadTimeWeaver loadTimeWeaver;

  @Nullable
  private BeanFactory beanFactory;

  /**
   * Create a new {@code LoadTimeWeaverAwareProcessor} that will
   * auto-retrieve the {@link LoadTimeWeaver} from the containing
   * {@link BeanFactory}, expecting a bean named
   * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
   */
  public LoadTimeWeaverAwareProcessor() {
  }

  /**
   * Create a new {@code LoadTimeWeaverAwareProcessor} for the given
   * {@link LoadTimeWeaver}.
   * <p>If the given {@code loadTimeWeaver} is {@code null}, then a
   * {@code LoadTimeWeaver} will be auto-retrieved from the containing
   * {@link BeanFactory}, expecting a bean named
   * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
   *
   * @param loadTimeWeaver the specific {@code LoadTimeWeaver} that is to be used
   */
  public LoadTimeWeaverAwareProcessor(@Nullable LoadTimeWeaver loadTimeWeaver) {
    this.loadTimeWeaver = loadTimeWeaver;
  }

  /**
   * Create a new {@code LoadTimeWeaverAwareProcessor}.
   * <p>The {@code LoadTimeWeaver} will be auto-retrieved from
   * the given {@link BeanFactory}, expecting a bean named
   * {@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}.
   *
   * @param beanFactory the BeanFactory to retrieve the LoadTimeWeaver from
   */
  public LoadTimeWeaverAwareProcessor(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof LoadTimeWeaverAware) {
      LoadTimeWeaver ltw = this.loadTimeWeaver;
      if (ltw == null) {
        Assert.state(this.beanFactory != null,
                "BeanFactory required if no LoadTimeWeaver explicitly specified");
        ltw = this.beanFactory.getBean(
                ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
      }
      ((LoadTimeWeaverAware) bean).setLoadTimeWeaver(ltw);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String name) {
    return bean;
  }

}
