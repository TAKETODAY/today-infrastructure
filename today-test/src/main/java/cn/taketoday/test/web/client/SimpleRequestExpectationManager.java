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
import java.util.Iterator;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple {@code RequestExpectationManager} that matches requests to expectations
 * sequentially, i.e. in the order of declaration of expectations.
 *
 * <p>When request expectations have an expected count greater than one,
 * only the first execution is expected to match the order of declaration.
 * Subsequent request executions may be inserted anywhere thereafter.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

  /** Expectations in the order of declaration (count may be > 1). */
  @Nullable
  private Iterator<RequestExpectation> expectationIterator;

  /** Track expectations that have a remaining count. */
  private final RequestExpectationGroup repeatExpectations = new RequestExpectationGroup();

  @Override
  protected void afterExpectationsDeclared() {
    Assert.state(this.expectationIterator == null, "Expectations already declared");
    this.expectationIterator = getExpectations().iterator();
  }

  @Override
  protected RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
    RequestExpectation expectation = this.repeatExpectations.findExpectation(request);
    if (expectation == null) {
      if (this.expectationIterator == null || !this.expectationIterator.hasNext()) {
        throw createUnexpectedRequestError(request);
      }
      expectation = this.expectationIterator.next();
      expectation.match(request);
    }
    this.repeatExpectations.update(expectation);
    return expectation;
  }

  @Override
  public void reset() {
    super.reset();
    this.expectationIterator = null;
    this.repeatExpectations.reset();
  }

}
