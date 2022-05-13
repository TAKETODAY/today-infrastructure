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

package cn.taketoday.web.multipart;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.web.multipart.support.ByteArrayMultipartFileEditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:36
 */
class ByteArrayMultipartFileEditorTests {

  private final ByteArrayMultipartFileEditor editor = new ByteArrayMultipartFileEditor();

  @Test
  public void setValueAsByteArray() throws Exception {
    String expectedValue = "Shumwere, shumhow, a shuck ish washing you. - Drunken Far Side";
    editor.setValue(expectedValue.getBytes());
    assertThat(editor.getAsText()).isEqualTo(expectedValue);
  }

  @Test
  public void setValueAsString() throws Exception {
    String expectedValue = "'Green Wing' - classic British comedy";
    editor.setValue(expectedValue);
    assertThat(editor.getAsText()).isEqualTo(expectedValue);
  }

  @Test
  public void setValueAsCustomObjectInvokesToString() throws Exception {
    final String expectedValue = "'Green Wing' - classic British comedy";
    Object object = new Object() {
      @Override
      public String toString() {
        return expectedValue;
      }
    };

    editor.setValue(object);
    assertThat(editor.getAsText()).isEqualTo(expectedValue);
  }

  @Test
  public void setValueAsNullGetsBackEmptyString() throws Exception {
    editor.setValue(null);
    assertThat(editor.getAsText()).isEqualTo("");
  }

  @Test
  public void setValueAsMultipartFile() throws Exception {
    String expectedValue = "That is comforting to know";
    MultipartFile file = mock(MultipartFile.class);
    given(file.getBytes()).willReturn(expectedValue.getBytes());
    editor.setValue(file);
    assertThat(editor.getAsText()).isEqualTo(expectedValue);
  }

  @Test
  public void setValueAsMultipartFileWithBadBytes() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    given(file.getBytes()).willThrow(new IOException());
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setValue(file));
  }

}