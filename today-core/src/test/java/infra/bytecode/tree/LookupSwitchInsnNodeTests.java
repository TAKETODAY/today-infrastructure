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

import java.util.Arrays;

import infra.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LookupSwitchInsnNode}.
 *
 * @author Eric Bruneton
 */
public class LookupSwitchInsnNodeTests extends AsmTest {

  @Test
  public void testConstructor() {
    LabelNode dflt = new LabelNode();
    int[] keys = new int[] { 1 };
    LabelNode[] labels = new LabelNode[] { new LabelNode() };

    LookupSwitchInsnNode lookupSwitchInsnNode = new LookupSwitchInsnNode(dflt, keys, labels);

    assertEquals(AbstractInsnNode.LOOKUPSWITCH_INSN, lookupSwitchInsnNode.getType());
    assertEquals(dflt, lookupSwitchInsnNode.dflt);
    assertEquals(Arrays.asList(new Integer[] { 1 }), lookupSwitchInsnNode.keys);
    assertEquals(Arrays.asList(labels), lookupSwitchInsnNode.labels);
  }
}
