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

package cn.taketoday.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.util.StringUtils;

/**
 * Builder used internally to create {@link WebResponse WebResponses}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.0
 */
final class MockWebResponseBuilder {

  private static final String DEFAULT_STATUS_MESSAGE = "N/A";

  private final long startTime;

  private final WebRequest webRequest;

  private final MockHttpServletResponse response;

  public MockWebResponseBuilder(long startTime, WebRequest webRequest, MockHttpServletResponse response) {
    Assert.notNull(webRequest, "WebRequest must not be null");
    Assert.notNull(response, "HttpServletResponse must not be null");
    this.startTime = startTime;
    this.webRequest = webRequest;
    this.response = response;
  }

  public WebResponse build() throws IOException {
    WebResponseData webResponseData = webResponseData();
    long endTime = System.currentTimeMillis();
    return new WebResponse(webResponseData, this.webRequest, endTime - this.startTime);
  }

  private WebResponseData webResponseData() throws IOException {
    List<NameValuePair> responseHeaders = responseHeaders();
    int statusCode = (this.response.getRedirectedUrl() != null ?
                      HttpStatus.MOVED_PERMANENTLY.value() : this.response.getStatus());
    String statusMessage = statusMessage(statusCode);
    return new WebResponseData(this.response.getContentAsByteArray(), statusCode, statusMessage, responseHeaders);
  }

  private String statusMessage(int statusCode) {
    String errorMessage = this.response.getErrorMessage();
    if (StringUtils.hasText(errorMessage)) {
      return errorMessage;
    }

    try {
      return HttpStatus.valueOf(statusCode).getReasonPhrase();
    }
    catch (IllegalArgumentException ex) {
      // ignore
    }

    return DEFAULT_STATUS_MESSAGE;
  }

  private List<NameValuePair> responseHeaders() {
    Collection<String> headerNames = this.response.getHeaderNames();
    List<NameValuePair> responseHeaders = new ArrayList<>(headerNames.size());
    for (String headerName : headerNames) {
      List<Object> headerValues = this.response.getHeaderValues(headerName);
      for (Object value : headerValues) {
        responseHeaders.add(new NameValuePair(headerName, String.valueOf(value)));
      }
    }
    String location = this.response.getRedirectedUrl();
    if (location != null) {
      responseHeaders.add(new NameValuePair("Location", location));
    }
    return responseHeaders;
  }

}
