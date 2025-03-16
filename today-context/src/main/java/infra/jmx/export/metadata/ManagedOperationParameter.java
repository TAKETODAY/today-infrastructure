/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jmx.export.metadata;

/**
 * Metadata about JMX operation parameters.
 * Used in conjunction with a {@link ManagedOperation} attribute.
 *
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ManagedOperationParameter {

  private int index = 0;

  private String name = "";

  private String description = "";

  /**
   * Set the index of this parameter in the operation signature.
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * Return the index of this parameter in the operation signature.
   */
  public int getIndex() {
    return this.index;
  }

  /**
   * Set the name of this parameter in the operation signature.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Return the name of this parameter in the operation signature.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set a description for this parameter.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Return a description for this parameter.
   */
  public String getDescription() {
    return this.description;
  }

}
