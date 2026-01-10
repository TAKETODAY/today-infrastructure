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

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link PropertyEditorRegistry property editor registry}.
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several different situations: write a corresponding
 * registrar and reuse that in each case.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 * @since 4.0 2022/2/17 17:41
 */
public interface PropertyEditorRegistrar {

  /**
   * Register custom {@link java.beans.PropertyEditor PropertyEditors} with
   * the given {@code PropertyEditorRegistry}.
   * <p>The passed-in registry will usually be a {@link BeanWrapper} or a
   * {@link infra.validation.DataBinder DataBinder}.
   * <p>It is expected that implementations will create brand new
   * {@code PropertyEditors} instances for each invocation of this
   * method (since {@code PropertyEditors} are not threadsafe).
   *
   * @param registry the {@code PropertyEditorRegistry} to register the
   * custom {@code PropertyEditors} with
   */
  void registerCustomEditors(PropertyEditorRegistry registry);

  /**
   * Indicate whether this registrar exclusively overrides default editors
   * rather than registering custom editors, intended to be applied lazily.
   * <p>This has an impact on registrar handling in a bean factory: see
   * {@link infra.beans.factory.config.ConfigurableBeanFactory#addPropertyEditorRegistrar}.
   *
   * @see PropertyEditorRegistry#registerCustomEditor
   * @see PropertyEditorRegistrySupport#overrideDefaultEditor
   * @see PropertyEditorRegistrySupport#setDefaultEditorRegistrar
   * @since 5.0
   */
  default boolean overridesDefaultEditors() {
    return false;
  }

}
