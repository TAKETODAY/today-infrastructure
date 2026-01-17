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

package infra.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/14 14:45
 */
class MediaTypeEditorTests {

  @Test
  void mediaTypeEditor() {
    MediaTypeEditor editor = new MediaTypeEditor();
    assertThat(editor.getAsText()).isEqualTo("");
    editor.setAsText("application/json");
    assertThat(editor.getValue()).isInstanceOf(MediaType.class).isEqualTo(MediaType.APPLICATION_JSON);

    editor.setAsText("");
    assertThat(editor.getValue()).isNull();
  }

  @Test
  void setAsText_withValidMediaTypeString_shouldSetValue() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setAsText("text/plain");

    Object value = editor.getValue();
    assertThat(value).isInstanceOf(MediaType.class);
    assertThat(value).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void setAsText_withComplexMediaTypeString_shouldSetValue() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setAsText("application/json;charset=UTF-8");

    Object value = editor.getValue();
    assertThat(value).isInstanceOf(MediaType.class);
    MediaType mediaType = (MediaType) value;
    assertThat(mediaType.getType()).isEqualTo("application");
    assertThat(mediaType.getSubtype()).isEqualTo("json");
  }

  @Test
  void setAsText_withWhitespace_shouldTrimAndSetValue() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setAsText("  application/xml  ");

    Object value = editor.getValue();
    assertThat(value).isInstanceOf(MediaType.class);
    assertThat(value).isEqualTo(MediaType.APPLICATION_XML);
  }

  @Test
  void setAsText_withNull_shouldSetValueToNull() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setAsText(null);

    assertThat(editor.getValue()).isNull();
  }

  @Test
  void setAsText_withEmptyString_shouldSetValueToNull() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setAsText("");

    assertThat(editor.getValue()).isNull();
  }

  @Test
  void setAsText_withWhitespaceOnlyString_shouldSetValueToNull() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setAsText("   ");

    assertThat(editor.getValue()).isNull();
  }

  @Test
  void getAsText_withNullValue_shouldReturnEmptyString() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setValue(null);

    assertThat(editor.getAsText()).isEqualTo("");
  }

  @Test
  void getAsText_withMediaTypeValue_shouldReturnStringRepresentation() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setValue(MediaType.APPLICATION_PDF);

    assertThat(editor.getAsText()).isEqualTo("application/pdf");
  }

  @Test
  void getAsText_withComplexMediaTypeValue_shouldReturnFullStringRepresentation() {
    MediaTypeEditor editor = new MediaTypeEditor();
    MediaType mediaType = MediaType.parseMediaType("application/json;charset=UTF-8");
    editor.setValue(mediaType);

    assertThat(editor.getAsText()).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  void setValue_withMediaType_shouldAllowGetValueAsText() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setValue(MediaType.IMAGE_PNG);

    assertThat(editor.getAsText()).isEqualTo("image/png");
    assertThat(editor.getValue()).isEqualTo(MediaType.IMAGE_PNG);
  }

  @Test
  void setValue_withNull_shouldAllowGetAsTextReturnsEmptyString() {
    MediaTypeEditor editor = new MediaTypeEditor();
    editor.setValue(MediaType.APPLICATION_OCTET_STREAM);
    editor.setValue(null);

    assertThat(editor.getAsText()).isEqualTo("");
    assertThat(editor.getValue()).isNull();
  }

}