/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core.annotation;

import infra.core.NestedRuntimeException;

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
