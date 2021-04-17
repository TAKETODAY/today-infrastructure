/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.server.light;

import cn.taketoday.web.http.ContentDisposition;
import cn.taketoday.web.http.HttpHeaders;

/**
 * @author TODAY 2021/4/16 23:41
 */
public abstract class RequestPart {
  protected final HttpHeaders headers;
  protected ContentDisposition contentDisposition;

  protected RequestPart(HttpHeaders httpHeaders) {
    this.headers = httpHeaders;
  }

  public String getName() {
    return getContentDisposition().getName();
  }

  public HttpHeaders getHttpHeaders() {
    return headers;
  }

  protected ContentDisposition getContentDisposition() {
    if (contentDisposition == null) {
      contentDisposition = headers.getContentDisposition();
    }
    return contentDisposition;
  }

}
