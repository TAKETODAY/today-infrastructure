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

package cn.taketoday.http.codec;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Sebastien Deleuze
 */
@XmlRootElement
public class Pojo {

  private String foo;

  private String bar;

  public Pojo() {
  }

  public Pojo(String foo, String bar) {
    this.foo = foo;
    this.bar = bar;
  }

  public String getFoo() {
    return this.foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

  public String getBar() {
    return this.bar;
  }

  public void setBar(String bar) {
    this.bar = bar;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Pojo) {
      Pojo other = (Pojo) o;
      return this.foo.equals(other.foo) && this.bar.equals(other.bar);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * foo.hashCode() + bar.hashCode();
  }

  @Override
  public String toString() {
    return "Pojo[foo='" + this.foo + "\'" + ", bar='" + this.bar + "\']";
  }
}
