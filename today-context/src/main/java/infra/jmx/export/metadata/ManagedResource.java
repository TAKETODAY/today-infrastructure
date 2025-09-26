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
import infra.jmx.export.naming.MetadataNamingStrategy;

/**
 * Metadata indicating that instances of an annotated class
 * are to be registered with a JMX server.
 * Only valid when used on a {@code Class}.
 *
 * @author Rob Harrop
 * @see MetadataMBeanInfoAssembler
 * @see MetadataNamingStrategy
 * @see MBeanExporter
 * @since 4.0
 */
public class ManagedResource extends AbstractJmxAttribute {

  @Nullable
  private String objectName;

  private boolean log = false;

  @Nullable
  private String logFile;

  @Nullable
  private String persistPolicy;

  private int persistPeriod = -1;

  @Nullable
  private String persistName;

  @Nullable
  private String persistLocation;

  /**
   * Set the JMX ObjectName of this managed resource.
   */
  public void setObjectName(@Nullable String objectName) {
    this.objectName = objectName;
  }

  /**
   * Return the JMX ObjectName of this managed resource.
   */
  @Nullable
  public String getObjectName() {
    return this.objectName;
  }

  public void setLog(boolean log) {
    this.log = log;
  }

  public boolean isLog() {
    return this.log;
  }

  public void setLogFile(@Nullable String logFile) {
    this.logFile = logFile;
  }

  @Nullable
  public String getLogFile() {
    return this.logFile;
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

  public void setPersistName(@Nullable String persistName) {
    this.persistName = persistName;
  }

  @Nullable
  public String getPersistName() {
    return this.persistName;
  }

  public void setPersistLocation(@Nullable String persistLocation) {
    this.persistLocation = persistLocation;
  }

  @Nullable
  public String getPersistLocation() {
    return this.persistLocation;
  }

}
