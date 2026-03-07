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

import org.jspecify.annotations.Nullable;

import infra.mock.web.HttpMockRequestImpl;
import infra.test.web.mock.MvcResult;
import infra.web.RequestContext;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to {@link HttpMockRequestImpl}.
 *
 * @param <SELF> the type of assertions
 * @author Stephane Nicoll
 * @since 5.0
 */
public abstract class AbstractMockHttpMockRequestAssert<SELF extends AbstractMockHttpMockRequestAssert<SELF>>
        extends AbstractHttpMockRequestAssert<SELF, HttpMockRequestImpl> {

  private final @Nullable MvcResult result;

  protected AbstractMockHttpMockRequestAssert(HttpMockRequestImpl result, Class<?> selfType) {
    super(result, selfType);
    this.result = null;
  }

  protected AbstractMockHttpMockRequestAssert(MvcResult result, Class<?> selfType) {
    super(result.getRequest(), selfType);
    this.result = result;
  }

  @Override
  protected @Nullable RequestContext getRequestContext() {
    return result != null ? result.getRequestContext() : null;
  }

}
