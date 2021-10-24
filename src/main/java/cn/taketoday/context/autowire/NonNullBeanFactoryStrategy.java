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

package cn.taketoday.context.autowire;

import java.lang.reflect.Parameter;

import cn.taketoday.beans.AbstractArgumentsResolvingStrategy;
import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;

/**
 * @author TODAY 2021/8/22 22:47
 * @since 4.0
 */
public abstract class NonNullBeanFactoryStrategy
        extends AbstractArgumentsResolvingStrategy implements ArgumentsResolvingStrategy {
  @Override
  protected boolean supportsArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
    BeanFactory beanFactory = resolvingContext.getBeanFactory();
    return beanFactory != null && supportsInternal(parameter, resolvingContext);
  }

  protected abstract boolean supportsInternal(
          Parameter parameter, ArgumentsResolvingContext resolvingContext);

  @Override
  protected final Object resolveInternal(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
    return resolveInternal(parameter, resolvingContext.getBeanFactory(), resolvingContext);
  }

  protected abstract Object resolveInternal(
          Parameter parameter, BeanFactory beanFactory, ArgumentsResolvingContext resolvingContext);

}
