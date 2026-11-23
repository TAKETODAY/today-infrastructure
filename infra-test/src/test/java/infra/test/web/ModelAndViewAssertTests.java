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

package infra.test.web;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ModelAndViewAssertTests {

  @Test
  void attributeTypeAvailabilityAndValue() {
    ModelAndView mav = new ModelAndView("view", "name", "value");
    assertThat(ModelAndViewAssert.assertAndReturnModelAttributeOfType(mav, "name", String.class))
            .isEqualTo("value");
    ModelAndViewAssert.assertModelAttributeAvailable(mav, "name");
    ModelAndViewAssert.assertModelAttributeValue(mav, "name", "value");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertAndReturnModelAttributeOfType(mav, "missing", String.class))
            .withMessageContaining("'missing' is null");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertAndReturnModelAttributeOfType(mav, "name", Integer.class))
            .withMessageContaining("java.lang.Integer").withMessageContaining("java.lang.String");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertModelAttributeAvailable(mav, "missing"))
            .withMessageContaining("not available");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertModelAttributeValue(mav, "name", "other"))
            .withMessageContaining("other");
  }

  @Test
  void comparesListsBySizeAndOrder() {
    ModelAndView mav = new ModelAndView("view", "items", List.of("a", "b"));
    ModelAndViewAssert.assertCompareListModelAttribute(mav, "items", List.of("a", "b"));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertCompareListModelAttribute(mav, "items", List.of("a")))
            .withMessageContaining("Size of model list");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertCompareListModelAttribute(mav, "items", List.of("b", "a")))
            .withMessageContaining("not equal");
  }

  @Test
  void sortsListsWithNaturalOrderOrComparator() {
    ModelAndView mav = new ModelAndView("view", "items", new ArrayList<>(List.of("b", "a")));
    ModelAndViewAssert.assertSortAndCompareListModelAttribute(mav, "items",
            new ArrayList<>(List.of("a", "b")), null);
    ModelAndViewAssert.assertSortAndCompareListModelAttribute(mav, "items",
            new ArrayList<>(List.of("a", "b")), Comparator.reverseOrder());
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertSortAndCompareListModelAttribute(mav, "items",
                    new ArrayList<>(List.of("a")), null)).withMessageContaining("Size of model list");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertSortAndCompareListModelAttribute(mav, "items",
                    new ArrayList<>(List.of("a", "c")), null)).withMessageContaining("not equal");
  }

  @Test
  void comparesAllModelValuesAndReportsMismatches() {
    ModelAndView mav = new ModelAndView("view", "name", "value");
    ModelAndViewAssert.assertModelAttributeValues(mav, Map.of("name", "value"));
    ModelAndViewAssert.assertModelAttributeValues(new ModelAndView(), Map.of());
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertModelAttributeValues(mav, Map.of("name", "other")))
            .withMessageContaining("Values of expected model do not match")
            .withMessageContaining("'name' differs").withMessageContaining("other").withMessageContaining("value");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertModelAttributeValues(mav, Map.of("other", "value")))
            .withMessageContaining("Set has too many elements:\n-name")
            .withMessageContaining("Set is missing elements:\n-other");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertModelAttributeValues(mav, Map.of()))
            .withMessageContaining("too many elements").withMessageNotContaining("missing elements");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertModelAttributeValues(new ModelAndView(), Map.of("name", "value")))
            .withMessageContaining("missing elements").withMessageNotContaining("too many elements");
  }

  @Test
  void comparesViewNamesIncludingNull() {
    ModelAndViewAssert.assertViewName(new ModelAndView("view"), "view");
    ModelAndViewAssert.assertViewName(new ModelAndView(), null);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> ModelAndViewAssert.assertViewName(new ModelAndView("actual"), "expected"))
            .withMessageContaining("actual").withMessageContaining("expected");
  }

}
