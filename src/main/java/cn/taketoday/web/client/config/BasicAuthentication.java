/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.client.config;

import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;

/**
 * Basic authentication details to be applied to {@link HttpHeaders}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @since 4.0
 */
class BasicAuthentication {

  private final String username;
  private final String password;
  private final Charset charset;

  BasicAuthentication(String username, String password, Charset charset) {
    Assert.notNull(username, "Username must not be null");
    Assert.notNull(password, "Password must not be null");
    this.username = username;
    this.password = password;
    this.charset = charset;
  }

  void applyTo(HttpHeaders headers) {
    if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
      headers.setBasicAuth(this.username, this.password, this.charset);
    }
  }

}
