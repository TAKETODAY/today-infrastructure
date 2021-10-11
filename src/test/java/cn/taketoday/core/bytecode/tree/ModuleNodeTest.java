// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.core.bytecode.tree;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.ModuleVisitor;

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
