/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.reflect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ObjectMethodInvoker}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ObjectMethodInvokerTests {

  @Test
  @DisplayName("Should invoke toString method")
  void shouldInvokeToString() throws Exception {
    Method toStringMethod = Object.class.getMethod("toString");
    MethodInvoker invoker = MethodInvoker.forMethod(toStringMethod);

    assertThat(invoker).isInstanceOf(ObjectMethodInvoker.class);

    Object result = invoker.invoke("Hello", null);
    assertThat(result).isEqualTo("Hello");

    result = invoker.invoke(123, null);
    assertThat(result).isEqualTo("123");
  }

  @Test
  @DisplayName("Should invoke hashCode method")
  void shouldInvokeHashCode() throws Exception {
    Method hashCodeMethod = Object.class.getMethod("hashCode");
    MethodInvoker invoker = MethodInvoker.forMethod(hashCodeMethod);

    assertThat(invoker).isInstanceOf(ObjectMethodInvoker.class);

    String testString = "test";
    Object result = invoker.invoke(testString, null);
    assertThat(result).isEqualTo(testString.hashCode());

    Integer testInt = 42;
    result = invoker.invoke(testInt, null);
    assertThat(result).isEqualTo(testInt.hashCode());
  }

  @Test
  @DisplayName("Should invoke equals method with equal object")
  void shouldInvokeEqualsWithEqualObject() throws Exception {
    Method equalsMethod = Object.class.getMethod("equals", Object.class);
    MethodInvoker invoker = MethodInvoker.forMethod(equalsMethod);

    assertThat(invoker).isInstanceOf(ObjectMethodInvoker.class);

    String str1 = "test";
    String str2 = "test";
    Object result = invoker.invoke(str1, new Object[] { str2 });
    assertThat(result).isEqualTo(true);
  }

  @Test
  @DisplayName("Should invoke equals method with unequal object")
  void shouldInvokeEqualsWithUnequalObject() throws Exception {
    Method equalsMethod = Object.class.getMethod("equals", Object.class);
    MethodInvoker invoker = MethodInvoker.forMethod(equalsMethod);

    String str1 = "test1";
    String str2 = "test2";
    Object result = invoker.invoke(str1, new Object[] { str2 });
    assertThat(result).isEqualTo(false);
  }

  @Test
  @DisplayName("Should invoke equals method with null argument")
  void shouldInvokeEqualsWithNullArgument() throws Exception {
    Method equalsMethod = Object.class.getMethod("equals", Object.class);
    MethodInvoker invoker = MethodInvoker.forMethod(equalsMethod);

    String str = "test";
    Object result = invoker.invoke(str, new Object[] { null });
    assertThat(result).isEqualTo(false);
  }

  @Test
  @DisplayName("Should invoke equals method with empty args")
  void shouldInvokeEqualsWithEmptyArgs() throws Exception {
    Method equalsMethod = Object.class.getMethod("equals", Object.class);
    MethodInvoker invoker = MethodInvoker.forMethod(equalsMethod);

    String str = "test";
    Object result = invoker.invoke(str, new Object[] {});
    assertThat(result).isEqualTo(false);
  }

  @Test
  @DisplayName("Should invoke getClass method")
  void shouldInvokeGetClass() throws Exception {
    Method getClassMethod = Object.class.getMethod("getClass");
    MethodInvoker invoker = MethodInvoker.forMethod(getClassMethod);

    assertThat(invoker).isInstanceOf(ObjectMethodInvoker.class);

    String testString = "test";
    Object result = invoker.invoke(testString, null);
    assertThat(result).isEqualTo(String.class);

    Integer testInt = 42;
    result = invoker.invoke(testInt, null);
    assertThat(result).isEqualTo(Integer.class);
  }

  @Test
  @DisplayName("Should throw NPE when invoking instance method on null object")
  void shouldThrowNPEOnNullObject() throws Exception {
    Method toStringMethod = Object.class.getMethod("toString");
    MethodInvoker invoker = MethodInvoker.forMethod(toStringMethod);

    assertThatThrownBy(() -> invoker.invoke(null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Cannot invoke toString on null object");
  }

  @Test
  @DisplayName("Should handle custom objects correctly")
  void shouldHandleCustomObjects() throws Exception {
    TestObject testObj = new TestObject("test", 42);

    Method toStringMethod = Object.class.getMethod("toString");
    MethodInvoker toStringInvoker = MethodInvoker.forMethod(toStringMethod);
    Object result = toStringInvoker.invoke(testObj, null);
    assertThat(result).isEqualTo("TestObject{name='test', age=42}");

    Method hashCodeMethod = Object.class.getMethod("hashCode");
    MethodInvoker hashCodeInvoker = MethodInvoker.forMethod(hashCodeMethod);
    result = hashCodeInvoker.invoke(testObj, null);
    assertThat(result).isEqualTo(testObj.hashCode());

    Method equalsMethod = Object.class.getMethod("equals", Object.class);
    MethodInvoker equalsInvoker = MethodInvoker.forMethod(equalsMethod);
    TestObject sameObj = new TestObject("test", 42);
    result = equalsInvoker.invoke(testObj, new Object[] { sameObj });
    assertThat(result).isEqualTo(true);

    Method getClassMethod = Object.class.getMethod("getClass");
    MethodInvoker getClassInvoker = MethodInvoker.forMethod(getClassMethod);
    result = getClassInvoker.invoke(testObj, null);
    assertThat(result).isEqualTo(TestObject.class);
  }

  @Test
  @DisplayName("Should create ObjectMethodInvoker for Object methods via forMethod with targetClass")
  void shouldCreateObjectMethodInvokerWithTargetClass() throws Exception {
    Method toStringMethod = Object.class.getMethod("toString");
    MethodInvoker invoker = MethodInvoker.forMethod(toStringMethod, String.class);

    assertThat(invoker).isInstanceOf(ObjectMethodInvoker.class);

    Object result = invoker.invoke("test", null);
    assertThat(result).isEqualTo("test");
  }

  @Test
  @DisplayName("Should verify ObjectMethodType detection")
  void shouldVerifyObjectMethodTypeDetection() throws Exception {
    assertThat(ObjectMethodType.forMethod(Object.class.getMethod("toString"))).isEqualTo(ObjectMethodType.TOSTRING);
    assertThat(ObjectMethodType.forMethod(Object.class.getMethod("hashCode"))).isEqualTo(ObjectMethodType.HASHCODE);
    assertThat(ObjectMethodType.forMethod(Object.class.getMethod("equals", Object.class))).isEqualTo(ObjectMethodType.EQUALS);
    assertThat(ObjectMethodType.forMethod(Object.class.getMethod("getClass"))).isEqualTo(ObjectMethodType.GETCLASS);
  }

  @Test
  @DisplayName("Should return null for non-Object methods")
  void shouldReturnNullForNonObjectMethods() throws Exception {
    assertThat(ObjectMethodType.forMethod(String.class.getMethod("length"))).isNull();
    assertThat(ObjectMethodType.forMethod(String.class.getMethod("toUpperCase"))).isNull();
  }

  /**
   * Test helper class
   */
  static class TestObject {
    private final String name;
    private final int age;

    TestObject(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String toString() {
      return "TestObject{name='%s', age=%d}".formatted(name, age);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof TestObject that))
        return false;
      return age == that.age && name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode() * 31 + age;
    }
  }
}

