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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import infra.lang.Constant;

/**
 * Simple {@link Resource} implementation that holds a resource description
 * but does not point to an actually readable resource.
 *
 * <p>To be used as placeholder if a {@code Resource} argument is
 * expected by an API but not necessarily used for actual reading.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/3/9 20:12
 * @since 3.0
 */
public class DescriptiveResource extends AbstractResource {

  private final String description;

  /**
   * Create a new DescriptiveResource.
   *
   * @param description the resource description
   */
  public DescriptiveResource(@Nullable String description) {
    this.description = (description != null ? description : Constant.BLANK);
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new FileNotFoundException(this + " cannot be opened because it does not point to a readable resource");
  }

  @Override
  public String toString() {
    return this.description;
  }

  /**
   * This implementation compares the underlying description String.
   */
  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof DescriptiveResource &&
            ((DescriptiveResource) other).description.equals(this.description)));
  }

  /**
   * This implementation returns the hash code of the underlying description String.
   */
  @Override
  public int hashCode() {
    return this.description.hashCode();
  }

}
