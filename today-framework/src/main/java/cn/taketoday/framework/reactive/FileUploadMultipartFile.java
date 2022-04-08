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
package cn.taketoday.framework.reactive;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;

import cn.taketoday.web.multipart.AbstractMultipartFile;
import cn.taketoday.web.multipart.MultipartFile;
import io.netty.handler.codec.http.multipart.FileUpload;

/**
 * Netty MultipartFile
 *
 * @author TODAY 2019-11-14 13:11
 * @see FileUpload
 */
final class FileUploadMultipartFile
        extends AbstractMultipartFile implements MultipartFile {
  @Serial
  private static final long serialVersionUID = 1L;
  private final FileUpload fileUpload;

  public FileUploadMultipartFile(FileUpload fileUpload) {
    this.fileUpload = fileUpload;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(getBytes());
  }

  @Override
  public String getContentType() {
    return fileUpload.getContentType();
  }

  @Override
  public long getSize() {
    return fileUpload.length();
  }

  @Override
  public String getName() {
    return fileUpload.getName();
  }

  @Override
  public String getOriginalFilename() {
    return fileUpload.getFilename();
  }

  @Override
  protected void saveInternal(File dest) throws IOException {
    fileUpload.renameTo(dest);
  }

  @Override
  public boolean isEmpty() {
    return getSize() == 0;
  }

  @Override
  protected byte[] doGetBytes() throws IOException {
    return fileUpload.get();
  }

  @Override
  public Object getOriginalResource() {
    return fileUpload;
  }

  @Override
  public void delete() {
    fileUpload.delete();
  }

}
