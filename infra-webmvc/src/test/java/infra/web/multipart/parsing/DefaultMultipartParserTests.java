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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import infra.web.RequestContext;
import infra.web.server.NotMultipartRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/4 20:17
 */
class DefaultMultipartParserTests {

  @Test
  void defaultConfigurationValues() {
    DefaultMultipartParser parser = new DefaultMultipartParser();

    assertThat(parser.getMaxFields()).isEqualTo(128);
    assertThat(parser.getMaxHeaderSize()).isEqualTo(MultipartInput.DEFAULT_PART_HEADER_SIZE_MAX);
    assertThat(parser.getThreshold()).isEqualTo(DefaultMultipartParser.DEFAULT_THRESHOLD);
    assertThat(parser.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(parser.getParsingBufferSize()).isEqualTo(DefaultMultipartParser.DEFAULT_BUF_SIZE);
    assertThat(parser.getProgressListener()).isNotNull();
  }

  @Test
  void setMaxFieldsValidValue() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setMaxFields(256);
    assertThat(parser.getMaxFields()).isEqualTo(256);
  }

  @Test
  void setMaxFieldsInvalidValue() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    assertThatThrownBy(() -> parser.setMaxFields(0))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parser.setMaxFields(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void setMaxHeaderSize() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setMaxHeaderSize(8192);
    assertThat(parser.getMaxHeaderSize()).isEqualTo(8192);
  }

  @Test
  void setProgressListener() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    ProgressListener listener = (bytesRead, totalBytes, currentItem) -> { };
    parser.setProgressListener(listener);
    assertThat(parser.getProgressListener()).isEqualTo(listener);

    parser.setProgressListener(null);
    assertThat(parser.getProgressListener()).isEqualTo(ProgressListener.NOP);
  }

  @Test
  void setThresholdValidValue() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setThreshold(20480);
    assertThat(parser.getThreshold()).isEqualTo(20480);
  }

  @Test
  void setThresholdInvalidValue() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    assertThatThrownBy(() -> parser.setThreshold(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void setTempRepository(@TempDir Path tempDir) {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setTempRepository(tempDir);
    assertThat(parser.getTempRepository()).isEqualTo(tempDir);
  }

  @Test
  void setTempRepositoryNull() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    assertThatThrownBy(() -> parser.setTempRepository(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void setDefaultCharset() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setDefaultCharset(StandardCharsets.ISO_8859_1);
    assertThat(parser.getDefaultCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void setDefaultCharsetNull() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    assertThatThrownBy(() -> parser.setDefaultCharset(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void setDeleteOnExit() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setDeleteOnExit(true);
    assertThat(parser.isDeleteOnExit()).isTrue();

    parser.setDeleteOnExit(false);
    assertThat(parser.isDeleteOnExit()).isFalse();
  }

  @Test
  void setParsingBufferSizeValidValue() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    parser.setParsingBufferSize(8192);
    assertThat(parser.getParsingBufferSize()).isEqualTo(8192);
  }

  @Test
  void setParsingBufferSizeInvalidValue() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    assertThatThrownBy(() -> parser.setParsingBufferSize(0))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parser.setParsingBufferSize(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseWithNonMultipartRequest() {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    RequestContext request = mock(RequestContext.class);
    when(request.isMultipart()).thenReturn(false);
    when(request.getContentTypeAsString()).thenReturn("application/json");

    assertThatThrownBy(() -> parser.parseRequest(request))
            .isInstanceOf(NotMultipartRequestException.class)
            .hasMessageContaining("the request doesn't contain a multipart/form-data or multipart/mixed stream");
  }

}