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

package infra.jmx;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public interface IJmxTestBean {

  int add(int x, int y);

  long myOperation();

  int getAge();

  void setAge(int age);

  void setName(String name) throws Exception;

  String getName();

  // used to test invalid methods that exist in the proxy interface
  void dontExposeMe();

}
