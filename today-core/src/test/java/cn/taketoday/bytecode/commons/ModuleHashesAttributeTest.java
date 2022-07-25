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
package cn.taketoday.bytecode.commons;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;

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
