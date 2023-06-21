/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.orm.jpa.domain;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Simple JavaBean domain object representing an person.
 *
 * @author Rod Johnson
 */
@Entity
@EntityListeners(PersonListener.class)
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  private transient TestBean testBean;

  // Lazy relationship to force use of instrumentation in JPA implementation.
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "DRIVERS_LICENSE_ID")
  private DriversLicense driversLicense;

  private String first_name;

  @Basic(fetch = FetchType.LAZY)
  private String last_name;

  public transient ApplicationContext postLoaded;

  public Integer getId() {
    return id;
  }

  public void setTestBean(TestBean testBean) {
    this.testBean = testBean;
  }

  public TestBean getTestBean() {
    return testBean;
  }

  public void setFirstName(String firstName) {
    this.first_name = firstName;
  }

  public String getFirstName() {
    return this.first_name;
  }

  public void setLastName(String lastName) {
    this.last_name = lastName;
  }

  public String getLastName() {
    return this.last_name;
  }

  public void setDriversLicense(DriversLicense driversLicense) {
    this.driversLicense = driversLicense;
  }

  public DriversLicense getDriversLicense() {
    return this.driversLicense;
  }

  @Override
  public String toString() {
    return getClass().getName() + ":(" + hashCode() + ") id=" + id + "; firstName=" + first_name +
            "; lastName=" + last_name + "; testBean=" + testBean;
  }

}
