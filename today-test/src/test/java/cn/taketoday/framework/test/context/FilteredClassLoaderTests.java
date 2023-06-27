/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.context;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FilteredClassLoader}.
 *
 * @author Phillip Webb
 * @author Roy Jacobs
 */
class FilteredClassLoaderTests {

  static ClassPathResource TEST_RESOURCE = new ClassPathResource(
          "cn/taketoday/framework/test/context/FilteredClassLoaderTestsResource.txt");

  @Test
  void loadClassWhenFilteredOnPackageShouldThrowClassNotFound() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader(
            FilteredClassLoaderTests.class.getPackage().getName())) {
      assertThatExceptionOfType(ClassNotFoundException.class)
              .isThrownBy(() -> Class.forName(getClass().getName(), false, classLoader));
    }
  }

  @Test
  void loadClassWhenFilteredOnClassShouldThrowClassNotFound() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader(FilteredClassLoaderTests.class)) {
      assertThatExceptionOfType(ClassNotFoundException.class)
              .isThrownBy(() -> Class.forName(getClass().getName(), false, classLoader));
    }
  }

  @Test
  void loadClassWhenNotFilteredShouldLoadClass() throws Exception {
    FilteredClassLoader classLoader = new FilteredClassLoader((className) -> false);
    Class<?> loaded = Class.forName(getClass().getName(), false, classLoader);
    assertThat(loaded.getName()).isEqualTo(getClass().getName());
    classLoader.close();
  }

  @Test
  void loadResourceWhenFilteredOnResourceShouldReturnNotFound() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader(TEST_RESOURCE)) {
      final URL loaded = classLoader.getResource(TEST_RESOURCE.getPath());
      assertThat(loaded).isNull();
    }
  }

  @Test
  void loadResourceWhenNotFilteredShouldLoadResource() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader((resourceName) -> false)) {
      final URL loaded = classLoader.getResource(TEST_RESOURCE.getPath());
      assertThat(loaded).isNotNull();
    }
  }

  @Test
  void loadResourcesWhenFilteredOnResourceShouldReturnNotFound() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader(TEST_RESOURCE)) {
      final Enumeration<URL> loaded = classLoader.getResources(TEST_RESOURCE.getPath());
      assertThat(loaded.hasMoreElements()).isFalse();
    }
  }

  @Test
  void loadResourcesWhenNotFilteredShouldLoadResource() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader((resourceName) -> false)) {
      final Enumeration<URL> loaded = classLoader.getResources(TEST_RESOURCE.getPath());
      assertThat(loaded.hasMoreElements()).isTrue();
    }
  }

  @Test
  void loadResourceAsStreamWhenFilteredOnResourceShouldReturnNotFound() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader(TEST_RESOURCE)) {
      final InputStream loaded = classLoader.getResourceAsStream(TEST_RESOURCE.getPath());
      assertThat(loaded).isNull();
    }
  }

  @Test
  void loadResourceAsStreamWhenNotFilteredShouldLoadResource() throws Exception {
    try (FilteredClassLoader classLoader = new FilteredClassLoader((resourceName) -> false)) {
      final InputStream loaded = classLoader.getResourceAsStream(TEST_RESOURCE.getPath());
      assertThat(loaded).isNotNull();
    }
  }

  @Test
  void publicDefineClassWhenFilteredThrowsException() throws Exception {
    Class<FilteredClassLoaderTests> hiddenClass = FilteredClassLoaderTests.class;
    try (FilteredClassLoader classLoader = new FilteredClassLoader(hiddenClass)) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> classLoader.publicDefineClass(hiddenClass.getName(), new byte[] {}, null));
    }
  }

}
