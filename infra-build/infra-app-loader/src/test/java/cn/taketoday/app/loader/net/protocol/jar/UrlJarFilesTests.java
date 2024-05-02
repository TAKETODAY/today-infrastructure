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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.net.protocol.Handlers;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link UrlJarFiles}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class UrlJarFilesTests {

  @TempDir
  File temp;

  private UrlJarFileFactory factory = mock(UrlJarFileFactory.class);

  private final UrlJarFiles jarFiles = new UrlJarFiles(this.factory);

  private File file;

  private URL url;

  @BeforeAll
  static void registerHandlers() {
    Handlers.register();
  }

  @BeforeEach
  void setup() throws Exception {
    this.file = new File(this.temp, "test.jar");
    this.url = new URL("nested:" + this.file.getAbsolutePath() + "/!nested.jar");
    TestJar.create(this.file);
  }

  @Test
  void getOrCreateWhenNotUsingCachesAlwaysCreatesNewJarFile() throws Exception {
    given(this.factory.createJarFile(any(), any())).willCallRealMethod();
    JarFile jarFile1 = this.jarFiles.getOrCreate(false, this.url);
    JarFile jarFile2 = this.jarFiles.getOrCreate(false, this.url);
    JarFile jarFile3 = this.jarFiles.getOrCreate(false, this.url);
    assertThat(jarFile1).isNotSameAs(jarFile2).isNotSameAs(jarFile3);
  }

  @Test
  void getOrCreateWhenUsingCachingReturnsCachedWhenAvailable() throws Exception {
    given(this.factory.createJarFile(any(), any())).willCallRealMethod();
    JarFile jarFile1 = this.jarFiles.getOrCreate(true, this.url);
    this.jarFiles.cacheIfAbsent(true, this.url, jarFile1);
    JarFile jarFile2 = this.jarFiles.getOrCreate(true, this.url);
    JarFile jarFile3 = this.jarFiles.getOrCreate(true, this.url);
    assertThat(jarFile1).isSameAs(jarFile2).isSameAs(jarFile3);
  }

  @Test
  void getCachedWhenNotCachedReturnsNull() {
    assertThat(this.jarFiles.getCached(this.url)).isNull();
  }

  @Test
  void getCachedWhenCachedReturnsCachedJar() throws Exception {
    given(this.factory.createJarFile(any(), any())).willCallRealMethod();
    JarFile jarFile = this.factory.createJarFile(this.url, null);
    this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
    assertThat(this.jarFiles.getCached(this.url)).isSameAs(jarFile);
  }

  @Test
  void cacheIfAbsentWhenNotUsingCachesDoesNotCacheAndReturnsFalse() throws Exception {
    given(this.factory.createJarFile(any(), any())).willCallRealMethod();
    JarFile jarFile = this.factory.createJarFile(this.url, null);
    this.jarFiles.cacheIfAbsent(false, this.url, jarFile);
    assertThat(this.jarFiles.getCached(this.url)).isNull();
  }

  @Test
  void cacheIfAbsentWhenUsingCachingAndNotAlreadyCachedCachesAndReturnsTrue() throws Exception {
    given(this.factory.createJarFile(any(), any())).willCallRealMethod();
    JarFile jarFile = this.factory.createJarFile(this.url, null);
    assertThat(this.jarFiles.cacheIfAbsent(true, this.url, jarFile)).isTrue();
    assertThat(this.jarFiles.getCached(this.url)).isSameAs(jarFile);
  }

  @Test
  void cacheIfAbsentWhenUsingCachingAndAlreadyCachedLeavesCacheAndReturnsFalse() throws Exception {
    given(this.factory.createJarFile(any(), any())).willCallRealMethod();
    JarFile jarFile1 = this.factory.createJarFile(this.url, null);
    JarFile jarFile2 = this.factory.createJarFile(this.url, null);
    assertThat(this.jarFiles.cacheIfAbsent(true, this.url, jarFile1)).isTrue();
    assertThat(this.jarFiles.cacheIfAbsent(true, this.url, jarFile2)).isFalse();
    assertThat(this.jarFiles.getCached(this.url)).isSameAs(jarFile1);
  }

  @Test
  void closeIfNotCachedWhenNotCachedClosesJarFile() throws Exception {
    JarFile jarFile = mock(JarFile.class);
    this.jarFiles.closeIfNotCached(this.url, jarFile);
    then(jarFile).should().close();
  }

  @Test
  void closeIfNotCachedWhenCachedDoesNotCloseJarFile() throws Exception {
    JarFile jarFile = mock(JarFile.class);
    this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
    this.jarFiles.closeIfNotCached(this.url, jarFile);
    then(jarFile).should(never()).close();
  }

  @Test
  void reconnectReconnectsAndAppliesUseCaches() throws Exception {
    JarFile jarFile = mock(JarFile.class);
    this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
    URLConnection existingConnection = mock(URLConnection.class);
    given(existingConnection.getUseCaches()).willReturn(true);
    URLConnection connection = this.jarFiles.reconnect(jarFile, existingConnection);
    assertThat(connection).isNotSameAs(existingConnection);
    assertThat(connection.getUseCaches()).isTrue();
  }

  @Test
  void reconnectWhenExistingConnectionIsNullReconnects() throws Exception {
    JarFile jarFile = mock(JarFile.class);
    this.jarFiles.cacheIfAbsent(true, this.url, jarFile);
    URLConnection connection = this.jarFiles.reconnect(jarFile, null);
    assertThat(connection).isNotNull();
    assertThat(connection.getUseCaches()).isTrue();
  }

}
