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

package cn.taketoday.core.io;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.lang.Assert;

/**
 * {@link Resource} implementation for a given {@link InputStream}.
 * <p>Should only be used if no other specific {@code Resource} implementation
 * is applicable. In particular, prefer {@link ByteArrayResource} or any of the
 * file-based {@code Resource} implementations where possible.
 *
 * <p>In contrast to other {@code Resource} implementations, this is a descriptor
 * for an <i>already opened</i> resource. Do not use an {@code InputStreamResource}
 * if you need to keep the resource descriptor somewhere, or if you need to read
 * from a stream multiple times.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/3/7 11:18
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileBasedResource
 * @see UrlBasedResource
 * @since 3.0
 */
public class InputStreamResource extends AbstractResource {

  private boolean read = false;
  private final String description;
  private final InputStream inputStream;

  /**
   * Create a new InputStreamResource.
   *
   * @param inputStream the InputStream to use
   */
  public InputStreamResource(InputStream inputStream) {
    this(inputStream, "resource loaded through InputStream");
  }

  /**
   * Create a new InputStreamResource.
   *
   * @param inputStream the InputStream to use
   * @param description where the InputStream comes from
   */
  public InputStreamResource(InputStream inputStream, String description) {
    Assert.notNull(inputStream, "InputStream must not be null");
    this.inputStream = inputStream;
    this.description = (description != null ? description : "");
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

  /**
   * This implementation throws IllegalStateException if attempting to
   * read the underlying stream multiple times.
   */
  @Override
  public InputStream getInputStream() throws IOException, IllegalStateException {
    Assert.state(!read, "InputStream has already been read - do not use InputStreamResource if a stream needs to be read multiple times");

    this.read = true;
    return this.inputStream;
  }

  /**
   * This implementation returns a description that includes the passed-in
   * description, if any.
   */
  @Override
  public String toString() {
    return "InputStream resource [" + this.description + "]";
  }

  /**
   * This implementation compares the underlying InputStream.
   */
  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof InputStreamResource &&
            ((InputStreamResource) other).inputStream.equals(this.inputStream)));
  }

  /**
   * This implementation returns the hash code of the underlying InputStream.
   */
  @Override
  public int hashCode() {
    return this.inputStream.hashCode();
  }

}
