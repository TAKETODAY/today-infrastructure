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

import java.io.Reader;

import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link ReaderEditor} class.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ReaderEditorTests {

  @Test
  public void testCtorWithNullResourceEditor() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReaderEditor(null));
  }

  @Test
  public void testSunnyDay() throws Exception {
    Reader reader = null;
    try {
      String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
              "/" + ClassUtils.getShortName(getClass()) + ".class";
      ReaderEditor editor = new ReaderEditor();
      editor.setAsText(resource);
      Object value = editor.getValue();
      assertThat(value).isNotNull();
      boolean condition = value instanceof Reader;
      assertThat(condition).isTrue();
      reader = (Reader) value;
      assertThat(reader.ready()).isTrue();
    }
    finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  @Test
  public void testWhenResourceDoesNotExist() throws Exception {
    String resource = "classpath:bingo!";
    ReaderEditor editor = new ReaderEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText(resource));
  }

  @Test
  public void testGetAsTextReturnsNullByDefault() throws Exception {
    assertThat(new ReaderEditor().getAsText()).isNull();
    String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
            "/" + ClassUtils.getShortName(getClass()) + ".class";
    ReaderEditor editor = new ReaderEditor();
    editor.setAsText(resource);
    assertThat(editor.getAsText()).isNull();
  }

}
