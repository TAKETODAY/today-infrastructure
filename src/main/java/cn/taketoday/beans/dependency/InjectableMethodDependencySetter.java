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

package cn.taketoday.beans.dependency;

import java.lang.reflect.Method;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.util.ReflectionUtils;

/**
 * <pre>
 *   &#64Autowired
 * //  @Autowired
 * //  @Inject
 *   public void setUserRepository1(UserRepository userRepository1) {
 *     this.userRepository1 = userRepository1;
 *   }
 * </pre>
 *
 * @author TODAY 2021/11/15 23:01
 * @since 4.0
 */
public record InjectableMethodDependencySetter(Method method) implements DependencySetter {

  @Override
  public void applyTo(Object bean, ConfigurableBeanFactory beanFactory) {
    ArgumentsResolver argumentsResolver = beanFactory.getArgumentsResolver();
    Object[] args = argumentsResolver.resolve(method);
    ReflectionUtils.invokeMethod(method, bean, args);
  }
}
