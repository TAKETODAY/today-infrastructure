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

package infra.beans;

import org.jspecify.annotations.Nullable;

import infra.core.conversion.ConversionService;

/**
 * Interface that encapsulates configuration methods for a {@link PropertyAccessor}.
 * Also extends the {@link PropertyEditorRegistry} interface for
 * {@link java.beans.PropertyEditor} management.
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:37
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

  /**
   * Default maximum nested property path depth: {@value}.
   * <p>This limit turns excessive recursive traversal into an
   * {@link InvalidPropertyException}, but cannot guarantee protection against
   * {@link StackOverflowError} for every JVM stack size, JIT state, and platform.
   * @since 5.0
   * @see #setMaxNestedPathDepth(int)
   */
  int DEFAULT_MAX_NESTED_PATH_DEPTH = 100;

  /**
   * Specify a ConversionService to use for converting
   * property values, as an alternative to JavaBeans PropertyEditors.
   */
  void setConversionService(@Nullable ConversionService conversionService);

  /**
   * Return the associated ConversionService, if any.
   */
  @Nullable
  ConversionService getConversionService();

  /**
   * Set whether to extract the old property value when applying a
   * property editor to a new value for a property.
   */
  void setExtractOldValueForEditor(boolean extractOldValueForEditor);

  /**
   * Return whether to extract the old property value when applying a
   * property editor to a new value for a property.
   */
  boolean isExtractOldValueForEditor();

  /**
   * Set whether this instance should attempt to "auto-grow" a
   * nested path that contains a {@code null} value.
   * <p>If {@code true}, a {@code null} path location will be populated
   * with a default object value and traversed instead of resulting in a
   * {@link NullValueInNestedPathException}.
   * <p>Default is {@code false} on a plain PropertyAccessor instance.
   */
  void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

  /**
   * Return whether "auto-growing" of nested paths has been activated.
   */
  boolean isAutoGrowNestedPaths();

  /**
   * Specify a limit for array and collection auto-growing.
   * <p>Default is unlimited on a plain accessor.
   *
   * @since 5.0
   */
  void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

  /**
   * Return the limit for array and collection auto-growing.
   *
   * @since 5.0
   */
  int getAutoGrowCollectionLimit();

  /**
   * Set the maximum number of intermediate properties traversed in a path;
   * {@code address.country.name} has depth 2. A value of {@code 0} disallows
   * nested paths while permitting simple, indexed, and mapped access.
   * Default is {@link #DEFAULT_MAX_NESTED_PATH_DEPTH}.
   * @param maxNestedPathDepth the non-negative limit
   * @since 5.0
   */
  void setMaxNestedPathDepth(int maxNestedPathDepth);

  /**
   * Return the maximum allowed depth of a nested property path.
   * @since 5.0
   */
  int getMaxNestedPathDepth();

}
