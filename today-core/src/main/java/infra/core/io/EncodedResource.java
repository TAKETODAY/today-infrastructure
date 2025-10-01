/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Objects;

import infra.lang.Assert;

/**
 * Holder that combines a {@link Resource} descriptor with a specific encoding
 * or {@code Charset} to be used for reading from the resource.
 *
 * <p>
 * Used as an argument for operations that support reading content with a
 * specific encoding, typically via a {@code java.io.Reader}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Resource#getInputStream()
 * @see java.io.Reader
 * @see java.nio.charset.Charset
 * @since 2019-12-05 23:17
 * @since 4.0
 */
public class EncodedResource implements InputStreamSource {

  @Nullable
  private final String encoding;

  @Nullable
  private final Charset charset;

  private final Resource resource;

  /**
   * Create a new {@code EncodedResource} for the given {@code Resource}, not
   * specifying an explicit encoding or {@code Charset}.
   *
   * @param resource the {@code Resource} to hold (never {@code null})
   */
  public EncodedResource(Resource resource) {
    this(resource, null, null);
  }

  /**
   * Create a new {@code EncodedResource} for the given {@code Resource}, using
   * the specified {@code encoding}.
   *
   * @param resource the {@code Resource} to hold (never {@code null})
   * @param encoding the encoding to use for reading from the resource
   */
  public EncodedResource(Resource resource, @Nullable String encoding) {
    this(resource, encoding, null);
  }

  /**
   * Create a new {@code EncodedResource} for the given {@code Resource}, using
   * the specified {@code Charset}.
   *
   * @param resource the {@code Resource} to hold (never {@code null})
   * @param charset the {@code Charset} to use for reading from the resource
   */
  public EncodedResource(Resource resource, @Nullable Charset charset) {
    this(resource, null, charset);
  }

  private EncodedResource(Resource resource, @Nullable String encoding, @Nullable Charset charset) {
    Assert.notNull(resource, "Resource is required");
    this.resource = resource;
    this.encoding = encoding;
    this.charset = charset;
  }

  /**
   * Return the {@code Resource} held by this {@code EncodedResource}.
   */
  public final Resource getResource() {
    return this.resource;
  }

  /**
   * Return the encoding to use for reading from the {@linkplain #getResource()
   * resource}, or {@code null} if none specified.
   */
  @Nullable
  public final String getEncoding() {
    return this.encoding;
  }

  /**
   * Return the {@code Charset} to use for reading from the
   * {@linkplain #getResource() resource}, or {@code null} if none specified.
   */
  @Nullable
  public final Charset getCharset() {
    return this.charset;
  }

  /**
   * Determine whether a {@link Reader} is required as opposed to an
   * {@link InputStream}, i.e. whether an {@linkplain #getEncoding() encoding} or
   * a {@link #getCharset() Charset} has been specified.
   *
   * @see #getReader()
   * @see #getInputStream()
   */
  public boolean requiresReader() {
    return (this.encoding != null || this.charset != null);
  }

  /**
   * Open a {@code java.io.Reader} for the specified resource, using the specified
   * {@link #getCharset() Charset} or {@linkplain #getEncoding() encoding} (if
   * any).
   *
   * @throws IOException if opening the Reader failed
   * @see #requiresReader()
   * @see #getInputStream()
   */
  public Reader getReader() throws IOException {
    if (this.charset != null) {
      return new InputStreamReader(this.resource.getInputStream(), this.charset);
    }
    else if (this.encoding != null) {
      return new InputStreamReader(this.resource.getInputStream(), this.encoding);
    }
    else {
      return new InputStreamReader(this.resource.getInputStream());
    }
  }

  /**
   * Open an {@code InputStream} for the specified resource, ignoring any
   * specified {@link #getCharset() Charset} or {@linkplain #getEncoding()
   * encoding}.
   *
   * @throws IOException if opening the InputStream failed
   * @see #requiresReader()
   * @see #getReader()
   */
  @Override
  public InputStream getInputStream() throws IOException {
    return this.resource.getInputStream();
  }

  /**
   * Returns the contents of the specified resource as a string, using the specified
   * {@link #getCharset() Charset} or {@linkplain #getEncoding() encoding} (if any).
   *
   * @throws IOException if opening the resource failed
   * @see Resource#getContentAsString(Charset)
   * @since 4.0
   */
  public String getContentAsString() throws IOException {
    Charset charset;
    if (this.charset != null) {
      charset = this.charset;
    }
    else if (this.encoding != null) {
      charset = Charset.forName(this.encoding);
    }
    else {
      charset = Charset.defaultCharset();
    }
    return this.resource.getContentAsString(charset);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EncodedResource o)) {
      return false;
    }
    return this.resource.equals(o.resource)
            && Objects.equals(this.charset, o.charset)
            && Objects.equals(this.encoding, o.encoding);
  }

  @Override
  public int hashCode() {
    return this.resource.hashCode();
  }

  @Override
  public String toString() {
    return this.resource.toString();
  }

}
