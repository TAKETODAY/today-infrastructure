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

package cn.taketoday.context.properties.processor.metadata;

import java.util.Locale;

/**
 * A group or property meta-data item from some {@link ConfigurationMetadata}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationMetadata
 * @since 4.0
 */
public final class ItemMetadata implements Comparable<ItemMetadata> {

  private final ItemType itemType;

  private String name;

  private String type;

  private String description;

  private String sourceType;

  private String sourceMethod;

  private Object defaultValue;

  private ItemDeprecation deprecation;

  ItemMetadata(ItemType itemType, String prefix, String name, String type, String sourceType, String sourceMethod,
          String description, Object defaultValue, ItemDeprecation deprecation) {
    this.itemType = itemType;
    this.name = buildName(prefix, name);
    this.type = type;
    this.sourceType = sourceType;
    this.sourceMethod = sourceMethod;
    this.description = description;
    this.defaultValue = defaultValue;
    this.deprecation = deprecation;
  }

  private String buildName(String prefix, String name) {
    StringBuilder fullName = new StringBuilder();
    if (prefix != null) {
      if (prefix.endsWith(".")) {
        prefix = prefix.substring(0, prefix.length() - 1);
      }
      fullName.append(prefix);
    }
    if (name != null) {
      if (!fullName.isEmpty()) {
        fullName.append('.');
      }
      fullName.append(ConfigurationMetadata.toDashedCase(name));
    }
    return fullName.toString();
  }

  public boolean isOfItemType(ItemType itemType) {
    return this.itemType == itemType;
  }

  public boolean hasSameType(ItemMetadata metadata) {
    return this.itemType == metadata.itemType;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSourceType() {
    return this.sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getSourceMethod() {
    return this.sourceMethod;
  }

  public void setSourceMethod(String sourceMethod) {
    this.sourceMethod = sourceMethod;
  }

  public Object getDefaultValue() {
    return this.defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ItemDeprecation getDeprecation() {
    return this.deprecation;
  }

  public void setDeprecation(ItemDeprecation deprecation) {
    this.deprecation = deprecation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItemMetadata other = (ItemMetadata) o;
    boolean result = true;
    result = result && nullSafeEquals(this.itemType, other.itemType);
    result = result && nullSafeEquals(this.name, other.name);
    result = result && nullSafeEquals(this.type, other.type);
    result = result && nullSafeEquals(this.description, other.description);
    result = result && nullSafeEquals(this.sourceType, other.sourceType);
    result = result && nullSafeEquals(this.sourceMethod, other.sourceMethod);
    result = result && nullSafeEquals(this.defaultValue, other.defaultValue);
    result = result && nullSafeEquals(this.deprecation, other.deprecation);
    return result;
  }

  @Override
  public int hashCode() {
    int result = nullSafeHashCode(this.itemType);
    result = 31 * result + nullSafeHashCode(this.name);
    result = 31 * result + nullSafeHashCode(this.type);
    result = 31 * result + nullSafeHashCode(this.description);
    result = 31 * result + nullSafeHashCode(this.sourceType);
    result = 31 * result + nullSafeHashCode(this.sourceMethod);
    result = 31 * result + nullSafeHashCode(this.defaultValue);
    result = 31 * result + nullSafeHashCode(this.deprecation);
    return result;
  }

  private boolean nullSafeEquals(Object o1, Object o2) {
    if (o1 == o2) {
      return true;
    }
    if (o1 == null || o2 == null) {
      return false;
    }
    return o1.equals(o2);
  }

  private int nullSafeHashCode(Object o) {
    return (o != null) ? o.hashCode() : 0;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder(this.name);
    buildToStringProperty(string, "type", this.type);
    buildToStringProperty(string, "sourceType", this.sourceType);
    buildToStringProperty(string, "description", this.description);
    buildToStringProperty(string, "defaultValue", this.defaultValue);
    buildToStringProperty(string, "deprecation", this.deprecation);
    return string.toString();
  }

  protected void buildToStringProperty(StringBuilder string, String property, Object value) {
    if (value != null) {
      string.append(" ").append(property).append(":").append(value);
    }
  }

  @Override
  public int compareTo(ItemMetadata o) {
    return getName().compareTo(o.getName());
  }

  public static ItemMetadata newGroup(String name, String type, String sourceType, String sourceMethod) {
    return new ItemMetadata(ItemType.GROUP, name, null, type, sourceType, sourceMethod, null, null, null);
  }

  public static ItemMetadata newProperty(String prefix, String name, String type, String sourceType,
          String sourceMethod, String description, Object defaultValue, ItemDeprecation deprecation) {
    return new ItemMetadata(ItemType.PROPERTY, prefix, name, type, sourceType, sourceMethod, description,
            defaultValue, deprecation);
  }

  public static String newItemMetadataPrefix(String prefix, String suffix) {
    return prefix.toLowerCase(Locale.ROOT) + ConfigurationMetadata.toDashedCase(suffix);
  }

  /**
   * The item type.
   */
  public enum ItemType {

    /**
     * Group item type.
     */
    GROUP,

    /**
     * Property item type.
     */
    PROPERTY

  }

}
