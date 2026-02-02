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

package infra.jdbc.type;

import infra.beans.BeanProperty;

/**
 * A smart type handler that can determine if it supports a given property.
 *
 * @param <T> the type of the property
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/23 14:54
 */
public interface SmartTypeHandler<T> extends TypeHandler<T> {

  /**
   * Tests whether this handler can handle the given property.
   *
   * @param property The bean property to test
   * @return {@code true} if this handler supports the property, otherwise {@code false}
   */
  boolean supportsProperty(BeanProperty property);

}
