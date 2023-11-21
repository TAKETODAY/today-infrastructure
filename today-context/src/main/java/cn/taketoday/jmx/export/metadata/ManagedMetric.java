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

import cn.taketoday.jmx.export.assembler.MetadataMBeanInfoAssembler;
import cn.taketoday.jmx.support.MetricType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Metadata that indicates to expose a given bean property as a JMX attribute,
 * with additional descriptor properties that indicate that the attribute is a
 * metric. Only valid when used on a JavaBean getter.
 *
 * @author Jennifer Hickey
 * @see MetadataMBeanInfoAssembler
 * @since 4.0
 */
public class ManagedMetric extends AbstractJmxAttribute {

  @Nullable
  private String category;

  @Nullable
  private String displayName;

  private MetricType metricType = MetricType.GAUGE;

  private int persistPeriod = -1;

  @Nullable
  private String persistPolicy;

  @Nullable
  private String unit;

  /**
   * The category of this metric (ex. throughput, performance, utilization).
   */
  public void setCategory(@Nullable String category) {
    this.category = category;
  }

  /**
   * The category of this metric (ex. throughput, performance, utilization).
   */
  @Nullable
  public String getCategory() {
    return this.category;
  }

  /**
   * A display name for this metric.
   */
  public void setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
  }

  /**
   * A display name for this metric.
   */
  @Nullable
  public String getDisplayName() {
    return this.displayName;
  }

  /**
   * A description of how this metric's values change over time.
   */
  public void setMetricType(MetricType metricType) {
    Assert.notNull(metricType, "MetricType is required");
    this.metricType = metricType;
  }

  /**
   * A description of how this metric's values change over time.
   */
  public MetricType getMetricType() {
    return this.metricType;
  }

  /**
   * The persist period for this metric.
   */
  public void setPersistPeriod(int persistPeriod) {
    this.persistPeriod = persistPeriod;
  }

  /**
   * The persist period for this metric.
   */
  public int getPersistPeriod() {
    return this.persistPeriod;
  }

  /**
   * The persist policy for this metric.
   */
  public void setPersistPolicy(@Nullable String persistPolicy) {
    this.persistPolicy = persistPolicy;
  }

  /**
   * The persist policy for this metric.
   */
  @Nullable
  public String getPersistPolicy() {
    return this.persistPolicy;
  }

  /**
   * The expected unit of measurement values.
   */
  public void setUnit(@Nullable String unit) {
    this.unit = unit;
  }

  /**
   * The expected unit of measurement values.
   */
  @Nullable
  public String getUnit() {
    return this.unit;
  }

}
