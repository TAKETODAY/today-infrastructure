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

package cn.taketoday.web.multipart;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.core.io.AbstractResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Adapt {@link MultipartFile} to {@link Resource},
 * exposing the content as {@code InputStream} and also overriding
 * {@link #contentLength()} as well as {@link #getName()}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MultipartFile#getResource()
 * @since 4.0 2022/4/2 11:58
 */
class MultipartFileResource extends AbstractResource {

  private final MultipartFile multipartFile;

  public MultipartFileResource(MultipartFile multipartFile) {
    Assert.notNull(multipartFile, "MultipartFile is required");
    this.multipartFile = multipartFile;
  }

  /**
   * This implementation always returns {@code true}.
   */
  @Override
  public boolean exists() {
    return true;
  }

  /**
   * This implementation always returns {@code true}.
   */
  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public long contentLength() {
    return this.multipartFile.getSize();
  }

  @Override
  public String getName() {
    return this.multipartFile.getOriginalFilename();
  }

  /**
   * This implementation throws IllegalStateException if attempting to
   * read the underlying stream multiple times.
   */
  @Override
  public InputStream getInputStream() throws IOException, IllegalStateException {
    return this.multipartFile.getInputStream();
  }

  /**
   * This implementation returns a description that has the Multipart name.
   */
  @Override
  public String toString() {
    return "MultipartFile resource [" + this.multipartFile.getName() + "]";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof MultipartFileResource &&
            ((MultipartFileResource) other).multipartFile.equals(this.multipartFile)));
  }

  @Override
  public int hashCode() {
    return this.multipartFile.hashCode();
  }

}
