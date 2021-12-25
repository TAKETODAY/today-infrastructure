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

package cn.taketoday.aop.proxy;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * Auto-proxy creator that considers infrastructure Advisor beans only,
 * ignoring any application-defined Advisors.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class InfrastructureAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Override
  protected void initBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.initBeanFactory(beanFactory);
    this.beanFactory = beanFactory;
  }

  @Override
  protected boolean isEligibleAdvisorBean(String beanName) {
    if (beanFactory != null) {
      BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
      if (definition != null) {
        return definition.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE;
      }
    }
    return false;
  }

}
