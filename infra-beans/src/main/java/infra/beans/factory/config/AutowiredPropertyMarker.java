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

package infra.beans.factory.config;

import java.io.Serial;
import java.io.Serializable;

import infra.beans.factory.support.BeanDefinitionBuilder;

/**
 * Simple marker class for an individually autowired property value, to be added
 * to {@link BeanDefinition#getPropertyValues()} for a specific bean property.
 *
 * <p>At runtime, this will be replaced with a {@link DependencyDescriptor}
 * for the corresponding bean property's write method, eventually to be resolved
 * through a {@link AutowireCapableBeanFactory#resolveDependency} step.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AutowireCapableBeanFactory#resolveDependency
 * @see BeanDefinition#getPropertyValues()
 * @see BeanDefinitionBuilder#addAutowiredProperty
 * @since 4.0 2022/3/6 21:15
 */
public final class AutowiredPropertyMarker implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The canonical instance for the autowired marker value.
   */
  public static final Object INSTANCE = new AutowiredPropertyMarker();

  private AutowiredPropertyMarker() {
  }

  @Serial
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public int hashCode() {
    return AutowiredPropertyMarker.class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return INSTANCE == obj;
  }

  @Override
  public String toString() {
    return "(autowired)";
  }

}
