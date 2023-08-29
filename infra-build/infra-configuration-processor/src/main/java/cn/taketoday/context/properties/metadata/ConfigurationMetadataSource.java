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
import java.util.HashMap;
import java.util.Map;

/**
 * A source of configuration metadata. Also defines where the source is declared, for
 * instance if it is defined as a {@code @Bean}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ConfigurationMetadataSource implements Serializable {

  private String groupId;

  private String type;

  private String description;

  private String shortDescription;

  private String sourceType;

  private String sourceMethod;

  private final Map<String, ConfigurationMetadataProperty> properties = new HashMap<>();

  /**
   * The identifier of the group to which this source is associated.
   *
   * @return the group id
   */
  public String getGroupId() {
    return this.groupId;
  }

  void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  /**
   * The type of the source. Usually this is the fully qualified name of a class that
   * defines configuration items. This class may or may not be available at runtime.
   *
   * @return the type
   */
  public String getType() {
    return this.type;
  }

  void setType(String type) {
    this.type = type;
  }

  /**
   * A description of this source, if any. Can be multi-lines.
   *
   * @return the description
   * @see #getShortDescription()
   */
  public String getDescription() {
    return this.description;
  }

  void setDescription(String description) {
    this.description = description;
  }

  /**
   * A single-line, single-sentence description of this source, if any.
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

  /**
   * The type where this source is defined. This can be identical to the
   * {@link #getType() type} if the source is self-defined.
   *
   * @return the source type
   */
  public String getSourceType() {
    return this.sourceType;
  }

  void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  /**
   * The method name that defines this source, if any.
   *
   * @return the source method
   */
  public String getSourceMethod() {
    return this.sourceMethod;
  }

  void setSourceMethod(String sourceMethod) {
    this.sourceMethod = sourceMethod;
  }

  /**
   * Return the properties defined by this source.
   *
   * @return the properties
   */
  public Map<String, ConfigurationMetadataProperty> getProperties() {
    return this.properties;
  }

}
