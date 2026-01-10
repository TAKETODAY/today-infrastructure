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

package infra.jdbc;

import java.sql.ResultSet;

import infra.beans.BeanProperty;

/**
 * use this handler when {@link ObjectPropertySetter}
 * handle {@link ObjectPropertySetter#setTo(Object, ResultSet, int)}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 09:24
 */
public interface PrimitiveTypeNullHandler {

  /**
   * handle null when {@link ObjectPropertySetter} {@code property} is {@code null}
   * and {@code property} is primitive-type
   */
  void handleNull(BeanProperty property, Object obj);
}
