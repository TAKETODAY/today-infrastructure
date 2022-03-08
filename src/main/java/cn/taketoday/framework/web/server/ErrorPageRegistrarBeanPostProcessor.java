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

package cn.taketoday.framework.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;

/**
 * {@link BeanPostProcessor} that applies all {@link ErrorPageRegistrar}s from the bean
 * factory to {@link ErrorPageRegistry} beans.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
public class ErrorPageRegistrarBeanPostProcessor implements InitializationBeanPostProcessor, BeanFactoryAware {

  private BeanFactory beanFactory;

  private List<ErrorPageRegistrar> registrars;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof ErrorPageRegistry) {
      postProcessBeforeInitialization((ErrorPageRegistry) bean);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  private void postProcessBeforeInitialization(ErrorPageRegistry registry) {
    for (ErrorPageRegistrar registrar : getRegistrars()) {
      registrar.registerErrorPages(registry);
    }
  }

  private Collection<ErrorPageRegistrar> getRegistrars() {
    if (this.registrars == null) {
      // Look up does not include the parent context
      this.registrars = new ArrayList<>(
              this.beanFactory.getBeansOfType(ErrorPageRegistrar.class, false, false).values());
      this.registrars.sort(AnnotationAwareOrderComparator.INSTANCE);
      this.registrars = Collections.unmodifiableList(this.registrars);
    }
    return this.registrars;
  }

}
