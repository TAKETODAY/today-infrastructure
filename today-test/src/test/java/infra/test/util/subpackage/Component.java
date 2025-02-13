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

package infra.test.util.subpackage;

import infra.beans.factory.annotation.Autowired;
import infra.lang.Assert;
import infra.util.StringUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Simple POJO representing a <em>component</em>; intended for use in
 * unit tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class Component {

  private Integer number;
  private String text;

  public Integer getNumber() {
    return this.number;
  }

  public String getText() {
    return this.text;
  }

  @Autowired
  protected void configure(Integer number, String text) {
    this.number = number;
    this.text = text;
  }

  @PostConstruct
  protected void init() {
    Assert.state(number != null, "number is required");
    Assert.state(StringUtils.hasText(text), "text must not be empty");
  }

  @PreDestroy
  protected void destroy() {
    this.number = null;
    this.text = null;
  }

  int subtract(int a, int b) {
    return a - b;
  }

  int add(int... args) {
    int sum = 0;
    for (int arg : args) {
      sum += arg;
    }
    return sum;
  }

  int multiply(Integer... args) {
    int product = 1;
    for (Integer arg : args) {
      product *= arg;
    }
    return product;
  }

}
