/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 19:30
 */
class OverridingClassLoaderTests {

  @Test
  void testExcludedPackages() {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader());
    assertThat(loader.isEligibleForOverriding("java.lang.String")).isFalse();
    assertThat(loader.isEligibleForOverriding("javax.swing.JFrame")).isFalse();
    assertThat(loader.isEligibleForOverriding("com.example.MyClass")).isTrue();
  }

  @Test
  void testOverrideDelegate() throws ClassNotFoundException {
    ClassLoader parentLoader = getClass().getClassLoader();
    ClassLoader delegateLoader = new ClassLoader(parentLoader) { };

    OverridingClassLoader loader = new OverridingClassLoader(parentLoader, delegateLoader);
    loader.excludePackage("java."); // 确保排除java包

    Class<?> stringClass = loader.loadClass("java.lang.String");
    assertThat(stringClass.getClassLoader()).isNull();
  }

  @Test
  void shouldLoadFromParentWhenExcluded() throws ClassNotFoundException {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader());
    loader.excludePackage(getClass().getPackage().getName());

    Class<?> loaded = loader.loadClass(getClass().getName());
    assertThat(loaded.getClassLoader()).isEqualTo(getClass().getClassLoader());
  }

  @Test
  void shouldOverrideFromDelegateWhenEligible() throws ClassNotFoundException {
    ClassLoader delegate = new ClassLoader() {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        return SimpleClass.class;
      }
    };

    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader(), delegate);

    Class<?> loaded = loader.loadClass(SimpleClass.class.getName());
    assertThat(loaded).isEqualTo(SimpleClass.class);
  }

  @Test
  void shouldReturnNullWhenNoStreamFound() throws Exception {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader()) {
      @Override
      protected InputStream openStreamForClass(String name) {
        return null;
      }
    };

    byte[] bytes = loader.loadBytesForClass("nonexistent.Class");
    assertThat(bytes).isNull();
  }

  @Test
  void shouldThrowClassNotFoundWhenStreamReadFails() {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader()) {
      @Override
      protected InputStream openStreamForClass(String name) {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Read failed");
          }
        };
      }
    };

    assertThatThrownBy(() -> loader.loadBytesForClass("failed.Class"))
            .isInstanceOf(ClassNotFoundException.class)
            .hasMessageContaining("Cannot load resource");
  }

  @Test
  void shouldExcludeMultiplePackages() {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader());
    loader.excludePackage("com.example.");
    loader.excludePackage("org.test.");

    assertThat(loader.isEligibleForOverriding("com.example.MyClass")).isFalse();
    assertThat(loader.isEligibleForOverriding("org.test.TestClass")).isFalse();
    assertThat(loader.isEligibleForOverriding("com.other.Class")).isTrue();
  }

  @Test
  void shouldExcludeAllClassesWhenExcludingRootPackage() {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader());
    loader.excludePackage(""); // 排除根包，相当于排除所有类

    assertThat(loader.isEligibleForOverriding("any.class.Name")).isFalse();
    assertThat(loader.isEligibleForOverriding("com.example.MyClass")).isFalse();
  }

  @Test
  void shouldLoadSameClassInstanceWhenCalledMultipleTimes() throws ClassNotFoundException {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader());

    Class<?> first = loader.loadClass(SimpleClass.class.getName());
    Class<?> second = loader.loadClass(SimpleClass.class.getName());

    assertThat(first).isSameAs(second);
  }

  @Test
  void shouldLoadClassWithResolve() throws ClassNotFoundException {
    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader());
    Class<?> loaded = loader.loadClass(SimpleClass.class.getName(), true);
    assertThat(loaded).isNotNull();
  }

  @Test
  void shouldNotAffectOriginalBytesWhenTransforming() throws ClassNotFoundException {
    byte[] original = new byte[] { 1, 2, 3 };
    byte[] modified = new byte[] { 4, 5, 6 };

    OverridingClassLoader loader = new OverridingClassLoader(getClass().getClassLoader()) {
      @Override
      protected byte[] transformIfNecessary(String name, byte[] bytes) {
        assertThat(bytes).isEqualTo(original);
        return modified;
      }
    };

    byte[] result = loader.transformIfNecessary("test.Class", original);
    assertThat(result).isEqualTo(modified);
    assertThat(original).containsExactly(1, 2, 3);
  }

  private static class SimpleClass { }

}
