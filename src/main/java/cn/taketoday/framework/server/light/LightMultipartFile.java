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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import cn.taketoday.web.Constant;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author TODAY 2021/4/16 21:15
 */
public class LightMultipartFile extends RequestPart implements MultipartFile {

  public LightMultipartFile(ByteArrayInputStream inputStream, HttpHeaders httpHeaders) {
    super(inputStream, httpHeaders);
  }

  @Override
  public String getContentType() {
    return headers.getFirst(Constant.CONTENT_TYPE);
  }

  @Override
  public long getSize() {
    return inputStream.available();
  }

  @Override
  public String getName() {
    return getContentDisposition().getName();
  }

  @Override
  public String getFileName() {
    return getContentDisposition().getFilename();
  }

  @Override
  public void save(File dest) throws IOException {

  }

  @Override
  public boolean isEmpty() {
    return inputStream.available() == 0L;
  }

  @Override
  public byte[] getBytes() throws IOException {
    byte[] ret = new byte[(int) getSize()];
    inputStream.read(ret);
    return ret;
  }

  @Override
  public Object getOriginalResource() {
    return null;
  }

  @Override
  public void delete() throws IOException {

  }

  @Override
  public ByteArrayInputStream getInputStream() {
    return inputStream;
  }

}
