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

package infra.expression.spel.testresources;

public class RecordPerson {

  private final String name;

  private Company company;

  public RecordPerson(String name) {
    this.name = name;
  }

  public RecordPerson(String name, Company company) {
    this.name = name;
    this.company = company;
  }

  public String name() {
    return name;
  }

  public Company company() {
    return company;
  }

}
