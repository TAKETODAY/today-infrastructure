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

package infra.web.multipart.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import infra.lang.Nullable;
import infra.util.ExceptionUtils;
import infra.web.multipart.MultipartFile;

/**
 * Abstract multipart-file
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/18 20:38
 */
public abstract class AbstractMultipartFile extends AbstractMultipart implements MultipartFile {

  @Nullable
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
  public final boolean isFormField() {
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

  @Override
  public final void cleanup() throws IOException {
    cachedBytes = null;
    deleteInternal();
  }

  protected abstract void deleteInternal() throws IOException;

  @Override
  public String toString() {
    return "%s: '%s'".formatted(getClass().getSimpleName(), getName());
  }

}
