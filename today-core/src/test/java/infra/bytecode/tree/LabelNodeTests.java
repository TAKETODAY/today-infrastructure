/*
 * Copyright 2017 - 2025 the original author or authors.
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
import infra.bytecode.Label;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link LabelNode}.
 *
 * @author Eric Bruneton
 */
public class LabelNodeTests extends AsmTest {

  @Test
  public void testConstructor() {
    Label label = new Label();

    LabelNode labelNode1 = new LabelNode();
    LabelNode labelNode2 = new LabelNode(label);

    assertEquals(AbstractInsnNode.LABEL, labelNode1.getType());
    assertNotNull(labelNode1.getLabel());
    assertEquals(label, labelNode2.getLabel());
  }
}
