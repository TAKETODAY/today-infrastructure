/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;

/**
 * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
 * have been processed. This type of selector can be particularly useful when the selected
 * imports are {@code @Conditional}.
 *
 * <p>Implementations can also extend the {@link cn.taketoday.core.Ordered}
 * interface or use the {@link Order} annotation to
 * indicate a precedence against other {@link DeferredImportSelector DeferredImportSelectors}.
 *
 * <p>Implementations may also provide an {@link #getImportGroup() import group} which
 * can provide additional sorting and filtering logic across different selectors.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface DeferredImportSelector extends ImportSelector {

  /**
   * Return a specific import group.
   * <p>The default implementations return {@code null} for no grouping required.
   *
   * @return the import group class, or {@code null} if none
   */
  @Nullable
  default Class<? extends Group> getImportGroup() {
    return null;
  }

  /**
   * Interface used to group results from different import selectors.
   */
  interface Group {

    /**
     * Process the {@link AnnotationMetadata} of the importing @{@link Configuration}
     * class using the specified {@link DeferredImportSelector}.
     */
    void process(AnnotationMetadata metadata, DeferredImportSelector selector);

    /**
     * Return the {@link Entry entries} of which class(es) should be imported
     * for this group.
     */
    Iterable<Entry> selectImports();

    /**
     * An entry that holds the {@link AnnotationMetadata} of the importing
     * {@link Configuration} class and the class name to import.
     *
     * @param metadata Return the {@link AnnotationMetadata} of the importing
     * {@link Configuration} class.
     * @param importClassName Return the fully qualified name of the class to import.
     */
    record Entry(AnnotationMetadata metadata, String importClassName) {

      @Override
      public String toString() {
        return this.importClassName;
      }
    }
  }

}
