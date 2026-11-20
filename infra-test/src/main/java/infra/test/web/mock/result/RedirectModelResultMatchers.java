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
import org.jspecify.annotations.Nullable;

import infra.test.web.mock.ResultMatcher;

import static infra.test.util.AssertionErrors.assertEquals;
import static infra.test.util.AssertionErrors.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for output redirect model assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#redirectModel()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RedirectModelResultMatchers {

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#redirectModel()}.
   */
  protected RedirectModelResultMatchers() {
  }

  /**
   * Assert a redirect model attribute's value with the given Hamcrest {@link Matcher}.
   */
  @SuppressWarnings("unchecked")
  public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
    return result -> assertThat("Redirect model attribute '" + name + "'", (T) result.getOutputRedirectModel().get(name), matcher);
  }

  /**
   * Assert a redirect model attribute's value.
   */
  public ResultMatcher attribute(String name, @Nullable Object value) {
    return result -> assertEquals("Redirect model attribute '" + name + "'", value, result.getOutputRedirectModel().get(name));
  }

  /**
   * Assert the existence of the given redirect model attributes.
   */
  public ResultMatcher attributeExists(String... names) {
    return result -> {
      for (String name : names) {
        assertNotNull("Redirect model attribute '" + name + "' does not exist", result.getOutputRedirectModel().get(name));
      }
    };
  }

  /**
   * Assert the number of redirect model attributes.
   */
  public ResultMatcher attributeCount(int count) {
    return result -> assertEquals("RedirectModel size", count, result.getOutputRedirectModel().size());
  }

}
