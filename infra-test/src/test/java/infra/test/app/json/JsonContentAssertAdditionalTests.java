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

package infra.test.app.json;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JsonContentAssertAdditionalTests {

  @Test
  void duplicateJsonCustomizersRejectNullAndUnrelatedTypes() {
    var factory = new DuplicateJsonObjectContextCustomizerFactory();
    var customizer = factory.createContextCustomizer(getClass(), List.of());
    org.assertj.core.api.Assertions.assertThat(customizer).isNotEqualTo(null).isNotEqualTo(new Object())
            .isEqualTo(factory.createContextCustomizer(getClass(), List.of()))
            .hasSameHashCodeAs(factory.createContextCustomizer(getClass(), List.of()));
  }

  @Test
  void nullAndUnsupportedEqualityArguments() {
    JsonContentAssert empty = json(null);
    empty.isEqualTo(null);
    empty.isEqualToJson((CharSequence) null, new DefaultComparator(JSONCompareMode.LENIENT));
    empty.isNotEqualToJson("{}", new DefaultComparator(JSONCompareMode.LENIENT));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> empty.isNotEqualTo(null));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> json("{}").isEqualTo(42))
            .withMessageContaining("Unsupported type");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> json("{}").isNotEqualTo(42))
            .withMessageContaining("Unsupported type");
  }

  @Test
  void pathExistenceDistinguishesNullMissingAndPresentValues() {
    JsonContentAssert actual = json("{\"nil\":null,\"value\":42}");
    actual.hasJsonPath("$.nil").doesNotHaveJsonPath("$.missing");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.hasJsonPath("$.missing"))
            .withMessageContaining("No JSON path");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.doesNotHaveJsonPath("$.nil"))
            .withMessageContaining("Expecting no JSON path");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.doesNotHaveJsonPath("$.value"));
    assertThatIllegalArgumentException().isThrownBy(() -> actual.hasJsonPath(null));
    assertThatIllegalArgumentException().isThrownBy(() -> actual.hasJsonPath(" "));
  }

  @Test
  void valueAssertionsReportWrongMissingAndNonemptyValues() {
    JsonContentAssert actual = json("{\"nil\":null,\"empty\":[],\"values\":[1],\"text\":\"value\"}");
    actual.hasEmptyJsonPathValue("$.empty").doesNotHaveEmptyJsonPathValue("$.text");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.hasEmptyJsonPathValue("$.text"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.doesNotHaveEmptyJsonPathValue("$.empty"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.hasJsonPathValue("$.nil"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.hasJsonPathValue("$.missing"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.hasJsonPathValue("$.values[?(@ > 10)]"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.hasJsonPathStringValue("$.values"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> actual.doesNotHaveJsonPathValue("$.text"));
    actual.doesNotHaveJsonPathValue("$.values[?(@ > 10)]");
    json(null).extractingJsonPathStringValue("$.text").isNull();
  }

  private JsonContentAssert json(String value) {
    return new JsonContentAssert(getClass(), value);
  }

  @Test
  void resourceEqualityArgumentsAreDispatchedByObjectAssertions() {
    Object equal = new ByteArrayResource("{}".getBytes(StandardCharsets.UTF_8));
    Object different = new ByteArrayResource("{\"value\":1}".getBytes(StandardCharsets.UTF_8));
    json("{}").isEqualTo(equal).isNotEqualTo(different);
  }

  @Test
  void malformedJsonAndComparatorFailuresRetainTheirCauses() {
    DefaultComparator comparator = new DefaultComparator(JSONCompareMode.LENIENT);
    assertThatIllegalStateException().isThrownBy(() -> json("{").isEqualToJson("{}"))
            .withCauseInstanceOf(org.json.JSONException.class);
    assertThatIllegalStateException().isThrownBy(() -> json("{").isEqualToJson("{}", comparator))
            .withCauseInstanceOf(org.json.JSONException.class);
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> json("{}").isEqualToJson((CharSequence) null));
    assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> json("{}").isEqualToJson((CharSequence) null, comparator));
  }

  @Test
  void indefinitePathsDistinguishEmptyAndNonemptySelections() {
    JsonContentAssert actual = json("{\"values\":[1,2]}");
    actual.hasJsonPathValue("$.values[?(@ > 0)]");
    actual.hasEmptyJsonPathValue("$.values[?(@ > 5)]");
    actual.doesNotHaveJsonPathValue("$.missing");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> actual.hasEmptyJsonPathValue("$.values[?(@ > 0)]"));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> actual.doesNotHaveJsonPathValue("$.values[?(@ > 0)]"));
  }

  @Test
  void failedEqualityAndInequalityIncludeTheJsonDifference() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> json("{\"value\":1}").isEqualToJson("{\"value\":2}"))
            .withMessageContaining("value");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> json("{}").isNotEqualToJson("{}"))
            .withMessageContaining("JSON Comparison failure");
  }

  @Test
  void emptyAndTypedPathsIncludeNullMissingAndNonemptySelections() {
    JsonContentAssert actual = json("{\"nil\":null,\"text\":\"hello\",\"values\":[1,2]}");
    actual.hasEmptyJsonPathValue("$.nil").hasEmptyJsonPathValue("$.missing");
    actual.doesNotHaveEmptyJsonPathValue("$.values[?(@ > 0)]");
    actual.extractingJsonPathValue("$.missing").isNull();
    actual.extractingJsonPathStringValue("$.nil").isNull();
    actual.extractingJsonPathNumberValue("$.missing").isNull();
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> actual.doesNotHaveEmptyJsonPathValue("$.nil"));
    actual.hasJsonPath("$.values[?(@ > 5)]");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> actual.doesNotHaveJsonPath("$.values[?(@ > 0)]"));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> actual.doesNotHaveJsonPath("$.values[?(@ > 5)]"));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> actual.extractingJsonPathNumberValue("$.text"));
  }

}
