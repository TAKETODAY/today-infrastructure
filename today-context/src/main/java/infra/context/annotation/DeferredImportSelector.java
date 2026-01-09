/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.type.AnnotationMetadata;

/**
 * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
 * have been processed. This type of selector can be particularly useful when the selected
 * imports are {@code @Conditional}.
 *
 * <p>Implementations can also extend the {@link Ordered}
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
