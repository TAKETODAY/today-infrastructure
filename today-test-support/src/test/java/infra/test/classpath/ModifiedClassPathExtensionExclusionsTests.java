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

package infra.test.classpath;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;

/**
 * Tests for {@link ModifiedClassPathExtension} excluding entries from the class path.
 *
 * @author Christoph Dreis
 */
@ClassPathExclusions(files = "hibernate-validator-*.jar", packages = "java.net.http")
class ModifiedClassPathExtensionExclusionsTests {

  private static final String EXCLUDED_RESOURCE = "META-INF/services/jakarta.validation.spi.ValidationProvider";

  @Test
  void fileExclusionsAreFilteredFromTestClassClassLoader() {
    assertThat(getClass().getClassLoader().getResource(EXCLUDED_RESOURCE)).isNull();
  }

  @Test
  void fileExclusionsAreFilteredFromThreadContextClassLoader() {
    assertThat(Thread.currentThread().getContextClassLoader().getResource(EXCLUDED_RESOURCE)).isNull();
  }

  @Test
  void packageExclusionsAreFilteredFromTestClassClassLoader() {
    assertThat(ClassUtils.isPresent("java.net.http.HttpClient", getClass().getClassLoader())).isFalse();
  }

  @Test
  void packageExclusionsAreFilteredFromThreadContextClassLoader() {
    assertThat(ClassUtils.isPresent("java.net.http.HttpClient", Thread.currentThread().getContextClassLoader()))
            .isFalse();
  }

  @Test
  void testsThatUseHamcrestWorkCorrectly() {
    Matcher<IllegalStateException> matcher = isA(IllegalStateException.class);
    assertThat(matcher.matches(new IllegalStateException())).isTrue();
  }

}
