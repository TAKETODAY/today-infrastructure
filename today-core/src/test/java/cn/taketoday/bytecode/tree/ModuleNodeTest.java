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

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ModuleVisitor;
import cn.taketoday.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ModuleNode}.
 *
 * @author Eric Bruneton
 */
public class ModuleNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    ModuleNode moduleNode1 = new ModuleNode("module1", 123, "1.0");
    ModuleNode moduleNode2 =
            new ModuleNode(
                    "module2",
                    456,
                    "2.0",
                    null,
                    null,
                    null,
                    null,
                    null) { };

    assertEquals("module1", moduleNode1.name);
    assertEquals(123, moduleNode1.access);
    assertEquals("1.0", moduleNode1.version);
    assertEquals("module2", moduleNode2.name);
    assertEquals(456, moduleNode2.access);
    assertEquals("2.0", moduleNode2.version);
  }

  @Test
  public void testAccept() {
    ModuleNode moduleNode = new ModuleNode("module", 123, "1.0");
    ModuleNode dstModuleNode = new ModuleNode("", 0, "");
    ClassVisitor copyModuleVisitor =
            new ClassVisitor() {
              @Override
              public ModuleVisitor visitModule(
                      final String name, final int access, final String version) {
                dstModuleNode.name = name;
                dstModuleNode.access = access;
                dstModuleNode.version = version;
                return dstModuleNode;
              }
            };

    moduleNode.accept(copyModuleVisitor);

    assertEquals("module", dstModuleNode.name);
    assertEquals(123, dstModuleNode.access);
    assertEquals("1.0", dstModuleNode.version);
  }
}
