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

package infra.web.multipart;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

import infra.core.io.AbstractResource;
import infra.core.io.Resource;
import infra.lang.Assert;

/**
 * Adapt {@link Part} to {@link Resource},
 * exposing the content as {@code InputStream} and also overriding
 * {@link #contentLength()} as well as {@link #getName()}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Part#getResource()
 * @since 4.0 2022/4/2 11:58
 */
class PartResource extends AbstractResource {

  private final Part part;

  public PartResource(Part part) {
    Assert.notNull(part, "Part is required");
    this.part = part;
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
    return this.part.getContentLength();
  }

  @Nullable
  @Override
  public String getName() {
    return this.part.getOriginalFilename();
  }

  /**
   * This implementation throws IllegalStateException if attempting to
   * read the underlying stream multiple times.
   */
  @Override
  public InputStream getInputStream() throws IOException, IllegalStateException {
    return this.part.getInputStream();
  }

  /**
   * This implementation returns a description that has the Part name.
   */
  @Override
  public String toString() {
    return "Part resource [%s]".formatted(this.part.getName());
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other || (other instanceof PartResource && ((PartResource) other).part.equals(this.part));
  }

  @Override
  public int hashCode() {
    return this.part.hashCode();
  }

}
