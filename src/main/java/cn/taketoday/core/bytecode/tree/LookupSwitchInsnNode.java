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
 * A node that represents a LOOKUPSWITCH instruction.
 *
 * @author Eric Bruneton
 */
public class LookupSwitchInsnNode extends AbstractInsnNode {

  /** Beginning of the default handler block. */
  public LabelNode dflt;

  /** The values of the keys. */
  public List<Integer> keys;

  /** Beginnings of the handler blocks. */
  public List<LabelNode> labels;

  /**
   * Constructs a new {@link LookupSwitchInsnNode}.
   *
   * @param dflt beginning of the default handler block.
   * @param keys the values of the keys.
   * @param labels beginnings of the handler blocks. {@code labels[i]} is the beginning of the
   * handler block for the {@code keys[i]} key.
   */
  public LookupSwitchInsnNode(final LabelNode dflt, final int[] keys, final LabelNode[] labels) {
    super(Opcodes.LOOKUPSWITCH);
    this.dflt = dflt;
    this.keys = Util.asArrayList(keys);
    this.labels = Util.asArrayList(labels);
  }

  @Override
  public int getType() {
    return LOOKUPSWITCH_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    int[] keysArray = new int[this.keys.size()];
    for (int i = 0, n = keysArray.length; i < n; ++i) {
      keysArray[i] = this.keys.get(i);
    }
    Label[] labelsArray = new Label[this.labels.size()];
    for (int i = 0, n = labelsArray.length; i < n; ++i) {
      labelsArray[i] = this.labels.get(i).getLabel();
    }
    methodVisitor.visitLookupSwitchInsn(dflt.getLabel(), keysArray, labelsArray);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    LookupSwitchInsnNode clone =
            new LookupSwitchInsnNode(clone(dflt, clonedLabels), null, clone(labels, clonedLabels));
    clone.keys.addAll(keys);
    return clone.cloneAnnotations(this);
  }
}
