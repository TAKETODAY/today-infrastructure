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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.function.Predicate;

import cn.taketoday.app.loader.Archive.Entry;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link Archive}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class ArchiveTests {

  @TempDir
  File temp;

  @Test
  void getClassPathUrlsWithOnlyIncludeFilterSearchesAllDirectories() throws Exception {
    Archive archive = mock(Archive.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    Predicate<Entry> includeFilter = (entry) -> false;
    archive.getClassPathUrls(includeFilter);
    then(archive).should().getClassPathUrls(includeFilter, Archive.ALL_ENTRIES);
  }

  @Test
  void isExplodedWhenHasRootDirectoryReturnsTrue() {
    Archive archive = mock(Archive.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    given(archive.getRootDirectory()).willReturn(this.temp);
    assertThat(archive.isExploded()).isTrue();
  }

  @Test
  void isExplodedWhenHasNoRootDirectoryReturnsFalse() {
    Archive archive = mock(Archive.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    given(archive.getRootDirectory()).willReturn(null);
    assertThat(archive.isExploded()).isFalse();
  }

  @Test
  void createFromProtectionDomainCreatesJarArchive() throws Exception {
    File jarFile = new File(this.temp, "test.jar");
    TestJar.create(jarFile);
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    CodeSource codeSource = mock(CodeSource.class);
    given(protectionDomain.getCodeSource()).willReturn(codeSource);
    given(codeSource.getLocation()).willReturn(jarFile.toURI().toURL());
    try (Archive archive = Archive.create(protectionDomain)) {
      assertThat(archive).isInstanceOf(JarFileArchive.class);
    }
  }

  @Test
  void createFromProtectionDomainWhenNoLocationThrowsException() throws Exception {
    File jarFile = new File(this.temp, "test.jar");
    TestJar.create(jarFile);
    ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
    assertThatIllegalStateException().isThrownBy(() -> Archive.create(protectionDomain))
            .withMessage("Unable to determine code source archive");
  }

  @Test
  void createFromFileWhenFileDoesNotExistThrowsException() {
    File target = new File(this.temp, "missing");
    assertThatIllegalStateException().isThrownBy(() -> Archive.create(target))
            .withMessageContaining("Unable to determine code source archive");
  }

  @Test
  void createFromFileWhenJarFileReturnsJarFileArchive() throws Exception {
    File target = new File(this.temp, "missing");
    TestJar.create(target);
    try (Archive archive = Archive.create(target)) {
      assertThat(archive).isInstanceOf(JarFileArchive.class);
    }
  }

  @Test
  void createFromFileWhenDirectoryReturnsExplodedFileArchive() throws Exception {
    File target = this.temp;
    try (Archive archive = Archive.create(target)) {
      assertThat(archive).isInstanceOf(ExplodedArchive.class);
    }
  }

}
