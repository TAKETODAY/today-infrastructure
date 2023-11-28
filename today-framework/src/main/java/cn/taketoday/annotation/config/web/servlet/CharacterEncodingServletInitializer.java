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

package cn.taketoday.annotation.config.web.servlet;

import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * apply character encoding to servlet context
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/16 23:01
 */
public class CharacterEncodingServletInitializer implements ServletContextInitializer {

  @Nullable
  private String requestCharacterEncoding = Constant.DEFAULT_ENCODING;

  @Nullable
  private String responseCharacterEncoding = Constant.DEFAULT_ENCODING;

  public void setRequestCharacterEncoding(@Nullable String requestCharacterEncoding) {
    this.requestCharacterEncoding = requestCharacterEncoding;
  }

  public void setResponseCharacterEncoding(@Nullable String responseCharacterEncoding) {
    this.responseCharacterEncoding = responseCharacterEncoding;
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    if (requestCharacterEncoding != null) {
      servletContext.setRequestCharacterEncoding(requestCharacterEncoding);
    }
    if (responseCharacterEncoding != null) {
      servletContext.setResponseCharacterEncoding(responseCharacterEncoding);
    }
  }
}
