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

package cn.taketoday.samples;

import cn.taketoday.http.CacheControl;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/6 16:34
 */
@RestController
@RequestMapping("/headers")
public class HeadersController {

  @GET
  public HttpHeaders headers() {
    DefaultHttpHeaders headers = HttpHeaders.forWritable();
    headers.setCacheControl(CacheControl.noCache());
    return headers;
  }

  @GET("/as-is")
  public HttpHeaders headers(HttpHeaders requestHeaders) {
    return requestHeaders;
  }

}
