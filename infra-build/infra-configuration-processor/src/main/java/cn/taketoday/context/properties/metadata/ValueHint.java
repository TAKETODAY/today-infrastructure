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

/**
 * Hint for a value a given property may have. Provide the value and an optional
 * description.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ValueHint implements Serializable {

  private Object value;

  private String description;

  private String shortDescription;

  /**
   * Return the hint value.
   *
   * @return the value
   */
  public Object getValue() {
    return this.value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * A description of this value, if any. Can be multi-lines.
   *
   * @return the description
   * @see #getShortDescription()
   */
  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * A single-line, single-sentence description of this hint, if any.
   *
   * @return the short description
   * @see #getDescription()
   */
  public String getShortDescription() {
    return this.shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  @Override
  public String toString() {
    return "ValueHint{value=" + this.value + ", description='" + this.description + '\'' + '}';
  }

}
