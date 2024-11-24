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


package infra.context.testfixture.context.annotation;

import infra.beans.factory.annotation.Autowired;
import infra.core.env.Environment;

public class AutowiredComponent {

  private Environment environment;

  private Integer counter;

  public Environment getEnvironment() {
    return this.environment;
  }

  @Autowired
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public Integer getCounter() {
    return this.counter;
  }

  @Autowired
  public void setCounter(Integer counter) {
    this.counter = counter;
  }

}
