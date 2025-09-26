/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jmx.export.metadata;

import org.jspecify.annotations.Nullable;

import infra.jmx.export.MBeanExporter;
import infra.jmx.export.assembler.MetadataMBeanInfoAssembler;

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
