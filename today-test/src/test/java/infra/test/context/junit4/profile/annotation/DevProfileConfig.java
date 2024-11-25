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

package infra.test.context.junit4.profile.annotation;

import infra.beans.testfixture.beans.Employee;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@Profile("dev")
@Configuration
public class DevProfileConfig {

  @Bean
  public Employee employee() {
    Employee employee = new Employee();
    employee.setName("John Smith");
    employee.setAge(42);
    employee.setCompany("Acme Widgets, Inc.");
    return employee;
  }

}