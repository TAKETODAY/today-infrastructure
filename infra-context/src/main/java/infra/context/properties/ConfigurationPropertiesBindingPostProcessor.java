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

package infra.context.properties;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.properties.bind.BindMethod;
import infra.core.Ordered;
import infra.core.PriorityOrdered;
import infra.core.env.PropertySources;
import infra.lang.Assert;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConfigurationPropertiesBindingPostProcessor
        implements InitializationBeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

  /**
   * The bean name that this post-processor is registered with.
   */
  public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

  @SuppressWarnings("NullAway.Init")
  private ApplicationContext applicationContext;

  @SuppressWarnings("NullAway.Init")
  private BeanDefinitionRegistry registry;

  @SuppressWarnings("NullAway.Init")
  private ConfigurationPropertiesBinder binder;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    // We can't use constructor injection of the application context because
    // it causes eager factory bean initialization
    this.registry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
    this.binder = ConfigurationPropertiesBinder.get(applicationContext);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (!hasBoundValueObject(beanName)) {
      bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
    }
    return bean;
  }

  private boolean hasBoundValueObject(String beanName) {
    return BindMethod.VALUE_OBJECT.equals(BindMethodAttribute.get(this.registry, beanName));
  }

  private void bind(@Nullable ConfigurationPropertiesBean bean) {
    if (bean == null) {
      return;
    }
    if (bean.asBindTarget().getBindMethod() == BindMethod.VALUE_OBJECT) {
      throw new IllegalStateException("Cannot bind @ConfigurationProperties for bean '%s'. Ensure that @ConstructorBinding has not been applied to regular bean"
              .formatted(bean.getName()));
    }
    try {
      this.binder.bind(bean);
    }
    catch (Exception ex) {
      throw new ConfigurationPropertiesBindException(bean, ex);
    }
  }

  /**
   * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
   * already registered.
   *
   * @param registry the bean definition registry
   */
  public static void register(BeanDefinitionRegistry registry) {
    Assert.notNull(registry, "Registry is required");
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      BeanDefinition definition = new RootBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class);
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      definition.setEnableDependencyInjection(false);
      registry.registerBeanDefinition(BEAN_NAME, definition);
    }
    ConfigurationPropertiesBinder.register(registry);
  }

}
