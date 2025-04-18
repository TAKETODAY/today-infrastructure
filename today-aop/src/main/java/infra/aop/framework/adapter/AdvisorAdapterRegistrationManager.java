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
