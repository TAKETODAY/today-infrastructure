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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
import cn.taketoday.app.loader.zip.ZipContent;
import cn.taketoday.app.loader.zip.ZipContent.Entry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityInfo}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class SecurityInfoTests {

  @TempDir
  File temp;

  @Test
  void getWhenNoSignatureFileReturnsNone() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    try (ZipContent content = ZipContent.open(file.toPath())) {
      SecurityInfo info = SecurityInfo.get(content);
      assertThat(info).isSameAs(SecurityInfo.NONE);
      for (int i = 0; i < content.size(); i++) {
        Entry entry = content.getEntry(i);
        assertThat(info.getCertificates(entry)).isNull();
        assertThat(info.getCodeSigners(entry)).isNull();
      }
    }
  }

  @Test
  void getWhenHasSignatureFileButNoSecurityMaterialReturnsNone() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file, false, true);
    try (ZipContent content = ZipContent.open(file.toPath())) {
      assertThat(content.hasJarSignatureFile()).isTrue();
      SecurityInfo info = SecurityInfo.get(content);
      assertThat(info).isSameAs(SecurityInfo.NONE);
    }
  }

  @Test
  void getWhenJarIsSigned() throws Exception {
    File file = TestJar.getSigned();
    try (ZipContent content = ZipContent.open(file.toPath())) {
      assertThat(content.hasJarSignatureFile()).isTrue();
      SecurityInfo info = SecurityInfo.get(content);
      for (int i = 0; i < content.size(); i++) {
        Entry entry = content.getEntry(i);
        if (entry.getName().endsWith(".class")) {
          assertThat(info.getCertificates(entry)).isNotNull();
          assertThat(info.getCodeSigners(entry)).isNotNull();
        }
      }
    }
  }

}
