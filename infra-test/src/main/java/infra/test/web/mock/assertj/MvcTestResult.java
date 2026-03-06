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

import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.MvcResult;

/**
 * Provides the result of an executed request using {@link infra.test.web.mock.assertj.MockMvcTester} that
 * is meant to be used with {@link org.assertj.core.api.Assertions#assertThat(AssertProvider)
 * assertThat}.
 *
 * <p>Can be in one of two distinct states:
 * <ol>
 * <li>The request processed successfully, even if it failed with an exception
 * that has been resolved. The {@linkplain #getMvcResult() result} is available,
 * and {@link #getUnresolvedException()} will return {@code null}.</li>
 * <li>The request failed unexpectedly. {@link #getUnresolvedException()}
 * provides more information about the error, and any attempt to access the
 * {@linkplain #getMvcResult() result} will fail with an exception.</li>
 * </ol>
 *
 * <p>If the request was asynchronous, it is fully resolved at this point and
 * regular assertions can be applied without having to wait for the completion
 * of the response.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see infra.test.web.mock.assertj.MockMvcTester
 * @since 5.0
 */
public interface MvcTestResult extends AssertProvider<MvcTestResultAssert> {

  /**
   * Return the {@linkplain MvcResult result} of the processing. If
   * the processing has failed with an unresolved exception, the
   * result is not available, see {@link #getUnresolvedException()}.
   *
   * @return the {@link MvcResult}
   * @throws IllegalStateException if the processing has failed with
   * an unresolved exception
   */
  MvcResult getMvcResult();

  /**
   * Return the performed {@linkplain  HttpMockRequestImpl request}.
   */
  default HttpMockRequestImpl getRequest() {
    return getMvcResult().getRequest();
  }

  /**
   * Return the resulting {@linkplain  MockHttpResponseImpl response}.
   */
  default MockHttpResponseImpl getResponse() {
    return getMvcResult().getResponse();
  }

  /**
   * Return the exception that was thrown unexpectedly while processing the
   * request, if any.
   */
  @Nullable
  Throwable getUnresolvedException();

  @Nullable
  Throwable getResolvedException();

}
