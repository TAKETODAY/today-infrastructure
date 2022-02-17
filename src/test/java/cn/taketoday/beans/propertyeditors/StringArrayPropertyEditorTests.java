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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class StringArrayPropertyEditorTests {

  @Test
  void withDefaultSeparator() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
    editor.setAsText("0,1,2");
    Object value = editor.getValue();
    assertTrimmedElements(value);
    assertThat(editor.getAsText()).isEqualTo("0,1,2");
  }

  @Test
  void trimByDefault() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
    editor.setAsText(" 0,1 , 2 ");
    Object value = editor.getValue();
    assertTrimmedElements(value);
    assertThat(editor.getAsText()).isEqualTo("0,1,2");
  }

  @Test
  void noTrim() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", false, false);
    editor.setAsText("  0,1  , 2 ");
    Object value = editor.getValue();
    String[] array = (String[]) value;
    for (int i = 0; i < array.length; ++i) {
      assertThat(array[i].length()).isEqualTo(3);
      assertThat(array[i].trim()).isEqualTo(("" + i));
    }
    assertThat(editor.getAsText()).isEqualTo("  0,1  , 2 ");
  }

  @Test
  void withCustomSeparator() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor(":");
    editor.setAsText("0:1:2");
    Object value = editor.getValue();
    assertTrimmedElements(value);
    assertThat(editor.getAsText()).isEqualTo("0:1:2");
  }

  @Test
  void withCharsToDelete() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", "\r\n", false);
    editor.setAsText("0\r,1,\n2");
    Object value = editor.getValue();
    assertTrimmedElements(value);
    assertThat(editor.getAsText()).isEqualTo("0,1,2");
  }

  @Test
  void withEmptyArray() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
    editor.setAsText("");
    Object value = editor.getValue();
    assertThat(value).isInstanceOf(String[].class);
    assertThat((String[]) value).isEmpty();
  }

  @Test
  void withEmptyArrayAsNull() {
    StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", true);
    editor.setAsText("");
    assertThat(editor.getValue()).isNull();
  }

  private static void assertTrimmedElements(Object value) {
    assertThat(value).isInstanceOf(String[].class);
    String[] array = (String[]) value;
    for (int i = 0; i < array.length; ++i) {
      assertThat(array[i]).isEqualTo(("" + i));
    }
  }

}
