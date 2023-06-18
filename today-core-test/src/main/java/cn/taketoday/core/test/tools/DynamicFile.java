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

package cn.taketoday.core.test.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Assert;

/**
 * Abstract base class for dynamically generated files.
 *
 * @author Phillip Webb
 * @see SourceFile
 * @see ResourceFile
 * @since 4.0
 */
public abstract sealed class DynamicFile permits SourceFile, ResourceFile {

  private final String path;

  private final String content;

  protected DynamicFile(String path, String content) {
    Assert.hasText(path, "'path' must not be empty");
    Assert.hasText(content, "'content' must not be empty");
    this.path = path;
    this.content = content;
  }

  protected static String toString(WritableContent writableContent) {
    try {
      StringBuilder stringBuilder = new StringBuilder();
      writableContent.writeTo(stringBuilder);
      return stringBuilder.toString();
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to read content", ex);
    }
  }

  /**
   * Return the contents of the file as a byte array.
   *
   * @return the file contents as a byte array
   */
  public byte[] getBytes() {
    return this.content.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Return the contents of the file.
   *
   * @return the file contents
   */
  public String getContent() {
    return this.content;
  }

  /**
   * Return the relative path of the file.
   *
   * @return the file path
   */
  public String getPath() {
    return this.path;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DynamicFile other = (DynamicFile) obj;
    return Objects.equals(this.path, other.path)
            && Objects.equals(this.content, other.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.path, this.content);
  }

  @Override
  public String toString() {
    return this.path;
  }

}
