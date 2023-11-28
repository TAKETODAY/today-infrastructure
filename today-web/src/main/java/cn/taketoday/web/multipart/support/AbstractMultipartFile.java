/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.multipart.support;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author TODAY 2021/4/18 20:38
 * @since 3.0
 */
public abstract class AbstractMultipartFile extends AbstractMultipart implements MultipartFile {

  @Serial
  private static final long serialVersionUID = 1L;

  protected byte[] cachedBytes;

  @Override
  public void transferTo(File dest) throws IOException {
    // fix #3 Upload file not found exception
    File parentFile = dest.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    /*
     * The uploaded file is being stored on disk
     * in a temporary location so move it to the
     * desired file.
     */
    if (dest.exists()) {
      Files.delete(dest.toPath());
    }
    saveInternal(dest);
  }

  protected abstract void saveInternal(File dest) throws IOException;

  @Override
  public byte[] getBytes() throws IOException {
    byte[] cachedBytes = this.cachedBytes;
    if (cachedBytes == null) {
      cachedBytes = doGetBytes();
      this.cachedBytes = cachedBytes;
    }
    return cachedBytes;
  }

  protected abstract byte[] doGetBytes() throws IOException;

  @Override
  public boolean isFormField() {
    return false;
  }

  @Override
  public String getValue() {
    try {
      return new String(getBytes(), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

}
