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

package cn.taketoday.test.web.servlet;

import java.io.IOException;

import cn.taketoday.util.StreamUtils;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Simple example Servlet used for testing.
 *
 * @author Phillip Webb
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ExampleServlet extends GenericServlet {

  private final boolean echoRequestInfo;

  private final boolean writeWithoutContentLength;

  public ExampleServlet() {
    this(false, false);
  }

  public ExampleServlet(boolean echoRequestInfo, boolean writeWithoutContentLength) {
    this.echoRequestInfo = echoRequestInfo;
    this.writeWithoutContentLength = writeWithoutContentLength;
  }

  @Override
  public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
    String content = "Hello World";
    if (this.echoRequestInfo) {
      content += " scheme=" + request.getScheme();
      content += " remoteaddr=" + request.getRemoteAddr();
    }
    if (this.writeWithoutContentLength) {
      response.setContentType("text/plain");
      ServletOutputStream outputStream = response.getOutputStream();
      StreamUtils.copy(content.getBytes(), outputStream);
      outputStream.flush();
    }
    else {
      response.getWriter().write(content);
    }
  }

}
