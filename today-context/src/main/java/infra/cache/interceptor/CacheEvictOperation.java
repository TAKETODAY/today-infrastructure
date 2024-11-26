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

package infra.cache.interceptor;

/**
 * Class describing a cache 'evict' operation.
 *
 * @author Costin Leau
 * @author Marcin Kamionowski
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CacheEvictOperation extends CacheOperation {

  private final boolean cacheWide;

  private final boolean beforeInvocation;

  /**
   * Create a new {@link CacheEvictOperation} instance from the given builder.
   */
  public CacheEvictOperation(Builder b) {
    super(b);
    this.cacheWide = b.cacheWide;
    this.beforeInvocation = b.beforeInvocation;
  }

  public boolean isCacheWide() {
    return this.cacheWide;
  }

  public boolean isBeforeInvocation() {
    return this.beforeInvocation;
  }

  /**
   * A builder that can be used to create a {@link CacheEvictOperation}.
   */
  public static class Builder extends CacheOperation.Builder {

    private boolean cacheWide = false;

    private boolean beforeInvocation = false;

    public void setCacheWide(boolean cacheWide) {
      this.cacheWide = cacheWide;
    }

    public void setBeforeInvocation(boolean beforeInvocation) {
      this.beforeInvocation = beforeInvocation;
    }

    @Override
    protected StringBuilder getOperationDescription() {
      StringBuilder sb = super.getOperationDescription();
      sb.append(',');
      sb.append(this.cacheWide);
      sb.append(',');
      sb.append(this.beforeInvocation);
      return sb;
    }

    @Override
    public CacheEvictOperation build() {
      return new CacheEvictOperation(this);
    }
  }

}
