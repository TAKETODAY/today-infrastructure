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

package infra.context.weaving;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.context.ConfigurableApplicationContext;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.lang.Assert;

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
