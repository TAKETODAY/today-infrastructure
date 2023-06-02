/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.test.util.AssertionErrors;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.view.ModelAndView;

/**
 * A collection of assertions intended to simplify testing scenarios dealing
 * with Infra Web MVC {@link cn.taketoday.web.view.ModelAndView
 * ModelAndView} objects.
 *
 * <p>Intended for use with JUnit 4 and TestNG. All {@code assert*()} methods
 * throw {@link AssertionError AssertionErrors}.
 *
 * @author Sam Brannen
 * @author Alef Arendsen
 * @author Bram Smeets
 * @see cn.taketoday.web.view.ModelAndView
 * @since 4.0
 */
public abstract class ModelAndViewAssert {

  /**
   * Checks whether the model value under the given {@code modelName}
   * exists and checks it type, based on the {@code expectedType}. If the
   * model entry exists and the type matches, the model value is returned.
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param modelName name of the object to add to the model (never {@code null})
   * @param expectedType expected type of the model value
   * @return the model value
   */
  @SuppressWarnings("unchecked")
  public static <T> T assertAndReturnModelAttributeOfType(ModelAndView mav, String modelName, Class<T> expectedType) {
    Map<String, Object> model = mav.getModel();
    Object obj = model.get(modelName);
    if (obj == null) {
      AssertionErrors.fail("Model attribute with name '" + modelName + "' is null");
    }
    AssertionErrors.assertTrue("Model attribute is not of expected type '" + expectedType.getName() + "' but rather of type '" +
            obj.getClass().getName() + "'", expectedType.isAssignableFrom(obj.getClass()));
    return (T) obj;
  }

  /**
   * Compare each individual entry in a list, without first sorting the lists.
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param modelName name of the object to add to the model (never {@code null})
   * @param expectedList the expected list
   */
  @SuppressWarnings("rawtypes")
  public static void assertCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList) {
    List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
    AssertionErrors.assertTrue("Size of model list is '" + modelList.size() + "' while size of expected list is '" +
            expectedList.size() + "'", expectedList.size() == modelList.size());
    AssertionErrors.assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
            expectedList.equals(modelList));
  }

  /**
   * Assert whether or not a model attribute is available.
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param modelName name of the object to add to the model (never {@code null})
   */
  public static void assertModelAttributeAvailable(ModelAndView mav, String modelName) {
    Map<String, Object> model = mav.getModel();
    AssertionErrors.assertTrue("Model attribute with name '" + modelName + "' is not available", model.containsKey(modelName));
  }

  /**
   * Compare a given {@code expectedValue} to the value from the model
   * bound under the given {@code modelName}.
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param modelName name of the object to add to the model (never {@code null})
   * @param expectedValue the model value
   */
  public static void assertModelAttributeValue(ModelAndView mav, String modelName, Object expectedValue) {
    Object modelValue = assertAndReturnModelAttributeOfType(mav, modelName, Object.class);
    AssertionErrors.assertTrue("Model value with name '" + modelName + "' is not the same as the expected value which was '" +
            expectedValue + "'", modelValue.equals(expectedValue));
  }

  /**
   * Inspect the {@code expectedModel} to see if all elements in the
   * model appear and are equal.
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param expectedModel the expected model
   */
  public static void assertModelAttributeValues(ModelAndView mav, Map<String, Object> expectedModel) {
    Map<String, Object> model = mav.getModel();

    if (!model.keySet().equals(expectedModel.keySet())) {
      StringBuilder sb = new StringBuilder("Keyset of expected model does not match.\n");
      appendNonMatchingSetsErrorMessage(expectedModel.keySet(), model.keySet(), sb);
      AssertionErrors.fail(sb.toString());
    }

    StringBuilder sb = new StringBuilder();
    model.forEach((modelName, mavValue) -> {
      Object assertionValue = expectedModel.get(modelName);
      if (!assertionValue.equals(mavValue)) {
        sb.append("Value under name '").append(modelName).append("' differs, should have been '").append(
                assertionValue).append("' but was '").append(mavValue).append("'\n");
      }
    });

    if (sb.length() != 0) {
      sb.insert(0, "Values of expected model do not match.\n");
      AssertionErrors.fail(sb.toString());
    }
  }

  /**
   * Compare each individual entry in a list after having sorted both lists
   * (optionally using a comparator).
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param modelName name of the object to add to the model (never {@code null})
   * @param expectedList the expected list
   * @param comparator the comparator to use (may be {@code null}). If not
   * specifying the comparator, both lists will be sorted not using any comparator.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void assertSortAndCompareListModelAttribute(
          ModelAndView mav, String modelName, List expectedList, Comparator comparator) {

    List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
    AssertionErrors.assertTrue("Size of model list is '" + modelList.size() + "' while size of expected list is '" +
            expectedList.size() + "'", expectedList.size() == modelList.size());

    modelList.sort(comparator);
    expectedList.sort(comparator);

    AssertionErrors.assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
            expectedList.equals(modelList));
  }

  /**
   * Check to see if the view name in the ModelAndView matches the given
   * {@code expectedName}.
   *
   * @param mav the ModelAndView to test against (never {@code null})
   * @param expectedName the name of the model value
   */
  public static void assertViewName(ModelAndView mav, String expectedName) {
    AssertionErrors.assertTrue("View name is not equal to '" + expectedName + "' but was '" + mav.getViewName() + "'",
            ObjectUtils.nullSafeEquals(expectedName, mav.getViewName()));
  }

  private static void appendNonMatchingSetsErrorMessage(
          Set<String> assertionSet, Set<String> incorrectSet, StringBuilder sb) {

    Set<String> tempSet = new HashSet<>(incorrectSet);
    tempSet.removeAll(assertionSet);

    if (!tempSet.isEmpty()) {
      sb.append("Set has too many elements:\n");
      for (Object element : tempSet) {
        sb.append('-');
        sb.append(element);
        sb.append('\n');
      }
    }

    tempSet = new HashSet<>(assertionSet);
    tempSet.removeAll(incorrectSet);

    if (!tempSet.isEmpty()) {
      sb.append("Set is missing elements:\n");
      for (Object element : tempSet) {
        sb.append('-');
        sb.append(element);
        sb.append('\n');
      }
    }
  }

}
