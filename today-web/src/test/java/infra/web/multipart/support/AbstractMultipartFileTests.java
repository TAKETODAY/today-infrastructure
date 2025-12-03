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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:47
 */
class AbstractMultipartFileTests {

  @Test
  void getBytesReturnsCachedBytesWhenAvailable() throws IOException {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);
    byte[] cached = "cached content".getBytes();
    multipartFile.cachedBytes = cached;

    when(multipartFile.getContentAsByteArray()).thenCallRealMethod();

    assertThat(multipartFile.getContentAsByteArray()).isSameAs(cached);
  }

  @Test
  void getBytesCallsDoGetBytesWhenCacheIsNull() throws IOException {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);
    byte[] content = "new content".getBytes();

    when(multipartFile.getContentAsByteArray()).thenCallRealMethod();
    when(multipartFile.doGetBytes()).thenReturn(content);

    byte[] result = multipartFile.getContentAsByteArray();

    assertThat(result).isNotNull().isEqualTo(content);
    assertThat(multipartFile.cachedBytes).isSameAs(content);
  }

  @Test
  void isFormFieldAlwaysReturnsFalse() {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);

    when(multipartFile.isFormField()).thenCallRealMethod();

    assertThat(multipartFile.isFormField()).isFalse();
  }

  @Test
  void getContentAsStringReturnsStringRepresentationOfBytes() throws IOException {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);
    String content = "test content";

    when(multipartFile.getContentAsString()).thenCallRealMethod();
    when(multipartFile.getContentAsByteArray()).thenReturn(content.getBytes(StandardCharsets.UTF_8));

    assertThat(multipartFile.getContentAsString()).isEqualTo(content);
  }

  @Test
  void getContentAsStringWrapsIOExceptionInRuntime() throws IOException {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);

    when(multipartFile.getContentAsString()).thenCallRealMethod();
    when(multipartFile.getContentAsByteArray()).thenThrow(new IOException("test exception"));

    assertThatExceptionOfType(IOException.class)
            .isThrownBy(multipartFile::getContentAsString);
  }

  @Test
  void cleanupClearsCachedBytesAndCallsDeleteInternal() throws IOException {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);
    multipartFile.cachedBytes = new byte[0];

    doCallRealMethod().when(multipartFile).cleanup();

    doNothing().when(multipartFile).deleteInternal();

    multipartFile.cleanup();

    assertThat(multipartFile.cachedBytes).isNull();
    verify(multipartFile).deleteInternal();
  }

  @Test
  void transferToCreatesParentDirectoriesIfNeeded() throws IOException {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);
    File parentDir = mock(File.class);
    File dest = mock(File.class);

    doCallRealMethod().when(multipartFile).transferTo(any(File.class));
    when(dest.getParentFile()).thenReturn(parentDir);
    when(parentDir.exists()).thenReturn(false);
    when(parentDir.mkdirs()).thenReturn(true);
    doNothing().when(multipartFile).saveInternal(dest);

    assertThatNoException().isThrownBy(() -> multipartFile.transferTo(dest));
    verify(parentDir).mkdirs();
  }

  @Test
  void toStringReturnsFormattedString() {
    AbstractMultipartFile multipartFile = mock(AbstractMultipartFile.class);

    when(multipartFile.toString()).thenCallRealMethod();
    when(multipartFile.getName()).thenReturn("test-file");

    String result = multipartFile.toString();

    assertThat(result).endsWith("'test-file'");
  }

}