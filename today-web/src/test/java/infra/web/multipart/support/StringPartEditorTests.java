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

import infra.web.multipart.MultipartFile;

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
    StringPartEditor editor = new StringPartEditor();
    String text = "test value";

    editor.setAsText(text);

    assertThat(editor.getAsText()).isEqualTo(text);
  }

  @Test
  void setValueWithMultipartFileConvertsToByteArray() throws IOException {
    StringPartEditor editor = new StringPartEditor();
    MultipartFile multipartFile = mock(MultipartFile.class);
    String content = "file content";
    when(multipartFile.getContentAsByteArray()).thenReturn(content.getBytes());

    editor.setValue(multipartFile);

    assertThat(editor.getValue()).isEqualTo(content);
  }

  @Test
  void setValueWithMultipartFileAndCharsetConvertsUsingSpecifiedCharset() throws IOException {
    StringPartEditor editor = new StringPartEditor(StandardCharsets.UTF_8);
    MultipartFile multipartFile = mock(MultipartFile.class);
    String content = "file content";
    when(multipartFile.getContentAsByteArray()).thenReturn(content.getBytes());

    editor.setValue(multipartFile);

    assertThat(editor.getValue()).isEqualTo(content);
  }

  @Test
  void setValueWithMultipartFileThrowsIllegalArgumentExceptionOnIoException() throws IOException {
    StringPartEditor editor = new StringPartEditor();
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getContentAsByteArray()).thenThrow(new IOException("test exception"));

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