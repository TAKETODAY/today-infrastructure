/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.embedded.undertow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import cn.taketoday.util.FileCopyUtils;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarResourceManager}.
 *
 * @author Andy Wilkinson
 */
class JarResourceManagerTests {

  @TempDir
  static File tempDir;

  @ResourceManagersTest
  void emptyPathIsHandledCorrectly(String filename, ResourceManager resourceManager) throws IOException {
    Resource resource = resourceManager.getResource("");
    assertThat(resource).isNotNull();
    assertThat(resource.isDirectory()).isTrue();
  }

  @ResourceManagersTest
  void rootPathIsHandledCorrectly(String filename, ResourceManager resourceManager) throws IOException {
    Resource resource = resourceManager.getResource("/");
    assertThat(resource).isNotNull();
    assertThat(resource.isDirectory()).isTrue();
  }

  @ResourceManagersTest
  void resourceIsFoundInJarFile(String filename, ResourceManager resourceManager) throws IOException {
    Resource resource = resourceManager.getResource("/hello.txt");
    assertThat(resource).isNotNull();
    assertThat(resource.isDirectory()).isFalse();
    assertThat(resource.getContentLength()).isEqualTo(5);
  }

  @ResourceManagersTest
  void resourceIsFoundInJarFileWithoutLeadingSlash(String filename, ResourceManager resourceManager)
          throws IOException {
    Resource resource = resourceManager.getResource("hello.txt");
    assertThat(resource).isNotNull();
    assertThat(resource.isDirectory()).isFalse();
    assertThat(resource.getContentLength()).isEqualTo(5);
  }

  static List<Arguments> resourceManagers() throws IOException {
    File jar = new File(tempDir, "test.jar");
    try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
      out.putNextEntry(new ZipEntry("hello.txt"));
      out.write("hello".getBytes());
    }
    File troublesomeNameJar = new File(tempDir, "test##1.0.jar");
    FileCopyUtils.copy(jar, troublesomeNameJar);
    return Arrays.asList(Arguments.of(jar.getName(), new JarResourceManager(jar)),
            Arguments.of(troublesomeNameJar.getName(), new JarResourceManager(troublesomeNameJar)));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("resourceManagers")
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  private @interface ResourceManagersTest {

  }

}
