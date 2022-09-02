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
package cn.taketoday.core.annotation;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Thrown by {@link AnnotationUtils} and <em>synthesized annotations</em>
 * if an annotation is improperly configured.
 *
 * @author Sam Brannen
 * @author TODAY 2021/10/19 14:25
 * @see AnnotationUtils
 * @see AnnotationUtils#isSynthesizedAnnotation(java.lang.annotation.Annotation)
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AnnotationConfigurationException extends NestedRuntimeException {

  /**
   * Construct a new {@code AnnotationConfigurationException} with the
   * supplied message.
   *
   * @param message the detail message
   */
  public AnnotationConfigurationException(String message) {
    super(message);
  }

  /**
   * Construct a new {@code AnnotationConfigurationException} with the
   * supplied message and cause.
   *
   * @param message the detail message
   * @param cause the root cause
   */
  public AnnotationConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
