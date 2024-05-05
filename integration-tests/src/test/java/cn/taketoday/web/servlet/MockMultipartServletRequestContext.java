/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.servlet;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.mock.web.MockMultipartHttpMockRequest;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.multipart.MultipartRequest;
import cn.taketoday.mock.api.http.HttpServletResponse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 17:04
 */
@SuppressWarnings("serial")
public class MockMultipartServletRequestContext extends ServletRequestContext {

  private final MockMultipartHttpMockRequest request;

  public MockMultipartServletRequestContext(MockMultipartHttpMockRequest request, HttpServletResponse response) {
    super(request, response);
    this.request = request;
  }

  public MockMultipartServletRequestContext(ApplicationContext applicationContext, MockMultipartHttpMockRequest request, HttpServletResponse response) {
    super(applicationContext, request, response);
    this.request = request;
  }

  @Override
  public MultipartRequest getMultipartRequest() {
    return request;
  }

}
