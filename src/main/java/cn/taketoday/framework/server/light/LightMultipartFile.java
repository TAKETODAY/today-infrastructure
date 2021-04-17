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

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.web.Constant;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author TODAY 2021/4/16 21:15
 */
public final class LightMultipartFile extends RequestPart implements MultipartFile {
  private InputStream inputStream;

  private File tempFile;
  private byte[] cachedBytes;

  private final int partSize;

  public void setTempFile(File tempFile) {
    this.tempFile = tempFile;
  }

  LightMultipartFile(File tempFile, HttpHeaders httpHeaders, int partSize) {
    super(httpHeaders);
    this.partSize = partSize;
    this.tempFile = tempFile;
  }

  LightMultipartFile(byte[] bytes, HttpHeaders httpHeaders, int partSize) {
    super(httpHeaders);
    this.cachedBytes = bytes;
    this.partSize = partSize;
  }

  @Override
  public String getContentType() {
    return headers.getFirst(Constant.CONTENT_TYPE);
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

  @Override
  public void save(File dest) throws IOException {
    // fix #3 Upload file not found exception
    File parentFile = dest.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    final InputStream inputStream = this.inputStream;
    if (inputStream != null) {
      /*
       * The uploaded file is being stored on disk
       * in a temporary location so move it to the
       * desired file.
       */
      if (dest.exists()) {
        if (!dest.delete()) {
          throw new FileUploadException(
                  "Cannot write uploaded file to disk!");
        }
      }
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

  void setCachedBytes(byte[] cachedBytes) {
    this.cachedBytes = cachedBytes;
  }

  @Override
  public byte[] getBytes() throws IOException {
    byte[] cachedBytes = this.cachedBytes;
    if (cachedBytes == null) {
      cachedBytes = new byte[(int) getSize()];
      Utils.readBytes(inputStream, cachedBytes);
      this.cachedBytes = cachedBytes;
    }
    return cachedBytes;
  }

  @Override
  public Object getOriginalResource() {
    return null;
  }

  @Override
  public void delete() {
    if (tempFile != null) {
      tempFile.delete();
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
