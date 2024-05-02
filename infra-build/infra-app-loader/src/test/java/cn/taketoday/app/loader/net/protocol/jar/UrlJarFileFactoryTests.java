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

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.net.protocol.Handlers;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UrlJarFileFactory}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class UrlJarFileFactoryTests {

  @TempDir
  File temp;

  private final UrlJarFileFactory factory = new UrlJarFileFactory();

  @Mock
  private Consumer<JarFile> closeAction;

  @BeforeAll
  static void registerHandlers() {
    Handlers.register();
  }

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void createJarFileWhenLocalFile() throws Throwable {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    URL url = file.toURI().toURL();
    JarFile jarFile = this.factory.createJarFile(url, this.closeAction);
    assertThat(jarFile).isInstanceOf(UrlJarFile.class);
    assertThat(jarFile).hasFieldOrPropertyWithValue("closeAction", this.closeAction);
  }

  @Test
  void createJarFileWhenNested() throws Throwable {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    URL url = new URL("nested:" + file.getPath() + "/!nested.jar");
    JarFile jarFile = this.factory.createJarFile(url, this.closeAction);
    assertThat(jarFile).isInstanceOf(UrlNestedJarFile.class);
    assertThat(jarFile).hasFieldOrPropertyWithValue("closeAction", this.closeAction);
  }

  @Test
  void createJarFileWhenStream() throws Exception {
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/test", (exchange) -> {
      exchange.sendResponseHeaders(200, file.length());
      try (InputStream in = new FileInputStream(file)) {
        in.transferTo(exchange.getResponseBody());
      }
      exchange.close();
    });
    server.start();
    try {
      URL url = new URL("http://localhost:" + server.getAddress().getPort() + "/test");
      JarFile jarFile = this.factory.createJarFile(url, this.closeAction);
      assertThat(jarFile).isInstanceOf(UrlJarFile.class);
      assertThat(jarFile).hasFieldOrPropertyWithValue("closeAction", this.closeAction);
    }
    finally {
      server.stop(0);
    }
  }

  @Test
  void createWhenHasRuntimeRef() {

  }

}
