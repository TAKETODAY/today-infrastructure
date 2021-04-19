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

import cn.taketoday.web.multipart.MultipartConfiguration;

/**
 * @author TODAY 2021/4/15 0:02
 */
public class LightHttpConfig {
  private int port = -1;

  private int headerMaxCount = 100;
  /**
   * user determine the response body initial size
   */
  private int responseBodyInitialSize = 512;

  private MultipartConfiguration multipartConfig;

  /**
   * Multi-segment request file size exceeds this value to cache the file
   */
  private int maxMultipartInMemSize  = 4096;

  /**
   * The buffer size of the file when buffering to a temporary
   *
   * @see MultipartConfiguration#getLocation()
   */
  private int multipartBufferSize;

  public void setMultipartConfig(MultipartConfiguration multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  public MultipartConfiguration getMultipartConfig() {
    return multipartConfig;
  }

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

  public void setMaxMultipartInMemSize(int maxMultipartInMemSize) {
    this.maxMultipartInMemSize = maxMultipartInMemSize;
  }

  /**
   * Multi-segment request file size exceeds this value to cache the file
   */
  public int getMaxMultipartInMemSize() {
    return maxMultipartInMemSize;
  }

  /**
   * The buffer size of the file when buffering to a temporary
   *
   * @see MultipartConfiguration#getLocation()
   */
  public int getMultipartBufferSize() {
    return multipartBufferSize;
  }

  public void setMultipartBufferSize(int multipartBufferSize) {
    this.multipartBufferSize = multipartBufferSize;
  }
  // static

  public static LightHttpConfig defaultConfig() {
    return new LightHttpConfig();
  }

}
