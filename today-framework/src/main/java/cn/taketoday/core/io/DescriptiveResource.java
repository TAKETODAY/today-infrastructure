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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.lang.Constant;

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
  public DescriptiveResource(String description) {
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
