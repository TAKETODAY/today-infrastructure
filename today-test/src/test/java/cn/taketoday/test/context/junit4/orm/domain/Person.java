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

package cn.taketoday.test.context.junit4.orm.domain;

/**
 * Person POJO.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class Person {

  private Long id;
  private String name;
  private DriversLicense driversLicense;

  public Person() {
  }

  public Person(Long id) {
    this(id, null, null);
  }

  public Person(String name) {
    this(name, null);
  }

  public Person(String name, DriversLicense driversLicense) {
    this(null, name, driversLicense);
  }

  public Person(Long id, String name, DriversLicense driversLicense) {
    this.id = id;
    this.name = name;
    this.driversLicense = driversLicense;
  }

  public Long getId() {
    return this.id;
  }

  protected void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DriversLicense getDriversLicense() {
    return this.driversLicense;
  }

  public void setDriversLicense(DriversLicense driversLicense) {
    this.driversLicense = driversLicense;
  }
}
