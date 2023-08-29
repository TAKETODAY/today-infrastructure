/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.sample.simple;

import java.util.Map;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Properties with array.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("array")
public class SimpleArrayProperties {

  private int[] primitive;

  private String[] simple;

  private Holder[] inner;

  private Map<String, Integer>[] nameToInteger;

  public int[] getPrimitive() {
    return this.primitive;
  }

  public void setPrimitive(int[] primitive) {
    this.primitive = primitive;
  }

  public String[] getSimple() {
    return this.simple;
  }

  public void setSimple(String[] simple) {
    this.simple = simple;
  }

  public Holder[] getInner() {
    return this.inner;
  }

  public void setInner(Holder[] inner) {
    this.inner = inner;
  }

  public Map<String, Integer>[] getNameToInteger() {
    return this.nameToInteger;
  }

  public void setNameToInteger(Map<String, Integer>[] nameToInteger) {
    this.nameToInteger = nameToInteger;
  }

  static class Holder {

  }

}
