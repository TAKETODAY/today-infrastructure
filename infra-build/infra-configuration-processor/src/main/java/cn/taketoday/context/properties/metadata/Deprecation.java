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
 * Indicate that a property is deprecated. Provide additional information about the
 * deprecation.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class Deprecation implements Serializable {

  private Level level = Level.WARNING;

  private String reason;

  private String shortReason;

  private String replacement;

  /**
   * Define the {@link Level} of deprecation.
   *
   * @return the deprecation level
   */
  public Level getLevel() {
    return this.level;
  }

  public void setLevel(Level level) {
    this.level = level;
  }

  /**
   * A reason why the related property is deprecated, if any. Can be multi-lines.
   *
   * @return the deprecation reason
   * @see #getShortReason()
   */
  public String getReason() {
    return this.reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  /**
   * A single-line, single-sentence reason why the related property is deprecated, if
   * any.
   *
   * @return the short deprecation reason
   * @see #getReason()
   */
  public String getShortReason() {
    return this.shortReason;
  }

  public void setShortReason(String shortReason) {
    this.shortReason = shortReason;
  }

  /**
   * The full name of the property that replaces the related deprecated property, if
   * any.
   *
   * @return the replacement property name
   */
  public String getReplacement() {
    return this.replacement;
  }

  public void setReplacement(String replacement) {
    this.replacement = replacement;
  }

  @Override
  public String toString() {
    return "Deprecation{level='" + this.level + '\'' + ", reason='" + this.reason + '\'' + ", replacement='"
            + this.replacement + '\'' + '}';
  }

  /**
   * Define the deprecation level.
   */
  public enum Level {

    /**
     * The property is still bound.
     */
    WARNING,

    /**
     * The property has been removed and is no longer bound.
     */
    ERROR

  }

}
