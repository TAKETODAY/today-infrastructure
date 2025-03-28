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

package infra.jmx.export.naming;

/**
 * @author Rob Harrop
 */
public class KeyNamingStrategyTests extends AbstractNamingStrategyTests {

  private static final String OBJECT_NAME = "spring:name=test";

  @Override
  protected ObjectNamingStrategy getStrategy() throws Exception {
    return new KeyNamingStrategy();
  }

  @Override
  protected Object getManagedResource() {
    return new Object();
  }

  @Override
  protected String getKey() {
    return OBJECT_NAME;
  }

  @Override
  protected String getCorrectObjectName() {
    return OBJECT_NAME;
  }

}
