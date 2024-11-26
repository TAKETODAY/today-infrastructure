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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link RecordComponentNode}.
 *
 * @author Eric Bruneton
 */
public class RecordComponentNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    RecordComponentNode componentNode = new RecordComponentNode("component", "I", null);

    assertEquals("component", componentNode.name);
    assertEquals("I", componentNode.descriptor);

  }

  @Test
  void visitAttribute() {
    RecordComponentNode componentNode = new RecordComponentNode("component", "I", null);
    componentNode.visitAttribute(new Comment());

    ClassNode classVisitor = new ClassNode();
    classVisitor.visitRecordComponent("component", "I", null);
    componentNode.accept(classVisitor);

    assertThat(componentNode.getDelegate()).isNull();
  }
}
