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

package infra.beans.factory.parsing;

/**
 * {@link ParseState} entry representing a bean definition.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class BeanEntry implements ParseState.Entry {

  private final String beanDefinitionName;

  /**
   * Create a new {@code BeanEntry} instance.
   *
   * @param beanDefinitionName the name of the associated bean definition
   */
  public BeanEntry(String beanDefinitionName) {
    this.beanDefinitionName = beanDefinitionName;
  }

  @Override
  public String toString() {
    return "Bean '" + this.beanDefinitionName + "'";
  }

}
