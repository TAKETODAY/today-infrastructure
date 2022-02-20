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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:44
 */
class HiddenHttpMethodFilterTests {

  private final HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();

  @Test
  public void filterWithParameter() throws IOException, ServletException {
    filterWithParameterForMethod("delete", "DELETE");
    filterWithParameterForMethod("put", "PUT");
    filterWithParameterForMethod("patch", "PATCH");
  }

  @Test
  public void filterWithParameterDisallowedMethods() throws IOException, ServletException {
    filterWithParameterForMethod("trace", "POST");
    filterWithParameterForMethod("head", "POST");
    filterWithParameterForMethod("options", "POST");
  }

  @Test
  public void filterWithNoParameter() throws IOException, ServletException {
    filterWithParameterForMethod(null, "POST");
  }

  private void filterWithParameterForMethod(String methodParam, String expectedMethod)
          throws IOException, ServletException {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
    if(methodParam != null) {
      request.addParameter("_method", methodParam);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) ->
            assertThat(((HttpServletRequest) filterRequest).getMethod())
                    .as("Invalid method").isEqualTo(expectedMethod);
    this.filter.doFilter(request, response, filterChain);
  }

}
