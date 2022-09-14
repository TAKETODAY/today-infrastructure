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

package cn.taketoday.core.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/26 23:30
 */
class ClassPathResourceTests {

  private static final String PACKAGE_PATH = "cn/taketoday/core/io";
  private static final String NONEXISTENT_RESOURCE_NAME = "nonexistent.xml";
  private static final String ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE = PACKAGE_PATH + '/' + NONEXISTENT_RESOURCE_NAME;
  private static final String ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH = '/' + ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE;

  @Nested
  class GetInputStream {

    @Test
    void withStringConstructorRaisesExceptionForNonexistentResource() {
      assertExceptionContainsAbsolutePath(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE));
    }

    @Test
    void withClassLoaderConstructorRaisesExceptionForNonexistentResource() {
      assertExceptionContainsAbsolutePath(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE, getClass().getClassLoader()));
    }

    @Test
    void withClassLiteralConstructorRaisesExceptionForNonexistentRelativeResource() {
      assertExceptionContainsAbsolutePath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()));
    }

    @Test
    void withClassLiteralConstructorRaisesExceptionForNonexistentAbsoluteResource() {
      assertExceptionContainsAbsolutePath(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE, getClass()));
    }

    private static void assertExceptionContainsAbsolutePath(ClassPathResource resource) {
      assertThatExceptionOfType(FileNotFoundException.class)
              .isThrownBy(resource::getInputStream)
              .withMessageContaining(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE);
    }

  }

  @Nested
  class GetDescription {

    @Test
    void withStringConstructor() {
      assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE));
    }

    @Test
    void withStringConstructorAndLeadingSlash() {
      assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH));
    }

    @Test
    void withClassLiteralConstructor() {
      assertDescription(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()));
    }

    @Test
    void withClassLiteralConstructorAndLeadingSlash() {
      assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH, getClass()));
    }

    @Test
    void withClassLoaderConstructor() {
      assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE, getClass().getClassLoader()));
    }

    @Test
    void withClassLoaderConstructorAndLeadingSlash() {
      assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH, getClass().getClassLoader()));
    }

    private static void assertDescription(ClassPathResource resource) {
      assertThat(resource.toString()).isEqualTo("class path resource [%s]", ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE);
    }

  }

  @Nested
  class GetPath {

    @Test
    void dropsLeadingSlashForClassLoaderAccess() {
      assertThat(new ClassPathResource("/test.html").getPath()).isEqualTo("test.html");
      assertThat(((ClassPathResource) new ClassPathResource("").createRelative("/test.html")).getPath()).isEqualTo("test.html");
    }

    @Test
    void convertsToAbsolutePathForClassRelativeAccess() {
      assertThat(new ClassPathResource("/test.html", getClass()).getPath()).isEqualTo("test.html");
      assertThat(new ClassPathResource("", getClass()).getPath()).isEqualTo(PACKAGE_PATH + "/");
      assertThat(((ClassPathResource) new ClassPathResource("", getClass()).createRelative("/test.html")).getPath()).isEqualTo("test.html");
      assertThat(((ClassPathResource) new ClassPathResource("", getClass()).createRelative("test.html")).getPath()).isEqualTo(PACKAGE_PATH + "/test.html");
    }

  }

  @Test
  void directoryNotReadable() throws Exception {
    Resource fileDir = new ClassPathResource("cn/taketoday/core");
    assertThat(fileDir.getURL()).asString().startsWith("file:");
    assertThat(fileDir.exists()).isTrue();
    assertThat(fileDir.isReadable()).isFalse();

    Resource jarDir = new ClassPathResource("reactor/core");
    assertThat(jarDir.getURL()).asString().startsWith("jar:");
    assertThat(jarDir.exists()).isTrue();
    assertThat(jarDir.isReadable()).isFalse();
  }

}
