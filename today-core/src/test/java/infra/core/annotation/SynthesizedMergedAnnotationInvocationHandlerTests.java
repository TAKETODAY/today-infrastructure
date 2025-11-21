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