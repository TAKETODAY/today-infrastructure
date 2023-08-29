/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.sample.generic;

/**
 * A configuration properties that uses the builder pattern with a generic.
 *
 * @param <T> the type of the return type
 * @author Stephane Nicoll
 */
public class GenericBuilderProperties<T extends GenericBuilderProperties<T>> {

  private int number;

  public int getNumber() {
    return this.number;
  }

  @SuppressWarnings("unchecked")
  public T setNumber(int number) {
    this.number = number;
    return (T) this;
  }

}
