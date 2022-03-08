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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.config.BeanDefinition;

/**
 * A subclass of {@link BeanCreationException} which indicates that the target scope
 * is not active, e.g. in case of request or session scope.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory#getBean
 * @see Scope
 * @see BeanDefinition#setScope
 * @since 4.0 2022/1/9 23:23
 */
@SuppressWarnings("serial")
public class ScopeNotActiveException extends BeanCreationException {

  /**
   * Create a new ScopeNotActiveException.
   *
   * @param beanName the name of the bean requested
   * @param scopeName the name of the target scope
   * @param cause the root cause, typically from {@link Scope#get}
   */
  public ScopeNotActiveException(String beanName, String scopeName, IllegalStateException cause) {
    super(beanName, "Scope '" + scopeName + "' is not active for the current thread; consider " +
            "defining a scoped proxy for this bean if you intend to refer to it from a singleton", cause);
  }

}
