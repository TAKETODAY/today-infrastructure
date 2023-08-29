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

import java.beans.FeatureDescriptor;
import java.util.Comparator;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Simple properties.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "simple")
public class SimpleProperties {

  /**
   * The name of this simple properties.
   */
  private String theName = "boot";

  // isFlag is also detected
  /**
   * A simple flag.
   */
  private boolean flag;

  // An interface can still be injected because it might have a converter
  private Comparator<?> comparator;

  // There is only a getter on this instance but we don't know what to do with it ->
  // ignored
  private FeatureDescriptor featureDescriptor;

  // There is only a setter on this "simple" property --> ignored
  @SuppressWarnings("unused")
  private Long counter;

  // There is only a getter on this "simple" property --> ignored
  private Integer size;

  public String getTheName() {
    return this.theName;
  }

  @Deprecated
  public void setTheName(String name) {
    this.theName = name;
  }

  @Deprecated
  public boolean isFlag() {
    return this.flag;
  }

  public void setFlag(boolean flag) {
    this.flag = flag;
  }

  public Comparator<?> getComparator() {
    return this.comparator;
  }

  public void setComparator(Comparator<?> comparator) {
    this.comparator = comparator;
  }

  public FeatureDescriptor getFeatureDescriptor() {
    return this.featureDescriptor;
  }

  public void setCounter(Long counter) {
    this.counter = counter;
  }

  public Integer getSize() {
    return this.size;
  }

}
