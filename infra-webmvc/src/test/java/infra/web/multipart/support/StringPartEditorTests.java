/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    infra.web.multipart.support.StringPartEditor editor = new infra.web.multipart.support.StringPartEditor();
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