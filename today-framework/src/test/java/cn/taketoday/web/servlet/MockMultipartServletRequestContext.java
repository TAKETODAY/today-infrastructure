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

package cn.taketoday.web.servlet;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.web.bind.MockMultipartHttpServletRequest;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.MultipartRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 17:04
 */
public class MockMultipartServletRequestContext extends MockServletRequestContext {

  private final MockMultipartHttpServletRequest request;

  public MockMultipartServletRequestContext(MockMultipartHttpServletRequest request, HttpServletResponse response) {
    super(request, response);
    this.request = request;
  }

  public MockMultipartServletRequestContext(ApplicationContext applicationContext, MockMultipartHttpServletRequest request, HttpServletResponse response) {
    super(applicationContext, request, response);
    this.request = request;
  }

  @Override
  public MultipartRequest getMultipartRequest() {
    return request;
  }

}
