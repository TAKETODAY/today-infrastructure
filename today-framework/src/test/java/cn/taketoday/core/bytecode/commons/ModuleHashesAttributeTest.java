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
package cn.taketoday.core.bytecode.commons;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.ClassWriter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ModuleHashesAttribute}.
 *
 * @author Eric Bruneton
 */
public class ModuleHashesAttributeTest {

  private static final byte[] HASH1 = { 0x1, 0x2, 0x3 };
  private static final byte[] HASH2 = { 0x4, 0x5, 0x6 };

  @Test
  public void testWriteAndRead() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visitAttribute(
            new ModuleHashesAttribute(
                    "algorithm",
                    Arrays.asList(new String[] { "module1", "module2" }),
                    Arrays.asList(new byte[][] { HASH1, HASH2 })));

    ModuleHashesAttribute moduleHashesAttribute = new ModuleHashesAttribute();
    new ClassReader(classWriter.toByteArray())
            .accept(
                    new ClassVisitor() {

                      @Override
                      public void visitAttribute(final Attribute attribute) {
                        if (attribute instanceof ModuleHashesAttribute) {
                          moduleHashesAttribute.algorithm = ((ModuleHashesAttribute) attribute).algorithm;
                          moduleHashesAttribute.modules = ((ModuleHashesAttribute) attribute).modules;
                          moduleHashesAttribute.hashes = ((ModuleHashesAttribute) attribute).hashes;
                        }
                      }
                    },
                    new Attribute[] { new ModuleHashesAttribute() },
                    0);

    assertEquals("algorithm", moduleHashesAttribute.algorithm);
    assertEquals(2, moduleHashesAttribute.modules.size());
    assertEquals("module1", moduleHashesAttribute.modules.get(0));
    assertEquals("module2", moduleHashesAttribute.modules.get(1));
    assertEquals(2, moduleHashesAttribute.hashes.size());
    assertArrayEquals(HASH1, moduleHashesAttribute.hashes.get(0));
    assertArrayEquals(HASH2, moduleHashesAttribute.hashes.get(1));
  }
}
