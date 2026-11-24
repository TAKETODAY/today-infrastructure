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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class GeneratedMapUtilsTests {

  @Test
  @SuppressWarnings("unchecked")
  void generatedMapIsUnmodifiable() {
    Map<String, String> map = GeneratedMapUtils.loadMap(Mappings.class.getName(), "values");
    assertThat(map).containsEntry("key", "value");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("other", "value"));
  }

  @Test
  void missingMethodAndClassAreReported() {
    assertThatIllegalStateException().isThrownBy(() -> GeneratedMapUtils.loadMap(Mappings.class.getName(), "missing"))
            .withMessageContaining("No missing() method found");
    assertThatIllegalStateException().isThrownBy(() -> GeneratedMapUtils.loadMap("missing.GeneratedMappings", "values"))
            .withMessageContaining("Failed to invoke values()").withCauseInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void invocationFailurePreservesIllegalStateAndWrapsOtherExceptions() {
    assertThatIllegalStateException().isThrownBy(() -> GeneratedMapUtils.loadMap(Mappings.class.getName(), "invalidState"))
            .withMessage("invalid state");
    assertThatIllegalStateException().isThrownBy(() -> GeneratedMapUtils.loadMap(Mappings.class.getName(), "invalidValue"))
            .withMessageContaining("Failed to invoke invalidValue()").withCauseInstanceOf(IllegalArgumentException.class);
  }

  public static class Mappings {

    public static Map<String, String> values() {
      return Map.of("key", "value");
    }

    public static Map<String, String> invalidState() {
      throw new IllegalStateException("invalid state");
    }

    public static Map<String, String> invalidValue() {
      throw new IllegalArgumentException("invalid value");
    }

  }

}
