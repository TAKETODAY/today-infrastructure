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

package infra.beans;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 10.06.2003
 */
public class BooleanTestBean {

  private boolean bool1;

  private Boolean bool2;

  public boolean isBool1() {
    return bool1;
  }

  public void setBool1(boolean bool1) {
    this.bool1 = bool1;
  }

  public Boolean getBool2() {
    return bool2;
  }

  public void setBool2(Boolean bool2) {
    this.bool2 = bool2;
  }

}
