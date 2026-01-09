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

package infra.cache.interceptor;

import org.jspecify.annotations.Nullable;

/**
 * Class describing a cache 'put' operation.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Marcin Kamionowski
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CachePutOperation extends CacheOperation {

  @Nullable
  private final String unless;

  /**
   * Create a new {@link CachePutOperation} instance from the given builder.
   *
   * @since 4.0
   */
  public CachePutOperation(Builder b) {
    super(b);
    this.unless = b.unless;
  }

  @Nullable
  public String getUnless() {
    return this.unless;
  }

  /**
   * A builder that can be used to create a {@link CachePutOperation}.
   *
   * @since 4.0
   */
  public static class Builder extends CacheOperation.Builder {

    @Nullable
    private String unless;

    public void setUnless(String unless) {
      this.unless = unless;
    }

    @Override
    protected StringBuilder getOperationDescription() {
      StringBuilder sb = super.getOperationDescription();
      sb.append(" | unless='");
      sb.append(this.unless);
      sb.append('\'');
      return sb;
    }

    @Override
    public CachePutOperation build() {
      return new CachePutOperation(this);
    }
  }

}
