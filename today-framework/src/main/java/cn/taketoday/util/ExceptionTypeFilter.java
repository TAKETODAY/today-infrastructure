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

package cn.taketoday.util;

import java.util.Collection;

/**
 * An {@link InstanceFilter} implementation that handles exception types. A type
 * will match against a given candidate if it is assignable to that candidate.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 20:40
 */
public class ExceptionTypeFilter extends InstanceFilter<Class<? extends Throwable>> {

  public ExceptionTypeFilter(Collection<? extends Class<? extends Throwable>> includes,
          Collection<? extends Class<? extends Throwable>> excludes, boolean matchIfEmpty) {

    super(includes, excludes, matchIfEmpty);
  }

  @Override
  protected boolean match(Class<? extends Throwable> instance, Class<? extends Throwable> candidate) {
    return candidate.isAssignableFrom(instance);
  }

}
