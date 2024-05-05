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
import cn.taketoday.test.web.mock.ResultMatcher;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for "output" flash attribute assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#flash}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class FlashAttributeResultMatchers {

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#flash()}.
   */
  protected FlashAttributeResultMatchers() {
  }

  /**
   * Assert a flash attribute's value with the given Hamcrest {@link Matcher}.
   */
  @SuppressWarnings("unchecked")
  public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
    return result -> assertThat("Flash attribute '" + name + "'", (T) result.getFlashMap().get(name), matcher);
  }

  /**
   * Assert a flash attribute's value.
   */
  public ResultMatcher attribute(String name, @Nullable Object value) {
    return result -> assertEquals("Flash attribute '" + name + "'", value, result.getFlashMap().get(name));
  }

  /**
   * Assert the existence of the given flash attributes.
   */
  public ResultMatcher attributeExists(String... names) {
    return result -> {
      for (String name : names) {
        assertNotNull("Flash attribute '" + name + "' does not exist", result.getFlashMap().get(name));
      }
    };
  }

  /**
   * Assert the number of flash attributes.
   */
  public ResultMatcher attributeCount(int count) {
    return result -> assertEquals("RedirectModel size", count, result.getFlashMap().size());
  }

}
