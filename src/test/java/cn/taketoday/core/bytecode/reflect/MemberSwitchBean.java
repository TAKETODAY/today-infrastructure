/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.reflect;

public class MemberSwitchBean {
  public int init = -1;

  public MemberSwitchBean() {
    init = 0;
  }

  public MemberSwitchBean(double foo) {
    init = 1;
  }

  public MemberSwitchBean(int foo) {
    init = 2;
  }

  public MemberSwitchBean(int foo, String bar, String baz) {
    init = 3;
  }

  public MemberSwitchBean(int foo, String bar, double baz) {
    init = 4;
  }

  public MemberSwitchBean(int foo, short bar, long baz) {
    init = 5;
  }

  public MemberSwitchBean(int foo, String bar) {
    init = 6;
  }

  public int foo() {
    return 0;
  }

  public int foo(double foo) {
    return 1;
  }

  public int foo(int foo) {
    return 2;
  }

  public int foo(int foo, String bar, String baz) {
    return 3;
  }

  public int foo(int foo, String bar, double baz) {
    return 4;
  }

  public int foo(int foo, short bar, long baz) {
    return 5;
  }

  public int foo(int foo, String bar) {
    return 6;
  }

  public int bar() {
    return 7;
  }

  public int bar(double foo) {
    return 8;
  }

  int pkg() {
    return 9;
  }

  public static int staticMethod() {
    return 10;
  }
}
