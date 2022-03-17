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

package cn.taketoday.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.jmx.support.MetricType;

/**
 * Method-level annotation that indicates to expose a given bean property as a
 * JMX attribute, with added descriptor properties to indicate that it is a metric.
 * Only valid when used on a JavaBean getter.
 *
 * @author Jennifer Hickey
 * @see cn.taketoday.jmx.export.metadata.ManagedMetric
 * @since 4.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedMetric {

  String category() default "";

  int currencyTimeLimit() default -1;

  String description() default "";

  String displayName() default "";

  MetricType metricType() default MetricType.GAUGE;

  int persistPeriod() default -1;

  String persistPolicy() default "";

  String unit() default "";

}
