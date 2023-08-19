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

package cn.taketoday.jdbc.core.test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Juergen Hoeller
 */
public class ConstructorPersonWithGenerics {

  private final String name;

  private final long age;

  private final Date bd;

  private final List<BigDecimal> balance;

  public ConstructorPersonWithGenerics(String name, long age, Date birthDate, List<BigDecimal> balance) {
    this.name = name;
    this.age = age;
    this.bd = birthDate;
    this.balance = balance;
  }

  public String name() {
    return this.name;
  }

  public long age() {
    return this.age;
  }

  public Date birthDate() {
    return this.bd;
  }

  public List<BigDecimal> balance() {
    return this.balance;
  }

}
