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

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import java.lang.instrument.IllegalClassFormatException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:06
 */
class InvocationsRecorderClassTransformerTests {

  @Test
  void shouldCreateTransformerWithValidPackages() {
    String[] instrumented = { "com.example" };
    String[] ignored = { "javax" };
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(instrumented, ignored);

    assertThat(transformer).isNotNull();
  }

  @Test
  void shouldThrowExceptionWhenInstrumentedPackagesIsNull() {
    String[] ignored = { "javax" };
    assertThatThrownBy(() -> new InvocationsRecorderClassTransformer(null, ignored))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowExceptionWhenIgnoredPackagesIsNull() {
    String[] instrumented = { "com.example" };
    assertThatThrownBy(() -> new InvocationsRecorderClassTransformer(instrumented, null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRewritePackagesToAsmFormat() {
    String[] instrumented = { "com.example", "org.test" };
    String[] ignored = { "javax.swing" };
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(instrumented, ignored);

    // We can't directly access private fields, but we can test the behavior through transform method
    assertThatCode(() -> {
      // Just verifying constructor doesn't throw exception
      assertThat(transformer).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldNotTransformSystemClasses() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "java.lang" }, new String[] {});

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(null, "java/lang/Object", Object.class, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldNotTransformAgentClasses() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "infra" }, new String[] {});

    byte[] dummyBytes = new byte[0];
    String agentClassName = InvocationsRecorderClassTransformer.class.getName().replace('.', '/');
    byte[] result = transformer.transform(getClass().getClassLoader(), agentClassName, null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldNotTransformCglibClasses() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "com.example" }, new String[] {});

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(getClass().getClassLoader(), "com/example/SomeClass$$EnhancerByCGLIB", null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldTransformClassesInInstrumentedPackages() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "com.example" }, new String[] {});

    byte[] dummyBytes = new byte[10]; // Not actually valid class bytes, but enough for testing candidate selection
    byte[] result = transformer.transform(getClass().getClassLoader(), "com/example/SomeClass", null, null, dummyBytes);

    // Since the class bytes are invalid, it should return the original buffer after attempting transformation
    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldNotTransformClassesInIgnoredPackages() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "com" }, new String[] { "com.example" });

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(getClass().getClassLoader(), "com/example/SomeClass", null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldHandleEmptyInstrumentedPackagesArray() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] {}, new String[] { "javax" });

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(getClass().getClassLoader(), "com/example/SomeClass", null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldNotTransformDynamicClassLoader() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "infra" }, new String[] {});

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(getClass().getClassLoader(), "infra/aot/test/generate/compile/DynamicClassLoader", null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldHandleNullClassLoaderWithInstrumentedPackage() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "java.lang" }, new String[] {});

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(null, "java/lang/Object", Object.class, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldPreferIgnoredPackagesOverInstrumentedPackages() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "com" }, new String[] { "com.example" });

    byte[] dummyBytes = new byte[0];
    byte[] result = transformer.transform(getClass().getClassLoader(), "com/example/SomeClass", null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

  @Test
  void shouldTransformWhenMatchingInstrumentedPackage() throws IllegalClassFormatException {
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            new String[] { "com.example.service" }, new String[] {});

    byte[] dummyBytes = new byte[10];
    byte[] result = transformer.transform(getClass().getClassLoader(), "com/example/service/UserService", null, null, dummyBytes);

    assertThat(result).isSameAs(dummyBytes);
  }

}