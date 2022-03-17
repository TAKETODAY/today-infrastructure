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

import cn.taketoday.http.client.ClientHttpRequest;

/**
 * {@code RequestExpectationManager} that matches requests to expectations
 * regardless of the order of declaration of expected requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UnorderedRequestExpectationManager extends AbstractRequestExpectationManager {

  private final RequestExpectationGroup remainingExpectations = new RequestExpectationGroup();

  @Override
  protected void afterExpectationsDeclared() {
    this.remainingExpectations.addAllExpectations(getExpectations());
  }

  @Override
  public RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
    RequestExpectation expectation = this.remainingExpectations.findExpectation(request);
    if (expectation == null) {
      throw createUnexpectedRequestError(request);
    }
    this.remainingExpectations.update(expectation);
    return expectation;
  }

  @Override
  public void reset() {
    super.reset();
    this.remainingExpectations.reset();
  }

}
