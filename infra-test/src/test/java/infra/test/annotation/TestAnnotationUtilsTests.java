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

package infra.test.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestAnnotationUtilsTests {

  @Test
  void missingAnnotationsUseDefaultTimeoutAndRepeatCount() throws Exception {
    var method = Samples.class.getDeclaredMethod("plain");
    assertThat(TestAnnotationUtils.getTimeout(method)).isZero();
    assertThat(TestAnnotationUtils.getRepeatCount(method)).isEqualTo(1);
  }

  @Test
  void negativeAnnotationValuesAreClampedToDefaults() throws Exception {
    var method = Samples.class.getDeclaredMethod("negative");
    assertThat(TestAnnotationUtils.getTimeout(method)).isZero();
    assertThat(TestAnnotationUtils.getRepeatCount(method)).isEqualTo(1);
    method = Samples.class.getDeclaredMethod("configured");
    assertThat(TestAnnotationUtils.getTimeout(method)).isEqualTo(200);
    assertThat(TestAnnotationUtils.getRepeatCount(method)).isEqualTo(3);
  }

  static class Samples {
    void plain() { }

    @Timed(millis = -1)
    @Repeat(-1)
    void negative() { }

    @Timed(millis = 200)
    @Repeat(3)
    void configured() { }
  }

}
