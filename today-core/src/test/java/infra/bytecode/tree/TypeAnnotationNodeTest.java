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
package infra.bytecode.tree;

import org.junit.jupiter.api.Test;

import infra.bytecode.AsmTest;
import infra.bytecode.TypePath;

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
