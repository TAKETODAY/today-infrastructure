/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
