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

package cn.taketoday.web.servlet.filter;

import java.io.IOException;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * A response wrapper used for the implementation of
 * {@link RelativeRedirectFilter} also shared with {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
final class RelativeRedirectResponseWrapper extends HttpServletResponseWrapper {

  private final HttpStatusCode redirectStatus;

  private RelativeRedirectResponseWrapper(HttpServletResponse response, HttpStatusCode redirectStatus) {
    super(response);
    Assert.notNull(redirectStatus, "'redirectStatus' is required");
    this.redirectStatus = redirectStatus;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    resetBuffer();
    setStatus(this.redirectStatus.value());
    setHeader(HttpHeaders.LOCATION, location);
    flushBuffer();
  }

  public static HttpServletResponse wrapIfNecessary(HttpServletResponse response,
          HttpStatusCode redirectStatus) {

    var wrapper = ServletUtils.getNativeResponse(response, RelativeRedirectResponseWrapper.class);

    return wrapper != null ? response :
           new RelativeRedirectResponseWrapper(response, redirectStatus);
  }

}
