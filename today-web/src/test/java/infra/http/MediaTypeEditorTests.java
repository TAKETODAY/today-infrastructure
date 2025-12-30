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