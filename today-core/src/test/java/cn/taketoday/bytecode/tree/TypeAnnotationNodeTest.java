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
package cn.taketoday.bytecode.tree;

import org.junit.jupiter.api.Test;

import cn.taketoday.bytecode.TypePath;
import cn.taketoday.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TypeAnnotationNode}.
 *
 * @author Eric Bruneton
 */
public class TypeAnnotationNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    TypePath typePath = TypePath.fromString("[");

    TypeAnnotationNode typeAnnotationNode = new TypeAnnotationNode(123, typePath, "LI;");

    assertEquals(123, typeAnnotationNode.typeRef);
    assertEquals(typePath, typeAnnotationNode.typePath);
    assertEquals("LI;", typeAnnotationNode.desc);
  }
}
