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

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 18:39
 */
class WeavingTransformerTests {

  @Test
  void transformWithNoTransformersReturnsSameBytes() {
    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    byte[] bytes = "test".getBytes();
    byte[] result = transformer.transformIfNecessary("com.example.Test", bytes);
    assertThat(result).isEqualTo(bytes);
  }

  @Test
  void transformWithSingleTransformer() throws IllegalClassFormatException {
    ClassFileTransformer mockTransformer = mock(ClassFileTransformer.class);
    byte[] input = "test".getBytes();
    byte[] transformed = "transformed".getBytes();
    when(mockTransformer.transform(any(), eq("com/example/Test"), isNull(), isNull(), eq(input)))
            .thenReturn(transformed);

    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    transformer.addTransformer(mockTransformer);

    byte[] result = transformer.transformIfNecessary("com.example.Test", input);
    assertThat(result).isEqualTo(transformed);
  }

  @Test
  void transformWithMultipleTransformers() throws IllegalClassFormatException {
    ClassFileTransformer transformer1 = mock(ClassFileTransformer.class);
    ClassFileTransformer transformer2 = mock(ClassFileTransformer.class);
    byte[] input = "test".getBytes();
    byte[] intermediate = "intermediate".getBytes();
    byte[] output = "output".getBytes();

    when(transformer1.transform(any(), eq("com/example/Test"), isNull(), isNull(), eq(input)))
            .thenReturn(intermediate);
    when(transformer2.transform(any(), eq("com/example/Test"), isNull(), isNull(), eq(intermediate)))
            .thenReturn(output);

    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    transformer.addTransformer(transformer1);
    transformer.addTransformer(transformer2);

    byte[] result = transformer.transformIfNecessary("com.example.Test", input);
    assertThat(result).isEqualTo(output);
  }

  @Test
  void transformerReturningNullSkipsTransformation() throws IllegalClassFormatException {
    ClassFileTransformer mockTransformer = mock(ClassFileTransformer.class);
    byte[] input = "test".getBytes();
    when(mockTransformer.transform(any(), anyString(), isNull(), isNull(), any()))
            .thenReturn(null);

    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    transformer.addTransformer(mockTransformer);

    byte[] result = transformer.transformIfNecessary("com.example.Test", input);
    assertThat(result).isEqualTo(input);
  }

  @Test
  void transformWithProtectionDomain() throws IllegalClassFormatException {
    ClassFileTransformer mockTransformer = mock(ClassFileTransformer.class);
    byte[] input = "test".getBytes();
    byte[] transformed = "transformed".getBytes();
    ProtectionDomain pd = new ProtectionDomain(null, null);

    when(mockTransformer.transform(any(), eq("com/example/Test"), isNull(), eq(pd), eq(input)))
            .thenReturn(transformed);

    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    transformer.addTransformer(mockTransformer);

    byte[] result = transformer.transformIfNecessary("com.example.Test", "com/example/Test", input, pd);
    assertThat(result).isEqualTo(transformed);
  }

  @Test
  void nullTransformerNotAllowed() {
    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    assertThatThrownBy(() -> transformer.addTransformer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transformer is required");
  }

  @Test
  void transformerFailureThrowsException() throws IllegalClassFormatException {
    ClassFileTransformer mockTransformer = mock(ClassFileTransformer.class);
    when(mockTransformer.transform(any(), anyString(), isNull(), isNull(), any()))
            .thenThrow(new IllegalClassFormatException("Bad format"));

    WeavingTransformer transformer = new WeavingTransformer(getClass().getClassLoader());
    transformer.addTransformer(mockTransformer);

    assertThatThrownBy(() -> transformer.transformIfNecessary("com.example.Test", "test".getBytes()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Class file transformation failed")
            .hasCauseInstanceOf(IllegalClassFormatException.class);
  }

}