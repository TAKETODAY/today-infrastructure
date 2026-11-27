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

package infra.aot.nativex.feature;

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.graalvm.nativeimage.hosted.Feature.DuringAnalysisAccess;
import org.graalvm.nativeimage.hosted.FieldValueTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.BiConsumer;

import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PreComputeFieldFeatureTests {

  @Test
  void transformsOnlyMatchingStaticFinalBooleanFields() throws Exception {
    var access = mock(DuringAnalysisAccess.class);

    reachabilityHandler().accept(access, DetectionFlags.class);

    var transformer = ArgumentCaptor.forClass(FieldValueTransformer.class);
    verify(access).registerFieldValueTransformer(eq(DetectionFlags.class.getDeclaredField("libraryPresent")), transformer.capture());
    assertThat(transformer.getValue().transform(null, false)).isEqualTo(true);
    verify(access).registerFieldValueTransformer(eq(DetectionFlags.class.getDeclaredField("LIBRARY_PRESENT")), transformer.capture());
    assertThat(transformer.getValue().transform(null, true)).isEqualTo(false);
    verifyNoMoreInteractions(access);
  }

  @Test
  void leavesFailedFieldInitializationForRuntime() {
    var access = mock(DuringAnalysisAccess.class);

    reachabilityHandler().accept(access, UnavailableFlags.class);

    verifyNoInteractions(access);
  }

  @Test
  @ClassPathExclusions("snakeyaml-*.jar")
  void skipsTypesWithMissingFieldDependencies() {
    var access = mock(DuringAnalysisAccess.class);
    assertThat(PreComputeFieldFeatureTests.class.getClassLoader().getResource("org/yaml/snakeyaml/Yaml.class")).isNull();

    reachabilityHandler().accept(access, OptionalDependencyFlags.class);

    verifyNoInteractions(access);
  }

  private static BiConsumer<DuringAnalysisAccess, Class<?>> reachabilityHandler() {
    var access = mock(BeforeAnalysisAccess.class);
    ArgumentCaptor<BiConsumer<DuringAnalysisAccess, Class<?>>> handler = ArgumentCaptor.captor();
    new PreComputeFieldFeature().beforeAnalysis(access);
    verify(access).registerSubtypeReachabilityHandler(handler.capture(), eq(Object.class));
    return handler.getValue();
  }

  static class DetectionFlags {

    static final boolean libraryPresent = true;

    static final Boolean LIBRARY_PRESENT = Boolean.FALSE;

    static final boolean unrelatedFlag = true;

    static final String namePresent = "ignored";

    static boolean mutablePresent = true;

    final boolean instancePresent = true;

  }

  static class UnavailableFlags {

    static final boolean libraryPresent = detectLibrary();

    private static boolean detectLibrary() {
      throw new IllegalStateException("Test library is unavailable");
    }

  }

  static class OptionalDependencyFlags {

    static final boolean yamlPresent = true;

    org.yaml.snakeyaml.Yaml yaml;

  }

}
