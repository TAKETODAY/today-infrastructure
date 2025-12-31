/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.multipart.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 12:30
 */
class StringPartEditorTests {

  @Test
  void setAsTextSetsStringValue() {
    infra.web.multipart.support.StringPartEditor editor = new StringPartEditor();
    String text = "test value";

    editor.setAsText(text);

    assertThat(editor.getAsText()).isEqualTo(text);
  }

  @Test
  void setValueWithMultipartFileConvertsToByteArray() throws IOException {
    StringPartEditor editor = new StringPartEditor();
    Part part = mock(Part.class);
    String content = "file content";
    when(part.getContentAsByteArray()).thenReturn(content.getBytes());
    when(part.getContentAsString(StandardCharsets.UTF_8)).thenReturn(content);
    when(part.getContentAsString()).thenReturn(content);

    editor.setValue(part);

    assertThat(editor.getValue()).isEqualTo(content);
  }

  @Test
  void setValueWithMultipartFileAndCharsetConvertsUsingSpecifiedCharset() throws IOException {
    StringPartEditor editor = new StringPartEditor(StandardCharsets.UTF_8);
    Part part = mock(Part.class);
    String content = "file content";
    when(part.getContentAsString()).thenReturn(content);
    when(part.getContentAsString(StandardCharsets.UTF_8)).thenReturn(content);

    editor.setValue(part);

    assertThat(editor.getValue()).isEqualTo(content);
  }

  @Test
  void setValueWithMultipartFileThrowsIllegalArgumentExceptionOnIoException() throws IOException {
    StringPartEditor editor = new StringPartEditor();
    Part multipartFile = mock(Part.class);
    when(multipartFile.getContentAsString()).thenThrow(new IOException("test exception"));
    when(multipartFile.getContentAsString(StandardCharsets.UTF_8)).thenThrow(new IOException("test exception"));

    assertThatThrownBy(() -> editor.setValue(multipartFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot read contents of multipart file")
            .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void setValueWithNonMultipartFileSetsValueDirectly() {
    StringPartEditor editor = new StringPartEditor();
    String value = "direct value";

    editor.setValue(value);

    assertThat(editor.getValue()).isEqualTo(value);
  }

}