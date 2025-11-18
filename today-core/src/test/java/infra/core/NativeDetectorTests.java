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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import infra.core.NativeDetector.Context;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/18 22:00
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NativeDetectorTests {

  @Test
  void inNativeImageReturnsTrueWhenImageCodeIsNotNull() throws Exception {
    setImageCode("runtime", new Runnable() {
      @Override
      public void run() {
        assertThat(NativeDetector.inNativeImage()).isTrue();
        assertThat(NativeDetector.inNativeImage(Context.RUN)).isTrue();
        assertThat(NativeDetector.inNativeImage(Context.BUILD)).isFalse();
      }
    });

  }

//  @Test
//  void inNativeImageWithContextReturnsFalseForNonMatchingContext() throws Exception {
//    setImageCode("buildtime", new Runnable() {
//      @Override
//      public void run() {
//        assertThat(NativeDetector.inNativeImage(Context.RUN)).isFalse();
//        assertThat(NativeDetector.inNativeImage(Context.BUILD)).isTrue();
//      }
//    });
//  }
//
//  @Test
//  void inNativeImageWithContextReturnsFalseWhenImageCodeIsNull() throws Exception {
//    setImageCode(null, new Runnable() {
//      @Override
//      public void run() {
//        assertThat(NativeDetector.inNativeImage(Context.RUN)).isFalse();
//        assertThat(NativeDetector.inNativeImage(Context.BUILD)).isFalse();
//      }
//    });
//
//  }

  @Test
  void contextToStringReturnsCorrectKey() {
    assertThat(Context.RUN.toString()).isEqualTo("runtime");
    assertThat(Context.BUILD.toString()).isEqualTo("buildtime");
  }

  private void setImageCode(@Nullable String string, Runnable runnable) {
    String property = System.getProperty("org.graalvm.nativeimage.imagecode");
    if (string != null) {
      System.setProperty("org.graalvm.nativeimage.imagecode", string);
    }
    else {
      System.clearProperty("org.graalvm.nativeimage.imagecode");
    }

    runnable.run();
    if (property != null) {
      System.setProperty("org.graalvm.nativeimage.imagecode", property);
    }
  }
}