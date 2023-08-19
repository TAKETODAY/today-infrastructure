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

/**
 * @author Juergen Hoeller
 */
public class ConstructorPersonWithSetters {

  private String name;

  private long age;

  private Date birthDate;

  private BigDecimal balance;

  public ConstructorPersonWithSetters(String name, long age, BigDecimal balance) {
    this.name = name.toUpperCase();
    this.age = age;
    this.balance = balance;
  }

  public void setName(String name) {
    throw new UnsupportedOperationException();
  }

  public void setAge(long age) {
    throw new UnsupportedOperationException();
  }

  public void setBirthDate(Date birthDate) {
    this.birthDate = birthDate;
  }

  public void setBalance(BigDecimal balance) {
    throw new UnsupportedOperationException();
  }

  public String name() {
    return this.name;
  }

  public long age() {
    return this.age;
  }

  public Date birthDate() {
    return this.birthDate;
  }

  public BigDecimal balance() {
    return this.balance;
  }

}
