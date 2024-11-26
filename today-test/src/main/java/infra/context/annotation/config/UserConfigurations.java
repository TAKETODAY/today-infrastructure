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

package infra.context.annotation.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import infra.core.Ordered;
import infra.core.PriorityOrdered;

/**
 * {@link Configurations} representing user-defined {@code @Configuration} classes (i.e.
 * those defined in classes usually written by the user).
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:13
 */
public class UserConfigurations extends Configurations implements PriorityOrdered {

  protected UserConfigurations(Collection<Class<?>> classes) {
    super(classes);
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  protected UserConfigurations merge(Set<Class<?>> mergedClasses) {
    return new UserConfigurations(mergedClasses);
  }

  public static UserConfigurations of(Class<?>... classes) {
    return new UserConfigurations(Arrays.asList(classes));
  }

}

