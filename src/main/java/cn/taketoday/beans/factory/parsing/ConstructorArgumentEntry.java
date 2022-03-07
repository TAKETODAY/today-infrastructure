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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.lang.Assert;

/**
 * {@link ParseState} entry representing a (possibly indexed)
 * constructor argument.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ConstructorArgumentEntry implements ParseState.Entry {

  private final int index;

  /**
   * Creates a new instance of the {@link ConstructorArgumentEntry} class
   * representing a constructor argument with a (currently) unknown index.
   */
  public ConstructorArgumentEntry() {
    this.index = -1;
  }

  /**
   * Creates a new instance of the {@link ConstructorArgumentEntry} class
   * representing a constructor argument at the supplied {@code index}.
   *
   * @param index the index of the constructor argument
   * @throws IllegalArgumentException if the supplied {@code index}
   * is less than zero
   */
  public ConstructorArgumentEntry(int index) {
    Assert.isTrue(index >= 0, "Constructor argument index must be greater than or equal to zero");
    this.index = index;
  }

  @Override
  public String toString() {
    return "Constructor-arg" + (this.index >= 0 ? " #" + this.index : "");
  }

}
