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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AssertionErrorsTests {

  @Test
  void successfulAssertionsHandleNullAndArrayValues() {
    assertThatNoException().isThrownBy(() -> {
      AssertionErrors.assertTrue("true", true);
      AssertionErrors.assertFalse("false", false);
      AssertionErrors.assertNull("null", null);
      AssertionErrors.assertNotNull("present", "value");
      AssertionErrors.assertEquals("nulls", null, null);
      AssertionErrors.assertEquals("arrays", new int[] { 1, 2 }, new int[] { 1, 2 });
      AssertionErrors.assertNotEquals("different", null, "value");
    });
  }

  @Test
  void failuresPreserveDescriptionsAndFormatValues() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> AssertionErrors.fail("failed"))
            .withMessage("failed");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> AssertionErrors.assertFalse("false", true))
            .withMessage("false");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> AssertionErrors.assertNull("null", "value"))
            .withMessage("null");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> AssertionErrors.assertNotNull("present", null))
            .withMessage("present");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> AssertionErrors.assertEquals("arrays", new int[] { 1 }, new int[] { 2 }))
            .withMessage("arrays expected:<{1}> but was:<{2}>");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> AssertionErrors.assertNotEquals("same", new int[] { 1 }, new int[] { 1 }))
            .withMessage("same was not expected to be:<{1}>");
  }

  @Test
  void jsonHelperDistinguishesLenientAndStrictComparisons() throws Exception {
    JsonExpectationsHelper helper = new JsonExpectationsHelper();
    helper.assertJsonEqual("{\"items\":[1,2]}", "{\"extra\":true,\"items\":[2,1]}");
    helper.assertJsonNotEqual("{\"value\":1}", "{\"value\":2}");
    helper.assertJsonNotEqual("{\"items\":[1,2]}", "{\"items\":[2,1]}", true);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> helper.assertJsonEqual("{\"items\":[1,2]}", "{\"items\":[2,1]}", true));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> helper.assertJsonNotEqual("{}", "{}"));
  }

}
