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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Define a component that is able to provide the values of a property.
 * <p>
 * Each provider is defined by a {@code name} and can have an arbitrary number of
 * {@code parameters}. The available providers are defined in the Infra App
 * documentation.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ValueProvider implements Serializable {

  private String name;

  private final Map<String, Object> parameters = new LinkedHashMap<>();

  /**
   * Return the name of the provider.
   *
   * @return the name
   */
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Return the parameters.
   *
   * @return the parameters
   */
  public Map<String, Object> getParameters() {
    return this.parameters;
  }

  @Override
  public String toString() {
    return "ValueProvider{name='" + this.name + ", parameters=" + this.parameters + '}';
  }

}
