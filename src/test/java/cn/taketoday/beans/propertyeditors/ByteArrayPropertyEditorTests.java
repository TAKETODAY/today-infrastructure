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

import java.beans.PropertyEditor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link ByteArrayPropertyEditor} class.
 *
 * @author Rick Evans
 */
public class ByteArrayPropertyEditorTests {

  private final PropertyEditor byteEditor = new ByteArrayPropertyEditor();

  @Test
  public void sunnyDaySetAsText() throws Exception {
    final String text = "Hideous towns make me throw... up";
    byteEditor.setAsText(text);

    Object value = byteEditor.getValue();
    assertThat(value).isNotNull().isInstanceOf(byte[].class);
    byte[] bytes = (byte[]) value;
    for (int i = 0; i < text.length(); ++i) {
      assertThat(bytes[i]).as("cyte[] differs at index '" + i + "'").isEqualTo((byte) text.charAt(i));
    }
    assertThat(byteEditor.getAsText()).isEqualTo(text);
  }

  @Test
  public void getAsTextReturnsEmptyStringIfValueIsNull() throws Exception {
    assertThat(byteEditor.getAsText()).isEqualTo("");

    byteEditor.setAsText(null);
    assertThat(byteEditor.getAsText()).isEqualTo("");
  }

}
