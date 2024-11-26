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

package infra.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link CharArrayPropertyEditor} class.
 *
 * @author Rick Evans
 */
public class CharArrayPropertyEditorTests {

  private final PropertyEditor charEditor = new CharArrayPropertyEditor();

  @Test
  public void sunnyDaySetAsText() throws Exception {
    final String text = "Hideous towns make me throw... up";
    charEditor.setAsText(text);

    Object value = charEditor.getValue();
    assertThat(value).isNotNull().isInstanceOf(char[].class);
    char[] chars = (char[]) value;
    for (int i = 0; i < text.length(); ++i) {
      assertThat(chars[i]).as("char[] differs at index '" + i + "'").isEqualTo(text.charAt(i));
    }
    assertThat(charEditor.getAsText()).isEqualTo(text);
  }

  @Test
  public void getAsTextReturnsEmptyStringIfValueIsNull() throws Exception {
    assertThat(charEditor.getAsText()).isEqualTo("");

    charEditor.setAsText(null);
    assertThat(charEditor.getAsText()).isEqualTo("");
  }

}
