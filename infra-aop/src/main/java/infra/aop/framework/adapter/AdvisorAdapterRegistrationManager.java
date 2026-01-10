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

package infra.aop.framework.adapter;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;

/**
 * BeanPostProcessor that registers {@link AdvisorAdapter} beans in the BeanFactory with
 * an {@link AdvisorAdapterRegistry} (by default the {@link DefaultAdvisorAdapterRegistry}).
 *
 * <p>The only requirement for it to work is that it needs to be defined
 * in application context along with "non-native" AdvisorAdapters that
 * need to be "recognized" by AOP framework.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @see #setAdvisorAdapterRegistry
 * @see AdvisorAdapter
 * @since 4.0
 */
public class AdvisorAdapterRegistrationManager implements InitializationBeanPostProcessor {

  private AdvisorAdapterRegistry advisorAdapterRegistry = DefaultAdvisorAdapterRegistry.getInstance();

  /**
   * Specify the AdvisorAdapterRegistry to register AdvisorAdapter beans with.
   * Default is the global AdvisorAdapterRegistry.
   *
   * @see DefaultAdvisorAdapterRegistry
   */
  public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
    this.advisorAdapterRegistry = advisorAdapterRegistry;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof AdvisorAdapter) {
      this.advisorAdapterRegistry.registerAdvisorAdapter((AdvisorAdapter) bean);
    }
    return bean;
  }

}
