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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.event.ApplicationEventMulticaster;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;

/**
 * {@code BeanPostProcessor} that detects beans which implement the {@code ApplicationListener}
 * interface. This catches beans that can't reliably be detected by {@code getBeanNamesForType}
 * and related operations which only work against top-level beans.
 *
 * <p>With standard Java serialization, this post-processor won't get serialized as part of
 * {@code DisposableBeanAdapter} to begin with. However, with alternative serialization
 * mechanisms, {@code DisposableBeanAdapter.writeReplace} might not get used at all, so we
 * defensively mark this post-processor's field state as {@code transient}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class ApplicationListenerDetector
        implements DestructionAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor, InitializationBeanPostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationListenerDetector.class);

  private final transient AbstractApplicationContext applicationContext;

  private final transient Map<String, Boolean> singletonNames = new ConcurrentHashMap<>(256);

  ApplicationListenerDetector(AbstractApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Object bean, String beanName) {
    if (bean instanceof ApplicationListener) {
      this.singletonNames.put(beanName, beanDefinition.isSingleton());
    }
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    // FIXME 优化 singletonNames
    if (bean instanceof ApplicationListener) {
      // potentially not detected as a listener by getBeanNamesForType retrieval
      Boolean flag = singletonNames.get(beanName);
      if (Boolean.TRUE.equals(flag)) {
        // singleton bean (top-level or inner): register on the fly
        applicationContext.addApplicationListener((ApplicationListener<?>) bean);
      }
      else if (Boolean.FALSE.equals(flag)) {
        if (logger.isWarnEnabled() && !applicationContext.containsBean(beanName)) {
          // inner bean with other scope - can't reliably process events
          logger.warn("Inner bean '{}' implements ApplicationListener interface " +
                  "but is not reachable for event multicasting by its containing ApplicationContext " +
                  "because it does not have singleton scope. Only top-level listener beans are allowed " +
                  "to be of non-singleton scope.", beanName);
        }
        singletonNames.remove(beanName);
      }
    }
    return bean;
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) {
    if (bean instanceof ApplicationListener) {
      try {
        ApplicationEventMulticaster multicaster = this.applicationContext.getApplicationEventMulticaster();
        multicaster.removeApplicationListener((ApplicationListener<?>) bean);
        multicaster.removeApplicationListenerBean(beanName);
      }
      catch (IllegalStateException ex) {
        // ApplicationEventMulticaster not initialized yet - no need to remove a listener
      }
    }
  }

  @Override
  public boolean requiresDestruction(Object bean) {
    return (bean instanceof ApplicationListener);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ApplicationListenerDetector &&
            this.applicationContext == ((ApplicationListenerDetector) other).applicationContext));
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.applicationContext);
  }

}
