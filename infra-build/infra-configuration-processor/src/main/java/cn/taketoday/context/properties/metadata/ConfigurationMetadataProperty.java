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
 * Define a configuration property. Each property is fully identified by its
 * {@link #getId() id} which is composed of a namespace prefix (the
 * {@link ConfigurationMetadataGroup#getId() group id}), if any and the {@link #getName()
 * name} of the property.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ConfigurationMetadataProperty implements Serializable {

  private String id;

  private String name;

  private String type;

  private String description;

  private String shortDescription;

  private Object defaultValue;

  private final Hints hints = new Hints();

  private Deprecation deprecation;

  /**
   * The full identifier of the property, in lowercase dashed form (e.g.
   * my.group.simple-property)
   *
   * @return the property id
   */
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * The name of the property, in lowercase dashed form (e.g. simple-property). If this
   * item does not belong to any group, the id is returned.
   *
   * @return the property name
   */
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The class name of the data type of the property. For example,
   * {@code java.lang.String}.
   * <p>
   * For consistency, the type of a primitive is specified using its wrapper
   * counterpart, i.e. {@code boolean} becomes {@code java.lang.Boolean}. If the type
   * holds generic information, these are provided as well, i.e. a {@code HashMap} of
   * String to Integer would be defined as {@code java.util.HashMap
   * <java.lang.String,java.lang.Integer>}.
   * <p>
   * Note that this class may be a complex type that gets converted from a String as
   * values are bound.
   *
   * @return the property type
   */
  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }

  /**
   * A description of the property, if any. Can be multi-lines.
   *
   * @return the property description
   * @see #getShortDescription()
   */
  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * A single-line, single-sentence description of this property, if any.
   *
   * @return the property short description
   * @see #getDescription()
   */
  public String getShortDescription() {
    return this.shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  /**
   * The default value, if any.
   *
   * @return the default value
   */
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Return the hints of this item.
   *
   * @return the hints
   */
  public Hints getHints() {
    return this.hints;
  }

  /**
   * The {@link Deprecation} for this property, if any.
   *
   * @return the deprecation
   * @see #isDeprecated()
   */
  public Deprecation getDeprecation() {
    return this.deprecation;
  }

  public void setDeprecation(Deprecation deprecation) {
    this.deprecation = deprecation;
  }

  /**
   * Specify if the property is deprecated.
   *
   * @return if the property is deprecated
   * @see #getDeprecation()
   */
  public boolean isDeprecated() {
    return this.deprecation != null;
  }

}
