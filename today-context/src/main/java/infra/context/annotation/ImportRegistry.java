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

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import infra.core.type.AnnotationMetadata;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;

/**
 * Registry of imported class {@link AnnotationMetadata}.
 * ImportStack
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ImportRegistry extends ArrayDeque<ConfigurationClass> {

  @Serial
  private static final long serialVersionUID = 1L;

  private final MultiValueMap<String, AnnotationMetadata> imports = MultiValueMap.forLinkedHashMap();

  public void registerImport(AnnotationMetadata importingClass, String importedClass) {
    this.imports.add(importedClass, importingClass);
  }

  @Nullable
  public AnnotationMetadata getImportingClassFor(String importedClass) {
    return CollectionUtils.lastElement(imports.get(importedClass));
  }

  public void removeImportingClass(String importingClass) {
    for (List<AnnotationMetadata> list : imports.values()) {
      Iterator<AnnotationMetadata> iterator = list.iterator();
      while (iterator.hasNext()) {
        if (iterator.next().getClassName().equals(importingClass)) {
          iterator.remove();
          break;
        }
      }
    }
  }

  /**
   * Given a stack containing (in order)
   * <ul>
   * <li>com.acme.Foo</li>
   * <li>com.acme.Bar</li>
   * <li>com.acme.Baz</li>
   * </ul>
   * return "[Foo->Bar->Baz]".
   */
  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner("->", "[", "]");
    for (ConfigurationClass configurationClass : this) {
      joiner.add(configurationClass.getSimpleName());
    }
    return joiner.toString();
  }

}
