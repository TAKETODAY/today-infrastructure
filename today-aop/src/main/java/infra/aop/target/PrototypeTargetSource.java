/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.target;

import java.io.Serial;

import infra.beans.factory.BeanFactory;

/**
 * {@link infra.aop.TargetSource} implementation that
 * creates a new instance of the target bean for each request,
 * destroying each instance on release (after each request).
 *
 * <p>Obtains bean instances from its containing
 * {@link BeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:46
 * @see #setBeanFactory
 * @see #setTargetBeanName
 * @since 3.0
 */
public class PrototypeTargetSource extends AbstractPrototypeTargetSource {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Obtain a new prototype instance for every call.
   *
   * @see #newPrototypeInstance()
   */
  @Override
  public Object getTarget() {
    return newPrototypeInstance();
  }

  /**
   * Destroy the given independent instance.
   *
   * @see #destroyPrototypeInstance
   */
  @Override
  public void releaseTarget(Object target) {
    destroyPrototypeInstance(target);
  }

  @Override
  public String toString() {
    return "PrototypeTargetSource for target bean with name '" + targetBeanName + "'";
  }

}
