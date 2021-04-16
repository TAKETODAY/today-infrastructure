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

import java.io.InputStream;

/**
 * @author TODAY 2021/4/15 0:02
 */
public class LightHttpConfig {
  private int port = 80; // todo update port

  private int headerMaxCount = 100;
  /**
   * user determine the response body initial size
   */
  private int responseBodyInitialSize = 1024;

  public void setResponseBodyInitialSize(int responseBodyInitialSize) {
    this.responseBodyInitialSize = responseBodyInitialSize;
  }

  public int getResponseBodyInitialSize() {
    return responseBodyInitialSize;
  }

  public void setHeaderMaxCount(int headerMaxCount) {
    this.headerMaxCount = headerMaxCount;
  }

  /**
   * @return max header count
   *
   * @see Utils#readHeaders(InputStream, LightHttpConfig)
   */
  public int getHeaderMaxCount() {
    return headerMaxCount;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  // static

  public static LightHttpConfig defaultConfig() {
    return new LightHttpConfig();
  }

}
