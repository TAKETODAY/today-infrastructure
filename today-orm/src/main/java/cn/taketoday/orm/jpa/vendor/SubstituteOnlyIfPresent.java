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

package cn.taketoday.orm.jpa.vendor;

import java.util.function.Predicate;

/**
 * Predicate intended to enable the related GraalVM substitution
 * only when the class is present on the classpath.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SubstituteOnlyIfPresent implements Predicate<String> {

  @Override
  public boolean test(String type) {
    try {
      Class.forName(type, false, getClass().getClassLoader());
      return true;
    }
    catch (ClassNotFoundException | NoClassDefFoundError ex) {
      return false;
    }
  }
}
