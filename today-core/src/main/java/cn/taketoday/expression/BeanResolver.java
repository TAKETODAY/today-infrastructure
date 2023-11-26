/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.expression;

/**
 * A bean resolver can be registered with the evaluation context and will kick in
 * for bean references: {@code @myBeanName} and {@code &myBeanName} expressions.
 * The {@code &} variant syntax allows access to the factory bean where relevant.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface BeanResolver {

  /**
   * Look up a bean by the given name and return a corresponding instance for it.
   * For attempting access to a factory bean, the name needs a {@code &} prefix.
   *
   * @param context the current evaluation context
   * @param beanName the name of the bean to look up
   * @return an object representing the bean
   * @throws AccessException if there is an unexpected problem resolving the bean
   */
  Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
