/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

import infra.lang.Assert;

/**
 * {@link Resource} implementation for a given {@link InputStream} or a given
 * {@link InputStreamSource} (which can be supplied as a lambda expression)
 * for a lazy {@link InputStream} on demand.
 *
 * <p>Should only be used if no other specific {@code Resource} implementation
 * is applicable. In particular, prefer {@link ByteArrayResource} or any of the
 * file-based {@code Resource} implementations if possible. If you need to obtain
 * a custom stream multiple times, use a custom {@link AbstractResource} subclass
 * with a corresponding {@code getInputStream()} implementation.
 *
 * <p>In contrast to other {@code Resource} implementations, this is a descriptor
 * for an <i>already opened</i> resource - therefore returning {@code true} from
 * {@link #isOpen()}. Do not use an {@code InputStreamResource} if you need to keep
 * the resource descriptor somewhere, or if you need to read from a stream multiple
 * times. This also applies when constructed with an {@code InputStreamSource}
 * which lazily obtains the stream but only allows for single access as well.
 *
 * <p><b>NOTE: This class does not provide an independent {@link #contentLength()}
 * implementation: Any such call will consume the given {@code InputStream}!</b>
 * Consider overriding {@code #contentLength()} with a custom implementation if
 * possible. For any other purpose, it is not recommended to extend from this
 * class; this is particularly true when used with Infra web resource rendering
 * which specifically skips {@code #contentLength()} for this exact class only.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileSystemResource
 * @see UrlResource
 * @since 3.0 2021/3/7 11:18
 */
public class InputStreamResource extends AbstractResource {

  private final InputStreamSource inputStreamSource;

  private final String description;

  private final Object equality;

  private boolean read = false;

  /**
   * Create a new {@code InputStreamResource} with a lazy {@code InputStream}
   * for single use.
   *
   * @param inputStreamSource an on-demand source for a single-use InputStream
   */
  public InputStreamResource(InputStreamSource inputStreamSource) {
    this(inputStreamSource, "resource loaded from InputStreamSource");
  }

  /**
   * Create a new {@code InputStreamResource} with a lazy {@code InputStream}
   * for single use.
   *
   * @param inputStreamSource an on-demand source for a single-use InputStream
   * @param description where the InputStream comes from
   */
  public InputStreamResource(InputStreamSource inputStreamSource, @Nullable String description) {
    Assert.notNull(inputStreamSource, "InputStreamSource is required");
    this.inputStreamSource = inputStreamSource;
    this.description = (description != null ? description : "");
    this.equality = inputStreamSource;
  }

  /**
   * Create a new {@code InputStreamResource} for an existing {@code InputStream}.
   * <p>Consider retrieving the InputStream on demand if possible, reducing its
   * lifetime and reliably opening it and closing it through regular
   * {@link InputStreamSource#getInputStream()} usage.
   *
   * @param inputStream the InputStream to use
   * @see #InputStreamResource(InputStreamSource)
   */
  public InputStreamResource(InputStream inputStream) {
    this(inputStream, "resource loaded through InputStream");
  }

  /**
   * Create a new {@code InputStreamResource} for an existing {@code InputStream}.
   *
   * @param inputStream the InputStream to use
   * @param description where the InputStream comes from
   * @see #InputStreamResource(InputStreamSource, String)
   */
  public InputStreamResource(InputStream inputStream, @Nullable String description) {
    Assert.notNull(inputStream, "InputStream is required");
    this.inputStreamSource = () -> inputStream;
    this.description = (description != null ? description : "");
    this.equality = inputStream;
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
    if (this.read) {
      throw new IllegalStateException("InputStream has already been read (possibly for early content length " +
              "determination) - do not use InputStreamResource if a stream needs to be read multiple times");
    }
    this.read = true;
    return this.inputStreamSource.getInputStream();
  }

  /**
   * This implementation returns a description that includes the passed-in
   * description, if any.
   */
  @Override
  public String toString() {
    return "InputStream resource [%s]".formatted(this.description);
  }

  /**
   * This implementation compares the underlying InputStream.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof InputStreamResource that &&
            this.equality.equals(that.equality)));
  }

  /**
   * This implementation returns the hash code of the underlying InputStream.
   */
  @Override
  public int hashCode() {
    return this.equality.hashCode();
  }

}
