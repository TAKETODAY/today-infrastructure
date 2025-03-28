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
package infra.bytecode.util;

import java.util.Map;

import infra.bytecode.Attribute;
import infra.bytecode.Label;

/**
 * An {@link Attribute} that can generate the ASM code to create an equivalent
 * attribute.
 *
 * @author Eugene Kuleshov
 */
// DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
public interface ASMifierSupport {

  /**
   * Generates the ASM code to create an attribute equal to this attribute.
   *
   * @param outputBuilder where the generated code must be appended.
   * @param visitorVariableName the name of the visitor variable in the produced code.
   * @param labelNames the names of the labels in the generated code.
   */
  void asmify(StringBuilder outputBuilder, String visitorVariableName, Map<Label, String> labelNames);
}
