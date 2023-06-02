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

package cn.taketoday.test.web.servlet.result;

import org.hamcrest.Matcher;

import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.web.view.ModelAndView;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.fail;
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
