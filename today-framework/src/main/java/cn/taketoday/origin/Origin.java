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

package cn.taketoday.origin;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * Interface that uniquely represents the origin of an item. For example, an item loaded
 * from a {@link File} may have an origin made up of the file name along with line/column
 * numbers.
 * <p>
 * Implementations must provide sensible {@code hashCode()}, {@code equals(...)} and
 * {@code #toString()} implementations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OriginProvider
 * @see TextResourceOrigin
 * @since 4.0
 */
public interface Origin {

  /**
   * Return the parent origin for this instance if there is one. The parent origin
   * provides the origin of the item that created this one.
   *
   * @return the parent origin or {@code null}
   * @see Origin#parentsFrom(Object)
   */
  @Nullable
  default Origin getParent() {
    return null;
  }

  /**
   * Find the {@link Origin} that an object originated from. Checks if the source object
   * is an {@link Origin} or {@link OriginProvider} and also searches exception stacks.
   *
   * @param source the source object or {@code null}
   * @return an optional {@link Origin}
   */
  @Nullable
  static Origin from(@Nullable Object source) {
    if (source instanceof Origin) {
      return (Origin) source;
    }
    Origin origin = null;
    if (source instanceof OriginProvider) {
      origin = ((OriginProvider) source).getOrigin();
    }
    if (origin == null && source instanceof Throwable) {
      return from(((Throwable) source).getCause());
    }
    return origin;
  }

  /**
   * Find the parents of the {@link Origin} that an object originated from. Checks if
   * the source object is an {@link Origin} or {@link OriginProvider} and also searches
   * exception stacks. Provides a list of all parents up to root {@link Origin},
   * starting with the most immediate parent.
   *
   * @param source the source object or {@code null}
   * @return a list of parents or an empty list if the source is {@code null}, has no
   * origin, or no parent
   */
  static List<Origin> parentsFrom(@Nullable Object source) {
    Origin origin = from(source);
    if (origin == null) {
      return Collections.emptyList();
    }
    LinkedHashSet<Origin> parents = new LinkedHashSet<>();
    origin = origin.getParent();
    while (origin != null && !parents.contains(origin)) {
      parents.add(origin);
      origin = origin.getParent();
    }
    return List.copyOf(parents);
  }

}
