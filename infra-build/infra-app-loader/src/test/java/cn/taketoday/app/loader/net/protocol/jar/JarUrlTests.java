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

package cn.taketoday.app.loader.net.protocol.jar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;

import cn.taketoday.app.loader.net.util.UrlDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarUrl}.
 *
 * @author Phillip Webb
 */
class JarUrlTests {

  @TempDir
  File temp;

  File jarFile;

  String jarFileUrlPath;

  @BeforeEach
  void setup() throws MalformedURLException {
    this.jarFile = new File(this.temp, "my.jar");
    this.jarFileUrlPath = this.jarFile.toURI().toURL().toString().substring("file:".length()).replace("!", "%21");
  }

  @Test
  void createWithFileReturnsUrl() {
    URL url = JarUrl.create(this.jarFile);
    assertThat(url).hasToString("jar:file:%s!/".formatted(this.jarFileUrlPath));
  }

  @Test
  void createWithFileAndEntryReturnsUrl() {
    JarEntry entry = new JarEntry("lib.jar");
    URL url = JarUrl.create(this.jarFile, entry);
    assertThat(url).hasToString("jar:nested:%s/!lib.jar!/".formatted(this.jarFileUrlPath));
  }

  @Test
  void createWithFileAndNullEntryReturnsUrl() {
    URL url = JarUrl.create(this.jarFile, (JarEntry) null);
    assertThat(url).hasToString("jar:file:%s!/".formatted(this.jarFileUrlPath));
  }

  @Test
  void createWithFileAndNameReturnsUrl() {
    URL url = JarUrl.create(this.jarFile, "lib.jar");
    assertThat(url).hasToString("jar:nested:%s/!lib.jar!/".formatted(this.jarFileUrlPath));
  }

  @Test
  void createWithFileAndNullNameReturnsUrl() {
    URL url = JarUrl.create(this.jarFile, (String) null);
    assertThat(url).hasToString("jar:file:%s!/".formatted(this.jarFileUrlPath));
  }

  @Test
  void createWithFileNameAndPathReturnsUrl() {
    URL url = JarUrl.create(this.jarFile, "lib.jar", "com/example/My.class");
    assertThat(url).hasToString("jar:nested:%s/!lib.jar!/com/example/My.class".formatted(this.jarFileUrlPath));
  }

  @Test
  void createWithReservedCharsInName() throws Exception {
    String badFolderName = "foo#bar!/baz/!oof";
    this.temp = new File(this.temp, badFolderName);
    setup();
    URL url = JarUrl.create(this.jarFile, "lib.jar", "com/example/My.class");
    assertThat(url).hasToString("jar:nested:%s/!lib.jar!/com/example/My.class".formatted(this.jarFileUrlPath));
    assertThat(UrlDecoder.decode(url.toString())).contains(badFolderName);
  }

}
