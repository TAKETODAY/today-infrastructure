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

package cn.taketoday.test.context.testng;

import org.testng.annotations.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for
 * {@link cn.taketoday.context.annotation.Configuration @Configuration} classes
 * with TestNG-based tests.
 *
 * <p>Configuration will be loaded from
 * {@link Config}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class AnnotationConfigTestNGContextTests extends AbstractTestNGContextTests {

  @Autowired
  Employee employee;

  @Autowired
  Pet pet;

  @Test
  public void autowiringFromConfigClass() {
    assertThat(employee).as("The employee should have been autowired.").isNotNull();
    assertThat(employee.getName()).isEqualTo("John Smith");

    assertThat(pet).as("The pet should have been autowired.").isNotNull();
    assertThat(pet.getName()).isEqualTo("Fido");
  }

  @Configuration
  static class Config {

    @Bean
    Employee employee() {
      return new Employee("John Smith");
    }

    @Bean
    Pet pet() {
      return new Pet("Fido");
    }

  }

}
