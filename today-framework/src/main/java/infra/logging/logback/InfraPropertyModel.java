/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.logging.logback;

import ch.qos.logback.core.model.NamedModel;

/**
 * Logback {@link NamedModel model} to support {@code <infra-property>} tags. Allows
 * Logback properties to be sourced from the Infra environment.
 *
 * @author Andy Wilkinson
 * @see InfraPropertyAction
 * @see InfraPropertyModelHandler
 */
class InfraPropertyModel extends NamedModel {

  private String scope;

  private String defaultValue;

  private String source;

  String getScope() {
    return this.scope;
  }

  void setScope(String scope) {
    this.scope = scope;
  }

  String getDefaultValue() {
    return this.defaultValue;
  }

  void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  String getSource() {
    return this.source;
  }

  void setSource(String source) {
    this.source = source;
  }

}
