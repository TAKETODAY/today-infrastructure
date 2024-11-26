/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.web.mock.result;

import org.hamcrest.Matcher;

import infra.test.web.mock.ResultMatcher;
import infra.web.view.ModelAndView;

import static infra.test.util.AssertionErrors.assertEquals;
import static infra.test.util.AssertionErrors.fail;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for assertions on the selected view.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#view}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ViewResultMatchers {

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#view()}.
   */
  protected ViewResultMatchers() {
  }

  /**
   * Assert the selected view name with the given Hamcrest {@link Matcher}.
   */
  public ResultMatcher name(Matcher<? super String> matcher) {
    return result -> {
      ModelAndView mav = result.getModelAndView();
      if (mav == null) {
        fail("No ModelAndView found");
      }
      assertThat("View name", mav.getViewName(), matcher);
    };
  }

  /**
   * Assert the selected view name.
   */
  public ResultMatcher name(String expectedViewName) {
    return result -> {
      ModelAndView mav = result.getModelAndView();
      if (mav == null) {
        fail("No ModelAndView found");
      }
      assertEquals("View name", expectedViewName, mav.getViewName());
    };
  }

}
