/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;

/**
 * Editor for {@code java.util.regex.Pattern}, to directly populate a Pattern property.
 * Expects the same syntax as Pattern's {@code compile} method.
 *
 * @author Juergen Hoeller
 * @see Pattern
 * @see Pattern#compile(String)
 * @since 2.0.1
 */
public class PatternEditor extends PropertyEditorSupport {

  private final int flags;

  /**
   * Create a new PatternEditor with default settings.
   */
  public PatternEditor() {
    this.flags = 0;
  }

  /**
   * Create a new PatternEditor with the given settings.
   *
   * @param flags the {@code java.util.regex.Pattern} flags to apply
   * @see Pattern#compile(String, int)
   * @see Pattern#CASE_INSENSITIVE
   * @see Pattern#MULTILINE
   * @see Pattern#DOTALL
   * @see Pattern#UNICODE_CASE
   * @see Pattern#CANON_EQ
   */
  public PatternEditor(int flags) {
    this.flags = flags;
  }

  @Override
  public void setAsText(@Nullable String text) {
    setValue(text != null ? Pattern.compile(text, this.flags) : null);
  }

  @Override
  public String getAsText() {
    Pattern value = (Pattern) getValue();
    return (value != null ? value.pattern() : "");
  }

}
