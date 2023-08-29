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

package cn.taketoday.context.properties.sample.immutable;

import java.util.Comparator;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.ConstructorBinding;
import cn.taketoday.context.properties.sample.DefaultValue;

/**
 * Simple properties, in immutable format.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("immutable")
public class ImmutableSimpleProperties {

  /**
   * The name of this simple properties.
   */
  private final String theName;

  /**
   * A simple flag.
   */
  private final boolean flag;

  // An interface can still be injected because it might have a converter
  private final Comparator<?> comparator;

  // Even if it is not exposed, we're still offering a way to bind the value through the
  // constructor, so it should be present in the metadata
  @SuppressWarnings("unused")
  private final Long counter;

  @ConstructorBinding
  public ImmutableSimpleProperties(@DefaultValue("boot") String theName, boolean flag, Comparator<?> comparator,
          Long counter) {
    this.theName = theName;
    this.flag = flag;
    this.comparator = comparator;
    this.counter = counter;
  }

  public String getTheName() {
    return this.theName;
  }

  @Deprecated
  public boolean isFlag() {
    return this.flag;
  }

  public Comparator<?> getComparator() {
    return this.comparator;
  }

}
