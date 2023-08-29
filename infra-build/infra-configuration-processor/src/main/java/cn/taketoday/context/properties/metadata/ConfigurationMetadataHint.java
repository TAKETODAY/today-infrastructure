/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * A raw view of a hint used for parsing only.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationMetadataHint {

  private static final String KEY_SUFFIX = ".keys";

  private static final String VALUE_SUFFIX = ".values";

  private String id;

  private final List<ValueHint> valueHints = new ArrayList<>();

  private final List<ValueProvider> valueProviders = new ArrayList<>();

  boolean isMapKeyHints() {
    return (this.id != null && this.id.endsWith(KEY_SUFFIX));
  }

  boolean isMapValueHints() {
    return (this.id != null && this.id.endsWith(VALUE_SUFFIX));
  }

  String resolveId() {
    if (isMapKeyHints()) {
      return this.id.substring(0, this.id.length() - KEY_SUFFIX.length());
    }
    if (isMapValueHints()) {
      return this.id.substring(0, this.id.length() - VALUE_SUFFIX.length());
    }
    return this.id;
  }

  String getId() {
    return this.id;
  }

  void setId(String id) {
    this.id = id;
  }

  List<ValueHint> getValueHints() {
    return this.valueHints;
  }

  List<ValueProvider> getValueProviders() {
    return this.valueProviders;
  }

}
