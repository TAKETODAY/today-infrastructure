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

package infra.aop.target;

import java.io.Serial;

import infra.beans.factory.BeanFactory;

/**
 * Simple {@link infra.aop.TargetSource} implementation,
 * freshly obtaining the specified target bean from its containing
 * {@link BeanFactory}.
 *
 * <p>Can obtain any kind of target bean: singleton, scoped, or prototype.
 * Typically, used for scoped beans.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:27
 * @since 3.0
 */
public class SimpleBeanTargetSource extends AbstractBeanFactoryTargetSource {
  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  public Object getTarget() {
    return getBeanFactory().getBean(getTargetBeanName());
  }

}
