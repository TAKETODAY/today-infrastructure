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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:22
 */
class SynthesizedMergedAnnotationInvocationHandlerTests {

  @Test
  void cloneArrayReturnsClonedArrays() throws Exception {
    boolean[] booleanArray = { true, false };
    byte[] byteArray = { 1, 2 };
    char[] charArray = { 'a', 'b' };
    double[] doubleArray = { 1.0, 2.0 };
    float[] floatArray = { 1.0f, 2.0f };
    int[] intArray = { 1, 2 };
    long[] longArray = { 1L, 2L };
    short[] shortArray = { 1, 2 };
    String[] stringArray = { "a", "b" };

    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(booleanArray)).isNotSameAs(booleanArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(byteArray)).isNotSameAs(byteArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(charArray)).isNotSameAs(charArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(doubleArray)).isNotSameAs(doubleArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(floatArray)).isNotSameAs(floatArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(intArray)).isNotSameAs(intArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(longArray)).isNotSameAs(longArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(shortArray)).isNotSameAs(shortArray);
    assertThat(SynthesizedMergedAnnotationInvocationHandler.cloneArray(stringArray)).isNotSameAs(stringArray);
  }

}