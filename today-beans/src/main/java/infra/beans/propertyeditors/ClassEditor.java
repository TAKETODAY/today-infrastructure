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

package infra.beans.propertyeditors;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyEditorSupport;

import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Property editor for {@link Class java.lang.Class}, to enable the direct
 * population of a {@code Class} property without recourse to having to use a
 * String class name property as bridge.
 *
 * <p>Also supports "java.lang.String[]"-style array class names, in contrast to the
 * standard {@link Class#forName(String)} method.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see Class#forName
 * @see ClassUtils#forName(String, ClassLoader)
 * @since 4.0
 */
public class ClassEditor extends PropertyEditorSupport {

  @Nullable
  private final ClassLoader classLoader;

  /**
   * Create a default ClassEditor, using the thread context ClassLoader.
   */
  public ClassEditor() {
    this(null);
  }

  /**
   * Create a default ClassEditor, using the given ClassLoader.
   *
   * @param classLoader the ClassLoader to use
   * (or {@code null} for the thread context ClassLoader)
   */
  public ClassEditor(@Nullable ClassLoader classLoader) {
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    Class<?> clazz = (Class<?>) getValue();
    if (clazz != null) {
      return ClassUtils.getQualifiedName(clazz);
    }
    else {
      return "";
    }
  }

}
