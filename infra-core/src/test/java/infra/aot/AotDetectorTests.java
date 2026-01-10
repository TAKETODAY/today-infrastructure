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

package infra.aot;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import infra.core.NativeDetector;
import infra.core.NativeDetector.Context;
import infra.lang.TodayStrategies;

import static infra.core.NativeDetector.Context.RUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/18 17:52
 */
class AotDetectorTests {

  @Test
  void useGeneratedArtifactsReturnsFalseWhenNotInNativeImageAndPropertyNotSet() throws Exception {
    try (MockedStatic<NativeDetector> mockedNativeDetector = mockStatic(NativeDetector.class);
            MockedStatic<TodayStrategies> mockedTodayStrategies = mockStatic(TodayStrategies.class)) {

      mockedNativeDetector.when(() -> NativeDetector.inNativeImage(RUN, Context.BUILD)).thenReturn(false);
      mockedTodayStrategies.when(() -> TodayStrategies.getFlag(AotDetector.AOT_ENABLED)).thenReturn(false);

      assertThat(AotDetector.useGeneratedArtifacts()).isFalse();
    }
  }

  @Test
  void useGeneratedArtifactsReturnsTrueWhenPropertyIsSet() throws Exception {
    try (MockedStatic<NativeDetector> mockedNativeDetector = mockStatic(NativeDetector.class);
            MockedStatic<TodayStrategies> mockedTodayStrategies = mockStatic(TodayStrategies.class)) {

      mockedNativeDetector.when(() -> NativeDetector.inNativeImage(Context.RUN, Context.BUILD)).thenReturn(false);
      mockedTodayStrategies.when(() -> TodayStrategies.getFlag(AotDetector.AOT_ENABLED)).thenReturn(true);

      assertThat(AotDetector.useGeneratedArtifacts()).isTrue();
    }
  }

}