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

package infra.aop.aspectj.autoproxy;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
class AnnotatedTestBeanImpl implements AnnotatedTestBean {

  @Override
  @TestAnnotation("this value")
  public String doThis() {
    return "doThis";
  }

  @Override
  @TestAnnotation("that value")
  public String doThat() {
    return "doThat";
  }

  @Override
  @TestAnnotation("array value")
  public String[] doArray() {
    return new String[] { "doThis", "doThat" };
  }

  // not annotated
  @Override
  public String doTheOther() {
    return "doTheOther";
  }

}
