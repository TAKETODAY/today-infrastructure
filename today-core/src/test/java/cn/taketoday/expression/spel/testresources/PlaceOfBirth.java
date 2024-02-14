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

package cn.taketoday.expression.spel.testresources;

import cn.taketoday.lang.Nullable;

public class PlaceOfBirth {

  private String city;

  public String Country;

  public PlaceOfBirth(String city) {
    this.city = city;
  }

  public String getCity() {
    return this.city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public int doubleIt(int i) {
    return i * 2;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return (o instanceof PlaceOfBirth that && this.city.equals(that.city));
  }

  @Override
  public int hashCode() {
    return this.city.hashCode();
  }

  /**
   * ObjectToObjectConverter supports String to X conversions, if X has a
   * constructor that takes a String.
   * <p>In order for round-tripping to work, we need toString() for PlaceOfBirth
   * to return what it was constructed with. This is a bit of a hack, because a
   * PlaceOfBirth also encapsulates a country, but as it is just a test object,
   * it is OK.
   */
  @Override
  public String toString() {
    return this.city;
  }

}
