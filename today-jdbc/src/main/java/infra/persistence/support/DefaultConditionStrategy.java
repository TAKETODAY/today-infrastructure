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

package infra.persistence.support;

import org.jspecify.annotations.Nullable;

import infra.persistence.EntityProperty;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.sql.Restriction;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:43
 */
public class DefaultConditionStrategy implements PropertyConditionStrategy {

  @Nullable
  @Override
  public Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object value) {
    if (value instanceof String string && StringUtils.isBlank(string)) {
      return null;
    }
    return new Condition(value, Restriction.equal(entityProperty.columnName), entityProperty, logicalAnd);
  }

}
