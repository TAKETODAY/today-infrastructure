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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.EmbeddedValueResolver;

/**
 * Represents the information about a named value,
 * including name, whether it's required and a default value.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.web.annotation.RequestParam
 * @since 4.0 2022/1/19 21:50
 */
public final class NamedValueInfo {

  public final String name;

  // default is true (required)
  public final boolean required;

  @Nullable
  public final String defaultValue;

  public final boolean nameEmbedded;

  public final boolean defaultValueEmbedded;

  public NamedValueInfo(String name) {
    this(name, true, null);
  }

  public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
    this.name = name;
    this.required = required;
    this.defaultValue = defaultValue;
    this.nameEmbedded = EmbeddedValueResolver.isEmbedded(name);
    this.defaultValueEmbedded = EmbeddedValueResolver.isEmbedded(defaultValue);
  }

  public NamedValueInfo(NamedValueInfo info, @Nullable String defaultValue) {
    this.name = info.name;
    this.required = info.required;
    this.defaultValue = defaultValue;
    this.nameEmbedded = info.nameEmbedded;
    this.defaultValueEmbedded = EmbeddedValueResolver.isEmbedded(defaultValue);
  }

}
