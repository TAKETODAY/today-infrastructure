/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import cn.taketoday.lang.Assert;

/**
 * {@link Resource} implementation for a given byte array.
 * <p>Creates a {@link ByteArrayInputStream} for the given byte array.
 *
 * <p>Useful for loading content from any given byte array,
 * without having to resort to a single-use {@link InputStreamResource}.
 * Particularly useful for creating mail attachments from local content,
 * where JavaMail needs to be able to read the stream multiple times.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/3/7 11:16
 * @see java.io.ByteArrayInputStream
 * @see InputStreamResource
 * @since 3.0
 */
public class ByteArrayResource extends AbstractResource {

  private final byte[] byteArray;

  private final String description;

  /**
   * Create a new {@code ByteArrayResource}.
   *
   * @param byteArray the byte array to wrap
   */
  public ByteArrayResource(byte[] byteArray) {
    this(byteArray, "resource loaded from byte array");
  }

  /**
   * Create a new {@code ByteArrayResource} with a description.
   *
   * @param byteArray the byte array to wrap
   * @param description where the byte array comes from
   */
  public ByteArrayResource(byte[] byteArray, String description) {
    Assert.notNull(byteArray, "Byte array is required");
    this.byteArray = byteArray;
    this.description = (description != null ? description : "");
  }

  /**
   * Return the underlying byte array.
   */
  public final byte[] getByteArray() {
    return this.byteArray;
  }

  @Override
  public boolean isReadable() {
    return exists();
  }

  /**
   * This implementation always returns {@code true}.
   */
  @Override
  public boolean exists() {
    return true;
  }

  /**
   * This implementation returns the length of the underlying byte array.
   */
  @Override
  public long contentLength() {
    return this.byteArray.length;
  }

  /**
   * This implementation returns a ByteArrayInputStream for the
   * underlying byte array.
   *
   * @see java.io.ByteArrayInputStream
   */
  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(this.byteArray);
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    int length = this.byteArray.length;
    byte[] result = new byte[length];
    System.arraycopy(this.byteArray, 0, result, 0, length);
    return result;
  }

  @Override
  public String getContentAsString(Charset charset) throws IOException {
    return new String(this.byteArray, charset);
  }

  /**
   * This implementation returns a description that includes the passed-in
   * {@code description}, if any.
   */
  @Override
  public String toString() {
    return "Byte array resource [" + this.description + "]";
  }

  /**
   * This implementation compares the underlying byte array.
   *
   * @see java.util.Arrays#equals(byte[], byte[])
   */
  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof ByteArrayResource &&
            Arrays.equals(((ByteArrayResource) other).byteArray, this.byteArray)));
  }

  /**
   * This implementation returns the hash code based on the
   * underlying byte array.
   */
  @Override
  public int hashCode() {
    return (byte[].class.hashCode() * 29 * this.byteArray.length);
  }

}
