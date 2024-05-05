/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.mock.result;

import org.hamcrest.Matcher;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.mock.MvcResult;
import cn.taketoday.test.web.mock.ResultMatcher;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.FieldError;
import cn.taketoday.web.view.ModelAndView;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertFalse;
import static cn.taketoday.test.util.AssertionErrors.assertNotNull;
import static cn.taketoday.test.util.AssertionErrors.assertNull;
import static cn.taketoday.test.util.AssertionErrors.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for assertions on the model.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#model}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class ModelResultMatchers {

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#model()}.
   */
  protected ModelResultMatchers() {
  }

  /**
   * Assert a model attribute value with the given Hamcrest {@link Matcher}.
   */
  @SuppressWarnings("unchecked")
  public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      assertThat("Model attribute '" + name + "'", (T) mav.getModel().get(name), matcher);
    };
  }

  /**
   * Assert a model attribute value.
   */
  public ResultMatcher attribute(String name, @Nullable Object value) {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      assertEquals("Model attribute '" + name + "'", value, mav.getModel().get(name));
    };
  }

  /**
   * Assert the given model attributes exist.
   */
  public ResultMatcher attributeExists(String... names) {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      for (String name : names) {
        assertNotNull("Model attribute '" + name + "' does not exist", mav.getModel().get(name));
      }
    };
  }

  /**
   * Assert the given model attributes do not exist.
   */
  public ResultMatcher attributeDoesNotExist(String... names) {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      for (String name : names) {
        assertNull("Model attribute '" + name + "' exists", mav.getModel().get(name));
      }
    };
  }

  /**
   * Assert the given model attribute(s) have errors.
   */
  public ResultMatcher attributeErrorCount(String name, int expectedCount) {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      Errors errors = getBindingResult(mav, name);
      assertEquals("Binding/validation error count for attribute '" + name + "',",
              expectedCount, errors.getErrorCount());
    };
  }

  /**
   * Assert the given model attribute(s) have errors.
   */
  public ResultMatcher attributeHasErrors(String... names) {
    return mvcResult -> {
      ModelAndView mav = getModelAndView(mvcResult);
      for (String name : names) {
        BindingResult result = getBindingResult(mav, name);
        assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
      }
    };
  }

  /**
   * Assert the given model attribute(s) do not have errors.
   */
  public ResultMatcher attributeHasNoErrors(String... names) {
    return mvcResult -> {
      ModelAndView mav = getModelAndView(mvcResult);
      for (String name : names) {
        BindingResult result = getBindingResult(mav, name);
        assertFalse("Unexpected errors for attribute '" + name + "': " + result.getAllErrors(),
                result.hasErrors());
      }
    };
  }

  /**
   * Assert the given model attribute field(s) have errors.
   */
  public ResultMatcher attributeHasFieldErrors(String name, String... fieldNames) {
    return mvcResult -> {
      ModelAndView mav = getModelAndView(mvcResult);
      BindingResult result = getBindingResult(mav, name);
      assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
      for (String fieldName : fieldNames) {
        boolean hasFieldErrors = result.hasFieldErrors(fieldName);
        assertTrue("No errors for field '" + fieldName + "' of attribute '" + name + "'", hasFieldErrors);
      }
    };
  }

  /**
   * Assert a field error code for a model attribute using exact String match.
   */
  public ResultMatcher attributeHasFieldErrorCode(String name, String fieldName, String error) {
    return mvcResult -> {
      ModelAndView mav = getModelAndView(mvcResult);
      BindingResult result = getBindingResult(mav, name);
      assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
      FieldError fieldError = result.getFieldError(fieldName);
      assertNotNull("No errors for field '" + fieldName + "' of attribute '" + name + "'", fieldError);
      String code = fieldError.getCode();
      assertEquals("Field error code", error, code);
    };
  }

  /**
   * Assert a field error code for a model attribute using a {@link Matcher}.
   */
  public ResultMatcher attributeHasFieldErrorCode(String name, String fieldName,
          Matcher<? super String> matcher) {

    return mvcResult -> {
      ModelAndView mav = getModelAndView(mvcResult);
      BindingResult result = getBindingResult(mav, name);
      assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
      FieldError fieldError = result.getFieldError(fieldName);
      assertNotNull("No errors for field '" + fieldName + "' of attribute '" + name + "'", fieldError);
      String code = fieldError.getCode();
      assertThat("Field name '" + fieldName + "' of attribute '" + name + "'", code, matcher);
    };
  }

  /**
   * Assert the total number of errors in the model.
   */
  public ResultMatcher errorCount(int expectedCount) {
    return result -> {
      int actualCount = getErrorCount(getModelAndView(result).getModelMap());
      assertEquals("Binding/validation error count", expectedCount, actualCount);
    };
  }

  /**
   * Assert the model has errors.
   */
  public ResultMatcher hasErrors() {
    return result -> {
      int count = getErrorCount(getModelAndView(result).getModelMap());
      assertTrue("Expected binding/validation errors", count != 0);
    };
  }

  /**
   * Assert the model has no errors.
   */
  public ResultMatcher hasNoErrors() {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      for (Object value : mav.getModel().values()) {
        if (value instanceof Errors errors) {
          assertFalse("Unexpected binding/validation errors: " + value, errors.hasErrors());
        }
      }
    };
  }

  /**
   * Assert the number of model attributes.
   */
  public ResultMatcher size(int size) {
    return result -> {
      ModelAndView mav = getModelAndView(result);
      int actual = 0;
      for (String key : mav.getModel().keySet()) {
        if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
          actual++;
        }
      }
      assertEquals("Model size", size, actual);
    };
  }

  private ModelAndView getModelAndView(MvcResult mvcResult) {
    ModelAndView mav = mvcResult.getModelAndView();
    assertNotNull("No ModelAndView found", mav);
    return mav;
  }

  private BindingResult getBindingResult(ModelAndView mav, String name) {
    BindingResult result = (BindingResult) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
    assertNotNull("No BindingResult for attribute: " + name, result);
    return result;
  }

  private int getErrorCount(ModelMap model) {
    int count = 0;
    for (Object value : model.values()) {
      if (value instanceof Errors errors) {
        count += errors.getErrorCount();
      }
    }
    return count;
  }

}
