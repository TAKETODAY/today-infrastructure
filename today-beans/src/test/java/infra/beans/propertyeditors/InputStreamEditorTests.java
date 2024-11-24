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

import java.io.InputStream;

import infra.beans.propertyeditors.InputStreamEditor;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link InputStreamEditor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class InputStreamEditorTests {

  @Test
  public void testCtorWithNullResourceEditor() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new InputStreamEditor(null));
  }

  @Test
  public void testSunnyDay() throws Exception {
    InputStream stream = null;
    try {
      String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
              "/" + ClassUtils.getShortName(getClass()) + ".class";
      InputStreamEditor editor = new InputStreamEditor();
      editor.setAsText(resource);
      Object value = editor.getValue();
      assertThat(value).isNotNull();
      boolean condition = value instanceof InputStream;
      assertThat(condition).isTrue();
      stream = (InputStream) value;
      assertThat(stream.available() > 0).isTrue();
    }
    finally {
      if (stream != null) {
        stream.close();
      }
    }
  }

  @Test
  public void testWhenResourceDoesNotExist() throws Exception {
    InputStreamEditor editor = new InputStreamEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText("classpath:bingo!"));
  }

  @Test
  public void testGetAsTextReturnsNullByDefault() throws Exception {
    assertThat(new InputStreamEditor().getAsText()).isNull();
    String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
            "/" + ClassUtils.getShortName(getClass()) + ".class";
    InputStreamEditor editor = new InputStreamEditor();
    editor.setAsText(resource);
    assertThat(editor.getAsText()).isNull();
  }

}
