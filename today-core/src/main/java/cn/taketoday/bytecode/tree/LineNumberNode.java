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

import java.util.Map;

import cn.taketoday.bytecode.MethodVisitor;

/**
 * A node that represents a line number declaration. These nodes are pseudo instruction nodes in
 * order to be inserted in an instruction list.
 *
 * @author Eric Bruneton
 */
public class LineNumberNode extends AbstractInsnNode {

  /** A line number. This number refers to the source file from which the class was compiled. */
  public int line;

  /** The first instruction corresponding to this line number. */
  public LabelNode start;

  /**
   * Constructs a new {@link LineNumberNode}.
   *
   * @param line a line number. This number refers to the source file from which the class was
   * compiled.
   * @param start the first instruction corresponding to this line number.
   */
  public LineNumberNode(final int line, final LabelNode start) {
    super(-1);
    this.line = line;
    this.start = start;
  }

  @Override
  public int getType() {
    return LINE;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitLineNumber(line, start.getLabel());
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new LineNumberNode(line, clone(start, clonedLabels));
  }
}
