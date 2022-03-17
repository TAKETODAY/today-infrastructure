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
package cn.taketoday.core.bytecode.util;

import java.util.Map;

import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.Label;

/**
 * An {@link Attribute} that can print a readable representation of itself.
 *
 * @author Eugene Kuleshov
 */
public interface TextifierSupport {

  /**
   * Generates a human readable representation of this attribute.
   *
   * @param outputBuilder where the human representation of this attribute must be appended.
   * @param labelNames the human readable names of the labels.
   */
  void textify(StringBuilder outputBuilder, Map<Label, String> labelNames);
}
