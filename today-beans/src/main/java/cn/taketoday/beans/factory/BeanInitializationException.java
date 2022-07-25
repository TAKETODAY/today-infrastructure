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
package cn.taketoday.beans.factory;

import java.io.Serial;

import cn.taketoday.beans.FatalBeanException;

/**
 * Exception that a bean implementation is suggested to throw if its own
 * factory-aware initialization code fails. BeansExceptions thrown by
 * bean factory methods themselves should simply be propagated as-is.
 *
 * <p>Note that {@code afterPropertiesSet()} or a custom "init-method"
 * can throw any exception.
 *
 * @author Juergen Hoeller
 * @author TODAY 2020-02-18 19:10
 * @see BeanFactoryAware#setBeanFactory
 * @see InitializingBean#afterPropertiesSet
 */
public class BeanInitializationException extends FatalBeanException {
  @Serial
  private static final long serialVersionUID = 1L;

  public BeanInitializationException() { }

  public BeanInitializationException(String message) {
    super(message);
  }

  public BeanInitializationException(Throwable cause) {
    super(cause);
  }

  public BeanInitializationException(String message, Throwable cause) {
    super(message, cause);
  }

}
