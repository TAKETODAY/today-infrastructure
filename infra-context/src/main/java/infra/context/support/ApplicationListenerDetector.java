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

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.DestructionAwareBeanPostProcessor;
import infra.beans.factory.support.MergedBeanDefinitionPostProcessor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationListener;
import infra.context.event.ApplicationEventMulticaster;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ObjectUtils;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    if (ApplicationListener.class.isAssignableFrom(beanType)) {
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
