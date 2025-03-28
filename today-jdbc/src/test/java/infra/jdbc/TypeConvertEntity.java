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

package infra.jdbc;

/**
 * Created by IntelliJ IDEA. User: lars Date: 9/5/11 Time: 1:40 PM To change
 * this template use File | Settings | File Templates.
 */
public class TypeConvertEntity {

  public int val1;
  private Long val2;

  public long getVal2() {
    return val2;
  }

  public void setVal2(long val2) {
    this.val2 = val2;
  }
}
