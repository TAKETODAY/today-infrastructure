/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.util.CollectionUtils;
import infra.util.ObjectUtils;

/**
 * Container for {@link BeanDefinitionCustomizer} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/24 17:37
 */
public class BeanDefinitionCustomizers {

  protected @Nullable List<BeanDefinitionCustomizer> customizers;

  /**
   * Sets the customizers to be applied to bean definitions.
   * <p>If customizers are provided, they are added to the existing list.
   * If {@code null} or empty, the existing list of customizers is cleared.
   *
   * @param customizers the customizers to add, or {@code null} to clear existing ones
   */
  public void setCustomizers(BeanDefinitionCustomizer @Nullable ... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
    else {
      // clear
      if (this.customizers != null) {
        this.customizers.clear();
      }
    }
  }

  /**
   * Sets the list of customizers to be applied to bean definitions.
   * <p>This method replaces any existing customizers with the provided list.
   * If {@code null} is provided, the customizers field will be set to {@code null}.
   *
   * @param customizers the list of customizers to set, or {@code null} to clear
   */
  public void setCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    this.customizers = customizers;
  }

  /**
   * Customizes the given bean definition by applying all registered customizers.
   * <p>If no customizers are registered, this method does nothing.
   *
   * @param definition the bean definition to customize
   */
  protected final void customize(BeanDefinition definition) {
    if (CollectionUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer definitionCustomizer : customizers) {
        definitionCustomizer.customize(definition);
      }
    }
  }

  private List<BeanDefinitionCustomizer> customizers() {
    if (customizers == null) {
      customizers = new ArrayList<>();
    }
    return customizers;
  }

}
