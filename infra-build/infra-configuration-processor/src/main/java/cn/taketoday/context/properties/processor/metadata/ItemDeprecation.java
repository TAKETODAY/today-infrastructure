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

package cn.taketoday.context.properties.processor.metadata;

/**
 * Describe an item deprecation.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ItemDeprecation {

  private String reason;

  private String replacement;

  private String since;

  private String level;

  public ItemDeprecation() {
    this(null, null, null);
  }

  public ItemDeprecation(String reason, String replacement, String since) {
    this(reason, replacement, since, null);
  }

  public ItemDeprecation(String reason, String replacement, String since, String level) {
    this.reason = reason;
    this.replacement = replacement;
    this.since = since;
    this.level = level;
  }

  public String getReason() {
    return this.reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getReplacement() {
    return this.replacement;
  }

  public void setReplacement(String replacement) {
    this.replacement = replacement;
  }

  public String getSince() {
    return this.since;
  }

  public void setSince(String since) {
    this.since = since;
  }

  public String getLevel() {
    return this.level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItemDeprecation other = (ItemDeprecation) o;
    return nullSafeEquals(this.reason, other.reason) && nullSafeEquals(this.replacement, other.replacement)
            && nullSafeEquals(this.level, other.level) && nullSafeEquals(this.since, other.since);
  }

  @Override
  public int hashCode() {
    int result = nullSafeHashCode(this.reason);
    result = 31 * result + nullSafeHashCode(this.replacement);
    result = 31 * result + nullSafeHashCode(this.level);
    result = 31 * result + nullSafeHashCode(this.since);
    return result;
  }

  @Override
  public String toString() {
    return "ItemDeprecation{reason='" + this.reason + '\'' + ", replacement='" + this.replacement + '\''
            + ", level='" + this.level + '\'' + ", since='" + this.since + '\'' + '}';
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

}
