/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jmx.export.metadata;

import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.jmx.export.assembler.MetadataMBeanInfoAssembler;
import cn.taketoday.lang.Nullable;

/**
 * Metadata that indicates to expose a given bean property as JMX attribute.
 * Only valid when used on a JavaBean getter or setter.
 *
 * @author Rob Harrop
 * @see MetadataMBeanInfoAssembler
 * @see MBeanExporter
 * @since 4.0
 */
public class ManagedAttribute extends AbstractJmxAttribute {

  /**
   * Empty attributes.
   */
  public static final ManagedAttribute EMPTY = new ManagedAttribute();

  @Nullable
  private Object defaultValue;

  @Nullable
  private String persistPolicy;

  private int persistPeriod = -1;

  /**
   * Set the default value of this attribute.
   */
  public void setDefaultValue(@Nullable Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Return the default value of this attribute.
   */
  @Nullable
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  public void setPersistPolicy(@Nullable String persistPolicy) {
    this.persistPolicy = persistPolicy;
  }

  @Nullable
  public String getPersistPolicy() {
    return this.persistPolicy;
  }

  public void setPersistPeriod(int persistPeriod) {
    this.persistPeriod = persistPeriod;
  }

  public int getPersistPeriod() {
    return this.persistPeriod;
  }

}
