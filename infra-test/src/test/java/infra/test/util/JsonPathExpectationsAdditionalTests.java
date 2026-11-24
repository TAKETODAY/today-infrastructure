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

package infra.test.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JsonPathExpectationsAdditionalTests {

  @Test
  void scalarComparisonHandlesNullsListsAndFailedConversions() {
    var helper = new JsonPathExpectationsHelper("$.value");
    helper.assertValue("{\"value\":null}", (Object) null);
    helper.assertValue("{\"value\":[1]}", 1);
    helper.assertValue("{\"value\":[1,2]}", helper.evaluateJsonPath("{\"value\":[1,2]}"));
    helper.assertValue("{\"value\":1}", 1.0);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> helper.assertValue("{\"value\":[]}", 1))
            .withMessageContaining("No matching value");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> helper.assertValue("{\"value\":[1,2]}", 1))
            .withMessageContaining("Got a list of values");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> helper.assertValue("{\"value\":\"text\"}", 1))
            .withMessageContaining("cannot be converted");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> helper.assertValue("{\"value\":1}", (Object) null));
  }

  @Test
  void indefinitePathsWithScalarResultDoNotBehaveAsLists() {
    var helper = new JsonPathExpectationsHelper("$[*].value.length()", Configuration.defaultConfiguration());
    helper.hasJsonPath("[{\"value\":[]},{\"value\":[1]}]");
    var suppressed = new JsonPathExpectationsHelper("$[?(@.missing)].value",
            Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build());
    suppressed.doesNotExist("[]");
    suppressed.doesNotHaveJsonPath("[]");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> suppressed.exists("[]"));
  }

  @Test
  void missingPathAssertionsIgnoreEvaluationFailures() {
    var helper = new JsonPathExpectationsHelper("$.value");
    helper.doesNotExist("not valid json");
    helper.doesNotHaveJsonPath("not valid json");
  }

  @Test
  void existenceAssertionsRejectNullAndEmptyIndefiniteResults() {
    var nullValue = new JsonPathExpectationsHelper("$.value");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> nullValue.exists("{\"value\":null}"))
            .withMessageContaining("No value at JSON path");

    var emptyResults = new JsonPathExpectationsHelper("$[*].missing");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> emptyResults.hasJsonPath("[{}]"))
            .withMessageContaining("No values for JSON path");
    emptyResults.doesNotHaveJsonPath("[{}]");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> emptyResults.doesNotHaveJsonPath("[{\"missing\":1}]")
    )
            .withMessageContaining("Expected no values at JSON path");
  }

}
