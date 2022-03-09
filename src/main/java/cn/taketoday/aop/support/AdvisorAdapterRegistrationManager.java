/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop.support;

import cn.taketoday.aop.framework.AdvisorAdapter;
import cn.taketoday.aop.framework.DefaultAdvisorAdapterRegistry;
import cn.taketoday.aop.proxy.AdvisorAdapterRegistry;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;

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
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof AdvisorAdapter) {
      this.advisorAdapterRegistry.registerAdvisorAdapter((AdvisorAdapter) bean);
    }
    return bean;
  }

}
