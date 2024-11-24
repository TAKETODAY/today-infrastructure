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

package infra.validation;

import infra.beans.testfixture.beans.TestBean;

/**
 * @author Juergen Hoeller
 * @since 07.03.2006
 */
public class FieldAccessBean {

  public String name;

  protected int age;

  private TestBean spouse;

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }

  public TestBean getSpouse() {
    return spouse;
  }

}
