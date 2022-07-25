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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

  @Override
  public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
    Class<?> type = bindable.getType().resolve();
    if (bindable.getValue() != null || type == null) {
      return null;
    }
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
      return constructors[0];
    }
    Constructor<?> constructor = null;
    for (Constructor<?> candidate : constructors) {
      if (!Modifier.isPrivate(candidate.getModifiers())) {
        if (constructor != null) {
          return null;
        }
        constructor = candidate;
      }
    }
    if (constructor != null && constructor.getParameterCount() > 0) {
      return constructor;
    }
    return null;
  }

}
