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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.properties.ConfigurationPropertiesBean.BindMethod;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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

  private ApplicationContext applicationContext;

  private BeanDefinitionRegistry registry;

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
    bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
    return bean;
  }

  private void bind(@Nullable ConfigurationPropertiesBean bean) {
    if (bean == null || hasBoundValueObject(bean.getName())) {
      return;
    }
    if (bean.getBindMethod() != BindMethod.JAVA_BEAN) {
      throw new IllegalStateException("Cannot bind @ConfigurationProperties for bean '"
              + bean.getName() + "'. Ensure that @ConstructorBinding has not been applied to regular bean");
    }
    try {
      this.binder.bind(bean);
    }
    catch (Exception ex) {
      throw new ConfigurationPropertiesBindException(bean, ex);
    }
  }

  private boolean hasBoundValueObject(String beanName) {
    BeanDefinition definition = registry.getBeanDefinition(beanName);
    if (definition != null) {
      return BindMethod.VALUE_OBJECT.equals(definition.getAttribute(BindMethod.class.getName()));
    }
    return false;
  }

  /**
   * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
   * already registered.
   *
   * @param registry the bean definition registry
   */
  public static void register(BeanDefinitionRegistry registry) {
    Assert.notNull(registry, "Registry must not be null");
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      BeanDefinition definition = new BeanDefinition(ConfigurationPropertiesBindingPostProcessor.class);
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      registry.registerBeanDefinition(BEAN_NAME, definition);
    }
    ConfigurationPropertiesBinder.register(registry);
  }

}
