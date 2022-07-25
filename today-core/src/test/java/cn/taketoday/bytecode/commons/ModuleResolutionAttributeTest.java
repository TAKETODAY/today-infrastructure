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

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ModuleResolutionAttribute}.
 *
 * @author Eric Bruneton
 */
public class ModuleResolutionAttributeTest {

  @Test
  public void testWriteAndRead() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visitAttribute(new ModuleResolutionAttribute(123));

    ModuleResolutionAttribute moduleResolutionAttribute = new ModuleResolutionAttribute();
    new ClassReader(classWriter.toByteArray())
            .accept(new ClassVisitor() {

                      @Override
                      public void visitAttribute(final Attribute attribute) {
                        if (attribute instanceof ModuleResolutionAttribute) {
                          moduleResolutionAttribute.resolution =
                                  ((ModuleResolutionAttribute) attribute).resolution;
                        }
                      }
                    },
                    new Attribute[] { new ModuleResolutionAttribute() },
                    0);

    assertEquals(123, moduleResolutionAttribute.resolution);
  }
}
