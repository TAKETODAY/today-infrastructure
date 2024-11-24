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
package infra.bytecode.commons;

import org.junit.jupiter.api.Test;

import infra.bytecode.Attribute;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
import infra.bytecode.commons.ModuleTargetAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ModuleTargetAttribute}.
 *
 * @author Eric Bruneton
 */
public class ModuleTargetAttributeTest {

  @Test
  public void testWriteAndRead() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visitAttribute(new ModuleTargetAttribute("platform"));

    ModuleTargetAttribute moduleTargetAttribute = new ModuleTargetAttribute();
    new ClassReader(classWriter.toByteArray())
            .accept(
                    new ClassVisitor() {

                      @Override
                      public void visitAttribute(final Attribute attribute) {
                        if (attribute instanceof ModuleTargetAttribute) {
                          moduleTargetAttribute.platform = ((ModuleTargetAttribute) attribute).platform;
                        }
                      }
                    },
                    new Attribute[] { new ModuleTargetAttribute() },
                    0);

    assertEquals("platform", moduleTargetAttribute.platform);
  }
}
