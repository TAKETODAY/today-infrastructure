/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.persistence.model;

import java.util.Objects;

import infra.core.style.ToStringBuilder;
import infra.persistence.Table;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/20 21:18
 */
@Table("t_no_id")
public class NoIdModel {

  public Integer age;

  public String name;

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NoIdModel noIdModel))
      return false;
    return Objects.equals(age, noIdModel.age) && Objects.equals(name, noIdModel.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(age, name);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("age", age)
            .append("name", name)
            .toString();
  }
}
