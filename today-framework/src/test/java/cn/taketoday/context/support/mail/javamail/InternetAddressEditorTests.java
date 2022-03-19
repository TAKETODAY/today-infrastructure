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

package cn.taketoday.context.support.mail.javamail;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.mail.javamail.InternetAddressEditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Brian Hanafee
 * @author Sam Brannen
 */
public class InternetAddressEditorTests {

  private static final String EMPTY = "";
  private static final String SIMPLE = "nobody@nowhere.com";
  private static final String BAD = "(";

  private final InternetAddressEditor editor = new InternetAddressEditor();

  @Test
  public void uninitialized() {
    assertThat(editor.getAsText()).as("Uninitialized editor did not return empty value string").isEqualTo(EMPTY);
  }

  @Test
  public void setNull() {
    editor.setAsText(null);
    assertThat(editor.getAsText()).as("Setting null did not result in empty value string").isEqualTo(EMPTY);
  }

  @Test
  public void setEmpty() {
    editor.setAsText(EMPTY);
    assertThat(editor.getAsText()).as("Setting empty string did not result in empty value string").isEqualTo(EMPTY);
  }

  @Test
  public void allWhitespace() {
    editor.setAsText(" ");
    assertThat(editor.getAsText()).as("All whitespace was not recognized").isEqualTo(EMPTY);
  }

  @Test
  public void simpleGoodAddress() {
    editor.setAsText(SIMPLE);
    assertThat(editor.getAsText()).as("Simple email address failed").isEqualTo(SIMPLE);
  }

  @Test
  public void excessWhitespace() {
    editor.setAsText(" " + SIMPLE + " ");
    assertThat(editor.getAsText()).as("Whitespace was not stripped").isEqualTo(SIMPLE);
  }

  @Test
  public void simpleBadAddress() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText(BAD));
  }

}
