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

import cn.taketoday.core.conversion.ConverterRegistry;

/**
 * A registry of field formatting logic.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface FormatterRegistry extends ConverterRegistry {

  /**
   * Adds a Printer to print fields of a specific type.
   * The field type is implied by the parameterized Printer instance.
   *
   * @param printer the printer to add
   * @see #addFormatter(Formatter)
   */
  void addPrinter(Printer<?> printer);

  /**
   * Adds a Parser to parse fields of a specific type.
   * The field type is implied by the parameterized Parser instance.
   *
   * @param parser the parser to add
   * @see #addFormatter(Formatter)
   */
  void addParser(Parser<?> parser);

  /**
   * Adds a Formatter to format fields of a specific type.
   * The field type is implied by the parameterized Formatter instance.
   *
   * @param formatter the formatter to add
   * @see #addFormatterForFieldType(Class, Formatter)
   */
  void addFormatter(Formatter<?> formatter);

  /**
   * Adds a Formatter to format fields of the given type.
   * <p>On print, if the Formatter's type T is declared and {@code fieldType} is not assignable to T,
   * a coercion to T will be attempted before delegating to {@code formatter} to print a field value.
   * On parse, if the parsed object returned by {@code formatter} is not assignable to the runtime field type,
   * a coercion to the field type will be attempted before returning the parsed field value.
   *
   * @param fieldType the field type to format
   * @param formatter the formatter to add
   */
  void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

  /**
   * Adds a Printer/Parser pair to format fields of a specific type.
   * The formatter will delegate to the specified {@code printer} for printing
   * and the specified {@code parser} for parsing.
   * <p>On print, if the Printer's type T is declared and {@code fieldType} is not assignable to T,
   * a coercion to T will be attempted before delegating to {@code printer} to print a field value.
   * On parse, if the object returned by the Parser is not assignable to the runtime field type,
   * a coercion to the field type will be attempted before returning the parsed field value.
   *
   * @param fieldType the field type to format
   * @param printer the printing part of the formatter
   * @param parser the parsing part of the formatter
   */
  void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

  /**
   * Adds a Formatter to format fields annotated with a specific format annotation.
   *
   * @param annotationFormatterFactory the annotation formatter factory to add
   */
  void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);

}
