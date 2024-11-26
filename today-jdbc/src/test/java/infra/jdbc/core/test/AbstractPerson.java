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

package infra.jdbc.core.test;

import java.util.Date;

/**
 * @author Thomas Risberg
 */
public abstract class AbstractPerson {

  private String name;

  private long age;

  private Date birth_date;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getAge() {
    return age;
  }

  public void setAge(long age) {
    this.age = age;
  }

  public Date getBirth_date() {
    return birth_date;
  }

  public void setBirth_date(Date birth_date) {
    this.birth_date = birth_date;
  }

}
