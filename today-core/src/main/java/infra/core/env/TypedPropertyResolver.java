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

package infra.core.env;

import org.jspecify.annotations.Nullable;

/**
 * @author TODAY 2021/10/3 15:33
 * @since 4.0
 */
public abstract class TypedPropertyResolver extends AbstractPropertyResolver {

  @Override
  @Nullable
  public String getProperty(String key) {
    return getProperty(key, String.class, true);
  }

  @Override
  @Nullable
  public <T> T getProperty(String key, Class<T> targetValueType) {
    return getProperty(key, targetValueType, true);
  }

  @Override
  @Nullable
  protected String getPropertyAsRawString(String key) {
    return getProperty(key, String.class, false);
  }

  @Nullable
  public abstract <T> T getProperty(
          String key, Class<T> targetValueType, boolean resolveNestedPlaceholders);

}
