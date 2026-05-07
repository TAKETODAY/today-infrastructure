/*
 * Copyright 2012-present the original author or authors.
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

package infra.testcontainers.beans;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.BeanDefinition;
import infra.core.annotation.MergedAnnotations;

/**
 * Extended {@link infra.beans.factory.config.BeanDefinition} interface used
 * to register testcontainer beans.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public interface TestcontainerBeanDefinition extends BeanDefinition {

  /**
   * Return the container image name or {@code null} if the image name is not yet known.
   *
   * @return the container image name
   */
  @Nullable String getContainerImageName();

  /**
   * Return any annotations declared alongside the container.
   *
   * @return annotations declared with the container
   */
  MergedAnnotations getAnnotations();

}
