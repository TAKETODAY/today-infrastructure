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

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;

/**
 * Registry of imported class {@link AnnotationMetadata}.
 * ImportStack
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 4.0
 */
final class ImportRegistry extends ArrayDeque<ConfigurationClass> {

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
