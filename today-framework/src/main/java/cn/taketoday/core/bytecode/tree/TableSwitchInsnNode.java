/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.tree;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;

/**
 * A node that represents a TABLESWITCH instruction.
 *
 * @author Eric Bruneton
 */
public class TableSwitchInsnNode extends AbstractInsnNode {

  /** The minimum key value. */
  public int min;

  /** The maximum key value. */
  public int max;

  /** Beginning of the default handler block. */
  public LabelNode dflt;

  /** Beginnings of the handler blocks. This list is a list of {@link LabelNode} objects. */
  public List<LabelNode> labels;

  /**
   * Constructs a new {@link TableSwitchInsnNode}.
   *
   * @param min the minimum key value.
   * @param max the maximum key value.
   * @param dflt beginning of the default handler block.
   * @param labels beginnings of the handler blocks. {@code labels[i]} is the beginning of the
   * handler block for the {@code min + i} key.
   */
  public TableSwitchInsnNode(
          final int min, final int max, final LabelNode dflt, final LabelNode... labels) {
    super(Opcodes.TABLESWITCH);
    this.min = min;
    this.max = max;
    this.dflt = dflt;
    this.labels = Util.asArrayList(labels);
  }

  @Override
  public int getType() {
    return TABLESWITCH_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    Label[] labelsArray = new Label[this.labels.size()];
    for (int i = 0, n = labelsArray.length; i < n; ++i) {
      labelsArray[i] = this.labels.get(i).getLabel();
    }
    methodVisitor.visitTableSwitchInsn(min, max, dflt.getLabel(), labelsArray);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new TableSwitchInsnNode(min, max, clone(dflt, clonedLabels), clone(labels, clonedLabels))
            .cloneAnnotations(this);
  }
}
