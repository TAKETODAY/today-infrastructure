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

package cn.taketoday.test.web.servlet.htmlunit;

import cn.taketoday.lang.Assert;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.web.servlet.request.RequestPostProcessor;
import cn.taketoday.util.StringUtils;

/**
 * {@link RequestPostProcessor} to update the request for a forwarded dispatch.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.0
 */
final class ForwardRequestPostProcessor implements RequestPostProcessor {

  private final String forwardedUrl;

  public ForwardRequestPostProcessor(String forwardedUrl) {
    Assert.hasText(forwardedUrl, "Forwarded URL must not be null or empty");
    this.forwardedUrl = forwardedUrl;
  }

  @Override
  public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
    request.setRequestURI(this.forwardedUrl);
    request.setServletPath(initServletPath(request.getContextPath()));
    return request;
  }

  private String initServletPath(String contextPath) {
    if (StringUtils.hasText(contextPath)) {
      Assert.state(this.forwardedUrl.startsWith(contextPath), "Forward supported to same contextPath only");
      return (this.forwardedUrl.length() > contextPath.length() ?
              this.forwardedUrl.substring(contextPath.length()) : "");
    }
    else {
      return this.forwardedUrl;
    }
  }

}
