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

package infra.beans.factory.support;

import infra.beans.FatalBeanException;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.config.BeanDefinition;

/**
 * factory method error
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/31 22:35
 */
public class FactoryMethodBeanException extends FatalBeanException {

  private final String beanName;

  private final BeanDefinition merged;

  private final InjectionPoint injectionPoint;

  public FactoryMethodBeanException(BeanDefinition merged,
          InjectionPoint injectionPoint, String beanName, String message) {
    super(message);
    this.merged = merged;
    this.injectionPoint = injectionPoint;
    this.beanName = beanName;
  }

  public BeanDefinition getBeanDefinition() {
    return merged;
  }

  public InjectionPoint getInjectionPoint() {
    return injectionPoint;
  }

  public String getBeanName() {
    return beanName;
  }

}
