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

package infra.core;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.core.NativeDetector.Context;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/18 22:00
 */
class NativeDetectorTests {

  @Disabled
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