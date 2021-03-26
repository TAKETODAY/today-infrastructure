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

package cn.taketoday.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author TODAY 2021/3/22 11:28
 * @since 3.0
 */
public class MockMultipartFile implements MultipartFile {
  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public long getSize() {
    return 0;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getFileName() {
    return null;
  }

  @Override
  public void save(File dest) throws IOException {

  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return new byte[0];
  }

  @Override
  public Object getOriginalResource() {
    return null;
  }

  @Override
  public void delete() throws IOException {

  }

  @Override
  public InputStream getInputStream() throws IOException {
    return null;
  }
}
