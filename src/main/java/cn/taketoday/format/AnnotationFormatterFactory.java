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

package cn.taketoday.format;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * A factory that creates formatters to format values of fields annotated with a particular
 * {@link Annotation}.
 *
 * <p>For example, a {@code DateTimeFormatAnnotationFormatterFactory} might create a formatter
 * that formats {@code Date} values set on fields annotated with {@code @DateTimeFormat}.
 *
 * @param <A> the annotation type that should trigger formatting
 * @author Keith Donald
 * @since 4.0
 */
public interface AnnotationFormatterFactory<A extends Annotation> {

  /**
   * The types of fields that may be annotated with the &lt;A&gt; annotation.
   */
  Set<Class<?>> getFieldTypes();

  /**
   * Get the Printer to print the value of a field of {@code fieldType} annotated with
   * {@code annotation}.
   * <p>If the type T the printer accepts is not assignable to {@code fieldType}, a
   * coercion from {@code fieldType} to T will be attempted before the Printer is invoked.
   *
   * @param annotation the annotation instance
   * @param fieldType the type of field that was annotated
   * @return the printer
   */
  Printer<?> getPrinter(A annotation, Class<?> fieldType);

  /**
   * Get the Parser to parse a submitted value for a field of {@code fieldType}
   * annotated with {@code annotation}.
   * <p>If the object the parser returns is not assignable to {@code fieldType},
   * a coercion to {@code fieldType} will be attempted before the field is set.
   *
   * @param annotation the annotation instance
   * @param fieldType the type of field that was annotated
   * @return the parser
   */
  Parser<?> getParser(A annotation, Class<?> fieldType);

}
