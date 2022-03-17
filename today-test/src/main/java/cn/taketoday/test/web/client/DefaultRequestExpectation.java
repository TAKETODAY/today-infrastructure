/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.web.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@code RequestExpectation} that simply delegates
 * to the request matchers and the response creator it contains.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultRequestExpectation implements RequestExpectation {

  private final RequestCount requestCount;

  private final List<RequestMatcher> requestMatchers = new ArrayList<>(1);

  @Nullable
  private ResponseCreator responseCreator;

  /**
   * Create a new request expectation that should be called a number of times
   * as indicated by {@code RequestCount}.
   *
   * @param expectedCount the expected request expectedCount
   */
  public DefaultRequestExpectation(ExpectedCount expectedCount, RequestMatcher requestMatcher) {
    Assert.notNull(expectedCount, "ExpectedCount is required");
    Assert.notNull(requestMatcher, "RequestMatcher is required");
    this.requestCount = new RequestCount(expectedCount);
    this.requestMatchers.add(requestMatcher);
  }

  protected RequestCount getRequestCount() {
    return this.requestCount;
  }

  protected List<RequestMatcher> getRequestMatchers() {
    return this.requestMatchers;
  }

  @Nullable
  protected ResponseCreator getResponseCreator() {
    return this.responseCreator;
  }

  @Override
  public ResponseActions andExpect(RequestMatcher requestMatcher) {
    Assert.notNull(requestMatcher, "RequestMatcher is required");
    this.requestMatchers.add(requestMatcher);
    return this;
  }

  @Override
  public void andRespond(ResponseCreator responseCreator) {
    Assert.notNull(responseCreator, "ResponseCreator is required");
    this.responseCreator = responseCreator;
  }

  @Override
  public void match(ClientHttpRequest request) throws IOException {
    for (RequestMatcher matcher : getRequestMatchers()) {
      matcher.match(request);
    }
  }

  /**
   * Note that as of 5.0.3, the creation of the response, which may block
   * intentionally, is separated from request count tracking, and this
   * method no longer increments the count transparently. Instead
   * {@link #incrementAndValidate()} must be invoked independently.
   */
  @Override
  public ClientHttpResponse createResponse(@Nullable ClientHttpRequest request) throws IOException {
    ResponseCreator responseCreator = getResponseCreator();
    Assert.state(responseCreator != null, "createResponse() called before ResponseCreator was set");
    return responseCreator.createResponse(request);
  }

  @Override
  public boolean hasRemainingCount() {
    return getRequestCount().hasRemainingCount();
  }

  @Override
  public void incrementAndValidate() {
    getRequestCount().incrementAndValidate();
  }

  @Override
  public boolean isSatisfied() {
    return getRequestCount().isSatisfied();
  }

  /**
   * Helper class that keeps track of actual vs expected request count.
   */
  protected static class RequestCount {

    private final ExpectedCount expectedCount;

    private int matchedRequestCount;

    public RequestCount(ExpectedCount expectedCount) {
      this.expectedCount = expectedCount;
    }

    public ExpectedCount getExpectedCount() {
      return this.expectedCount;
    }

    public int getMatchedRequestCount() {
      return this.matchedRequestCount;
    }

    public void incrementAndValidate() {
      this.matchedRequestCount++;
      if (getMatchedRequestCount() > getExpectedCount().getMaxCount()) {
        throw new AssertionError("No more calls expected.");
      }
    }

    public boolean hasRemainingCount() {
      return (getMatchedRequestCount() < getExpectedCount().getMaxCount());
    }

    public boolean isSatisfied() {
      // Only validate min count since max count is checked on every request...
      return (getMatchedRequestCount() >= getExpectedCount().getMinCount());
    }
  }

}
