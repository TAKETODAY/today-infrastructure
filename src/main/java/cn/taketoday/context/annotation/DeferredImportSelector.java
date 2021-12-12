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

package cn.taketoday.context.annotation;

import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;

/**
 * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
 * have been processed. This type of selector can be particularly useful when the selected
 * imports are {@code @Conditional}.
 *
 * <p>Implementations can also extend the {@link cn.taketoday.core.Ordered}
 * interface or use the {@link cn.taketoday.core.Order} annotation to
 * indicate a precedence against other {@link DeferredImportSelector DeferredImportSelectors}.
 *
 * <p>Implementations may also provide an {@link #getImportGroup() import group} which
 * can provide additional sorting and filtering logic across different selectors.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
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
     */
    class Entry {

      private final AnnotationMetadata metadata;
      private final String importClassName;

      public Entry(AnnotationMetadata metadata, String importClassName) {
        this.metadata = metadata;
        this.importClassName = importClassName;
      }

      /**
       * Return the {@link AnnotationMetadata} of the importing
       * {@link Configuration} class.
       */
      public AnnotationMetadata getMetadata() {
        return this.metadata;
      }

      /**
       * Return the fully qualified name of the class to import.
       */
      public String getImportClassName() {
        return this.importClassName;
      }

      @Override
      public boolean equals(@Nullable Object other) {
        if (this == other) {
          return true;
        }
        if (other == null || getClass() != other.getClass()) {
          return false;
        }
        Entry entry = (Entry) other;
        return (this.metadata.equals(entry.metadata) && this.importClassName.equals(entry.importClassName));
      }

      @Override
      public int hashCode() {
        return (this.metadata.hashCode() * 31 + this.importClassName.hashCode());
      }

      @Override
      public String toString() {
        return this.importClassName;
      }
    }
  }

}
