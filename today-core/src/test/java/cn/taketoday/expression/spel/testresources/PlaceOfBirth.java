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

package cn.taketoday.expression.spel.testresources;

///CLOVER:OFF
public class PlaceOfBirth {
  private String city;

  public String Country;

  /**
   * Keith now has a converter that supports String to X, if X has a ctor that takes a String.
   * In order for round tripping to work we need toString() for X to return what it was
   * constructed with.  This is a bit of a hack because a PlaceOfBirth also encapsulates a
   * country - but as it is just a test object, it is ok.
   */
  @Override
  public String toString() { return city; }

  public String getCity() {
    return city;
  }

  public void setCity(String s) {
    this.city = s;
  }

  public PlaceOfBirth(String string) {
    this.city = string;
  }

  public int doubleIt(int i) {
    return i * 2;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PlaceOfBirth)) {
      return false;
    }
    PlaceOfBirth oPOB = (PlaceOfBirth) o;
    return (city.equals(oPOB.city));
  }

  @Override
  public int hashCode() {
    return city.hashCode();
  }

}
