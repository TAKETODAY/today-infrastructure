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

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static org.assertj.core.api.Assertions.*;
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