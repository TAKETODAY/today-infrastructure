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

package infra.context.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import infra.core.OverridingClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ContextTypeMatchClassLoader}
 */
class ContextTypeMatchClassLoaderTests {

  @Test
  void testBasicClassLoading() throws Exception {
    ContextTypeMatchClassLoader loader = new ContextTypeMatchClassLoader(getClass().getClassLoader());
    Class<?> stringClass = loader.loadClass("java.lang.String");
    assertThat(stringClass).isNotNull();
    assertThat(stringClass.getName()).isEqualTo("java.lang.String");
  }

  @Test
  void testPublicDefineClass() throws Exception {
    ContextTypeMatchClassLoader loader = new ContextTypeMatchClassLoader(getClass().getClassLoader());

    // 读取类文件字节
    byte[] classBytes = getTestClassBytes();

    Class<?> definedClass = loader.publicDefineClass(
            TestClass.class.getName(), classBytes, null);

    assertThat(definedClass).isNotNull();
    assertThat(definedClass.getName()).isEqualTo(TestClass.class.getName());
  }

  @Test
  void testParentClassLoaderDelegation() throws Exception {
    ClassLoader parentLoader = new ClassLoader(getClass().getClassLoader()) { };
    ContextTypeMatchClassLoader loader = new ContextTypeMatchClassLoader(parentLoader);

    Class<?> loadedClass = loader.loadClass("java.lang.String");
    assertThat(loadedClass.getClassLoader()).isNull(); // String类由bootstrap加载器加载

    Class<?> testClass = loader.loadClass(TestClass.class.getName());
    assertThat(testClass.getClassLoader()).isNotInstanceOf(OverridingClassLoader.class);
  }

  @Test
  void testInvalidClassLoading() {
    ContextTypeMatchClassLoader loader = new ContextTypeMatchClassLoader(getClass().getClassLoader());

    assertThatThrownBy(() -> loader.loadClass("nonexistent.Class"))
            .isInstanceOf(ClassNotFoundException.class);

    assertThatThrownBy(() -> loader.publicDefineClass("invalid.Class", new byte[0], null))
            .isInstanceOf(ClassFormatError.class);
  }

  private byte[] getTestClassBytes() throws IOException {
    String className = TestClass.class.getName().replace('.', '/') + ".class";
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(className)) {
      assertThat(is).isNotNull();
      return is.readAllBytes();
    }
  }

  // 测试用的内部类
  static class TestClass {
    private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

  // 新增测试用的内部类
  static class AnotherTestClass {
    private int value;

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }
}
