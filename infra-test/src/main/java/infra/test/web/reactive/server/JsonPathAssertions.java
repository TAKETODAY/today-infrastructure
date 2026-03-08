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

package infra.test.web.reactive.server;

import com.jayway.jsonpath.Configuration;

import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import infra.core.ParameterizedTypeReference;
import infra.test.util.JsonPathExpectationsHelper;
import infra.test.web.support.AbstractJsonPathAssertions;

/**
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions.
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 * @since 4.0
 */
public class JsonPathAssertions extends AbstractJsonPathAssertions<WebTestClient.BodyContentSpec> {

  JsonPathAssertions(WebTestClient.BodyContentSpec spec, String content, String expression,
          @Nullable Configuration configuration) {

    super(spec, content, expression, configuration);
  }

  /**
   * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher)}.
   */
  public <T> WebTestClient.BodyContentSpec value(Matcher<? super T> matcher) {
    getPathHelper().assertValue(getContent(), matcher);
    return getBodySpec();
  }

  /**
   * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, Class)}.
   */
  public <T> WebTestClient.BodyContentSpec value(Class<T> targetType, Matcher<? super T> matcher) {
    getPathHelper().assertValue(getContent(), matcher, targetType);
    return getBodySpec();
  }

  /**
   * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, ParameterizedTypeReference)}.
   */
  public <T> WebTestClient.BodyContentSpec value(ParameterizedTypeReference<T> targetType, Matcher<? super T> matcher) {
    getPathHelper().assertValue(getContent(), matcher, targetType);
    return getBodySpec();
  }

}
