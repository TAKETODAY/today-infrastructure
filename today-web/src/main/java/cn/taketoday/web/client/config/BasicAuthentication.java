/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.lang.Nullable;

/**
 * Basic authentication details to be applied to {@link HttpHeaders}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BasicAuthentication {

  private final String username;
  private final String password;

  @Nullable
  private final Charset charset;

  BasicAuthentication(String username, String password, @Nullable Charset charset) {
    Assert.notNull(username, "Username is required");
    Assert.notNull(password, "Password is required");
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
