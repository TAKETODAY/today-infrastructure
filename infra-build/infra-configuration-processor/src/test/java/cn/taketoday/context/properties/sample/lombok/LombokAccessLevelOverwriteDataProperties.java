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

package cn.taketoday.context.properties.sample.lombok;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties using Lombok @Data on element level and overwriting behaviour
 * with @Getter and @Setter at field level.
 *
 * @author Jonas Keßler
 */
@Data
@ConfigurationProperties(prefix = "accesslevel.overwrite.data")
@SuppressWarnings("unused")
public class LombokAccessLevelOverwriteDataProperties {

  private String name0;

  @Getter(AccessLevel.PUBLIC)
  @Setter(AccessLevel.PUBLIC)
  private String name1;

  @Getter(AccessLevel.PUBLIC)
  private String name2;

  @Setter(AccessLevel.PUBLIC)
  private String name3;

  @Getter
  @Setter
  private String name4;

  @Getter
  private String name5;

  @Setter
  private String name6;

  /*
   * AccessLevel.NONE
   */
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String ignoredAccessLevelNone;

  @Getter(AccessLevel.NONE)
  private String ignoredGetterAccessLevelNone;

  @Setter(AccessLevel.NONE)
  private String ignoredSetterAccessLevelNone;

  /*
   * AccessLevel.PRIVATE
   */
  @Getter(AccessLevel.PRIVATE)
  @Setter(AccessLevel.PRIVATE)
  private String ignoredAccessLevelPrivate;

  @Getter(AccessLevel.PRIVATE)
  private String ignoredGetterAccessLevelPrivate;

  @Setter(AccessLevel.PRIVATE)
  private String ignoredSetterAccessLevelPrivate;

  /*
   * AccessLevel.PACKAGE
   */
  @Getter(AccessLevel.PACKAGE)
  @Setter(AccessLevel.PACKAGE)
  private String ignoredAccessLevelPackage;

  @Getter(AccessLevel.PACKAGE)
  private String ignoredGetterAccessLevelPackage;

  @Setter(AccessLevel.PACKAGE)
  private String ignoredSetterAccessLevelPackage;

  /*
   * AccessLevel.PROTECTED
   */
  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  private String ignoredAccessLevelProtected;

  @Getter(AccessLevel.PROTECTED)
  private String ignoredGetterAccessLevelProtected;

  @Setter(AccessLevel.PROTECTED)
  private String ignoredSetterAccessLevelProtected;

  /*
   * AccessLevel.MODULE
   */
  @Getter(AccessLevel.MODULE)
  @Setter(AccessLevel.MODULE)
  private String ignoredAccessLevelModule;

  @Getter(AccessLevel.MODULE)
  private String ignoredGetterAccessLevelModule;

  @Setter(AccessLevel.MODULE)
  private String ignoredSetterAccessLevelModule;

}
