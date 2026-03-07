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

package infra.test.web.mock.assertj;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpSession;
import infra.mock.web.HttpMockRequestImpl;
import infra.session.Session;
import infra.util.function.SingletonSupplier;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.async.DeferredResult;

/**
 * Base AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be
 * applied to an {@link HttpMockRequest}.
 *
 * @param <SELF> the type of assertions
 * @param <ACTUAL> the type of the object to assert
 * @author Stephane Nicoll
 * @since 5.0
 */
public abstract class AbstractHttpMockRequestAssert<SELF extends AbstractHttpMockRequestAssert<SELF, ACTUAL>, ACTUAL extends HttpMockRequest>
        extends AbstractObjectAssert<SELF, ACTUAL> {

  private final Supplier<MapAssert<String, Object>> attributesAssertProvider;

  private final Supplier<MapAssert<String, Object>> sessionAttributesAssertProvider;

  protected AbstractHttpMockRequestAssert(ACTUAL actual, Class<?> selfType) {
    super(actual, selfType);
    this.attributesAssertProvider = SingletonSupplier.of(() -> createAttributesAssert(actual));
    this.sessionAttributesAssertProvider = SingletonSupplier.of(() -> createSessionAttributesAssert(actual));
  }

  private static MapAssert<String, Object> createAttributesAssert(HttpMockRequest request) {
    Map<String, Object> map = toMap(request.getAttributeNames().asIterator(), request::getAttribute);
    return Assertions.assertThat(map).as("Request Attributes");
  }

  protected MapAssert<String, Object> createSessionAttributesAssert(HttpMockRequest request) {
    HttpSession httpSession = request.getSession();
    Assertions.assertThat(httpSession).as("HTTP session").isNotNull();
    Map<String, Object> map = toMap(httpSession.getAttributeNames().asIterator(), httpSession::getAttribute);
    RequestContext requestContext = getRequestContext();
    if (requestContext != null) {
      Session session = RequestContextUtils.getSession(requestContext);
      if (session != null) {
        map.putAll(session.getAttributes());
      }
    }
    return Assertions.assertThat(map).as("Session Attributes");
  }

  protected @Nullable RequestContext getRequestContext() {
    return null;
  }

  /**
   * Return a new {@linkplain MapAssert assertion} object that uses the request
   * attributes as the object to test, with values mapped by attribute name.
   * <p>Example: <pre><code class='java'>
   * // Check for the presence of a request attribute named "attributeName":
   * assertThat(request).attributes().containsKey("attributeName");
   * </code></pre>
   */
  public MapAssert<String, Object> attributes() {
    return this.attributesAssertProvider.get();
  }

  /**
   * Return a new {@linkplain MapAssert assertion} object that uses the session
   * attributes as the object to test, with values mapped by attribute name.
   * <p>Example: <pre><code class='java'>
   * // Check for the presence of a session attribute named "username":
   * assertThat(request).sessionAttributes().containsKey("username");
   * </code></pre>
   */
  public MapAssert<String, Object> sessionAttributes() {
    return this.sessionAttributesAssertProvider.get();
  }

  /**
   * Verify whether asynchronous processing has started, usually as a result
   * of a controller method returning a {@link Callable} or {@link DeferredResult}.
   * <p>The test will await the completion of a {@code Callable} so that
   * the asynchronous result is available and can be further asserted.
   * <p>Neither a {@code Callable} nor a {@code DeferredResult} will complete
   * processing all the way since a {@link HttpMockRequestImpl} does not
   * perform asynchronous dispatches.
   *
   * @param started whether asynchronous processing should have started
   */
  public SELF hasAsyncStarted(boolean started) {
    Assertions.assertThat(this.actual.isAsyncStarted())
            .withFailMessage("Async expected %s have started", (started ? "to" : "not to"))
            .isEqualTo(started);
    return this.myself;
  }

  private static Map<String, Object> toMap(Iterator<String> keys, Function<String, Object> valueProvider) {
    Map<String, Object> map = new LinkedHashMap<>();
    while (keys.hasNext()) {
      String key = keys.next();
      map.put(key, valueProvider.apply(key));
    }
    return map;
  }

}
