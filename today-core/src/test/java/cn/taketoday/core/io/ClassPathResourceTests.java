/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.taketoday.core.OverridingClassLoader;

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
  class EqualsAndHashCode {

    @Test
    void equalsAndHashCode() {
      Resource resource1 = new ClassPathResource("cn/taketoday/core/io/Resource.class");
      Resource resource2 = new ClassPathResource("cn/taketoday/core/../core/io/./Resource.class");
      Resource resource3 = new ClassPathResource("cn/taketoday/core/").createRelative("../core/io/./Resource.class");

      assertThat(resource2).isEqualTo(resource1);
      assertThat(resource3).isEqualTo(resource1);
      assertThat(resource2).hasSameHashCodeAs(resource1);
      assertThat(resource3).hasSameHashCodeAs(resource1);

      // Check whether equal/hashCode works in a HashSet.
      HashSet<Resource> resources = new HashSet<>();
      resources.add(resource1);
      resources.add(resource2);
      assertThat(resources).hasSize(1);
    }

    @Test
    void resourcesWithDifferentInputPathsAreEqual() {
      Resource resource1 = new ClassPathResource("cn/taketoday/core/io/Resource.class", getClass().getClassLoader());
      ClassPathResource resource2 = new ClassPathResource("cn/taketoday/core/../core/io/./Resource.class", getClass().getClassLoader());
      assertThat(resource2).isEqualTo(resource1);
    }

    @Test
    void resourcesWithEquivalentAbsolutePathsFromTheSameClassLoaderAreEqual() {
      ClassPathResource resource1 = new ClassPathResource("Resource.class", getClass());
      ClassPathResource resource2 = new ClassPathResource("cn/taketoday/core/io/Resource.class", getClass().getClassLoader());
      assertThat(resource1.getPath()).isEqualTo(resource2.getPath());
      assertThat(resource1).isEqualTo(resource2);
      assertThat(resource2).isEqualTo(resource1);
    }

    @Test
    void resourcesWithEquivalentAbsolutePathsHaveSameHashCode() {
      ClassPathResource resource1 = new ClassPathResource("Resource.class", getClass());
      ClassPathResource resource2 = new ClassPathResource("cn/taketoday/core/io/Resource.class", getClass().getClassLoader());
      assertThat(resource1.getPath()).isEqualTo(resource2.getPath());
      assertThat(resource1).hasSameHashCodeAs(resource2);
    }

    @Test
    void resourcesWithEquivalentAbsolutePathsFromDifferentClassLoadersAreNotEqual() {
      class SimpleThrowawayClassLoader extends OverridingClassLoader {
        SimpleThrowawayClassLoader(ClassLoader parent) {
          super(parent);
        }
      }

      ClassPathResource resource1 = new ClassPathResource("Resource.class", getClass());
      ClassPathResource resource2 = new ClassPathResource("cn/taketoday/core/io/Resource.class",
              new SimpleThrowawayClassLoader(getClass().getClassLoader()));
      assertThat(resource1.getPath()).isEqualTo(resource2.getPath());
      assertThat(resource1).isNotEqualTo(resource2);
      assertThat(resource2).isNotEqualTo(resource1);
    }

    @Test
    void relativeResourcesAreEqual() throws Exception {
      Resource resource = new ClassPathResource("dir/");
      Resource relative = resource.createRelative("subdir");
      assertThat(relative).isEqualTo(new ClassPathResource("dir/subdir"));
    }

  }

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
    Resource fileDir = new ClassPathResource("a");
    assertThat(fileDir.getURL()).asString().startsWith("file:");
    assertThat(fileDir.exists()).isTrue();
    assertThat(fileDir.isReadable()).isFalse();

    Resource jarDir = new ClassPathResource("reactor/core");
    assertThat(jarDir.getURL()).asString().startsWith("jar:");
    assertThat(jarDir.exists()).isTrue();
    assertThat(jarDir.isReadable()).isFalse();
  }

  @Test
  void emptyFileReadable(@TempDir File tempDir) throws IOException {
    File file = new File(tempDir, "empty.txt");
    assertThat(file.createNewFile()).isTrue();
    assertThat(file.isFile()).isTrue();

    ClassLoader fileClassLoader = new URLClassLoader(new URL[] { tempDir.toURI().toURL() });

    Resource emptyFile = new ClassPathResource("empty.txt", fileClassLoader);
    assertThat(emptyFile.exists()).isTrue();
    assertThat(emptyFile.isReadable()).isTrue();
    assertThat(emptyFile.contentLength()).isEqualTo(0);

    File jarFile = new File(tempDir, "test.jar");
    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(jarFile))) {
      zipOut.putNextEntry(new ZipEntry("empty2.txt"));
      zipOut.closeEntry();
    }
    assertThat(jarFile.isFile()).isTrue();

    ClassLoader jarClassLoader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() });

    Resource emptyJarEntry = new ClassPathResource("empty2.txt", jarClassLoader);
    assertThat(emptyJarEntry.exists()).isTrue();
    assertThat(emptyJarEntry.isReadable()).isTrue();
    assertThat(emptyJarEntry.contentLength()).isEqualTo(0);
  }

}
