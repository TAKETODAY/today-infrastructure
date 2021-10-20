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

package cn.taketoday.web.framework.server.light;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import cn.taketoday.web.http.ContentDisposition;
import cn.taketoday.web.multipart.AbstractMultipartFile;

/**
 * @author TODAY 2021/4/16 21:15
 */
public final class LightMultipartFile extends AbstractMultipartFile implements RequestPart {

  private final int partSize;
  protected ContentDisposition contentDisposition;

  private String contentType;

  private File tempFile;
  private InputStream inputStream;

  LightMultipartFile(File tempFile, ContentDisposition contentDisposition, int partSize) {
    this.contentDisposition = contentDisposition;
    this.partSize = partSize;
    this.tempFile = tempFile;
  }

  LightMultipartFile(File tempFile, ContentDisposition contentDisposition, String contentType, int partSize) {
    this.partSize = partSize;
    this.tempFile = tempFile;
    this.contentType = contentType;
    this.contentDisposition = contentDisposition;
  }

  LightMultipartFile(byte[] bytes, ContentDisposition contentDisposition, int partSize) {
    this.partSize = partSize;
    this.cachedBytes = bytes;
    this.contentDisposition = contentDisposition;
  }

  public LightMultipartFile(byte[] bytes, ContentDisposition contentDisposition, String contentType, int partSize) {
    this.partSize = partSize;
    this.cachedBytes = bytes;
    this.contentType = contentType;
    this.contentDisposition = contentDisposition;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public long getSize() {
    return partSize;
  }

  @Override
  public String getName() {
    return getContentDisposition().getName();
  }

  @Override
  public String getFileName() {
    return getContentDisposition().getFilename();
  }

  public ContentDisposition getContentDisposition() {
    return contentDisposition;
  }

  public void setTempFile(File tempFile) {
    this.tempFile = tempFile;
  }

  @Override
  public void saveInternal(File dest) throws IOException {
    final InputStream inputStream = this.inputStream;
    if (inputStream != null) {
      if (!tempFile.renameTo(dest)) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(tempFile));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
          Utils.copy(in, out);
        }
      }
    }
    else {
      try (FileOutputStream fout = new FileOutputStream(dest)) {
        fout.write(getBytes());
      }
    }
  }

  @Override
  public boolean isEmpty() {
    return partSize == 0L;
  }

  protected void setCachedBytes(byte[] cachedBytes) {
    this.cachedBytes = cachedBytes;
  }

  @Override
  protected byte[] doGetBytes() throws IOException {
    byte[] cachedBytes = new byte[partSize];
    Utils.readBytes(inputStream, cachedBytes);
    return cachedBytes;
  }

  @Override
  public Object getOriginalResource() {
    return null;
  }

  @Override
  public void delete() throws IOException {
    if (tempFile != null) {
      Files.delete(tempFile.toPath());
    }
  }

  @Override
  public InputStream getInputStream() {
    InputStream inputStream = this.inputStream;
    if (inputStream == null) {
      inputStream = new ByteArrayInputStream(cachedBytes);
      this.inputStream = inputStream;
    }
    return inputStream;
  }

}
