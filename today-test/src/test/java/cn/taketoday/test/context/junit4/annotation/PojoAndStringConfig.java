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

package cn.taketoday.test.context.junit4.annotation;

import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

/**
 * ApplicationContext configuration class for various integration tests.
 *
 * <p>The beans defined in this configuration class map directly to the
 * beans defined in {@code SpringJUnit4ClassRunnerAppCtxTests-context.xml}.
 * Consequently, the application contexts loaded from these two sources
 * should be identical with regard to bean definitions.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Configuration
public class PojoAndStringConfig {

  @Bean
  public Employee employee() {
    Employee employee = new Employee();
    employee.setName("John Smith");
    employee.setAge(42);
    employee.setCompany("Acme Widgets, Inc.");
    return employee;
  }

  @Bean
  public Pet pet() {
    return new Pet("Fido");
  }

  @Bean
  public String foo() {
    return "Foo";
  }

  @Bean
  public String bar() {
    return "Bar";
  }

  @Bean
  public String quux() {
    return "Quux";
  }

}
