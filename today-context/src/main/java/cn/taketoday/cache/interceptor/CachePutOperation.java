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

package cn.taketoday.cache.interceptor;

import cn.taketoday.lang.Nullable;

/**
 * Class describing a cache 'put' operation.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Marcin Kamionowski
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
