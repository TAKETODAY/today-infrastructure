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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PreRemove;

@Entity
@IdClass(EmployeeId.class)
@Convert(converter = EmployeeKindConverter.class, attributeName = "kind")
public class Employee {

  @Id
  @Column
  private String name;

  @Id
  @Column
  private String department;

  private EmployeeLocation location;

  @Convert(converter = EmployeeCategoryConverter.class)
  private EmployeeCategory category;

  private EmployeeKind kind;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public EmployeeLocation getLocation() {
    return location;
  }

  public void setLocation(EmployeeLocation location) {
    this.location = location;
  }

  public EmployeeCategory getCategory() {
    return category;
  }

  public void setCategory(EmployeeCategory category) {
    this.category = category;
  }

  public EmployeeKind getKind() {
    return kind;
  }

  public void setKind(EmployeeKind kind) {
    this.kind = kind;
  }

  @PreRemove
  public void preRemove() {
  }
}
