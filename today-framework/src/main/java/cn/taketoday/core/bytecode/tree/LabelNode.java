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

import java.util.Map;

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;

/** An {@link AbstractInsnNode} that encapsulates a {@link Label}. */
public class LabelNode extends AbstractInsnNode {

  private Label value;

  public LabelNode() {
    super(-1);
  }

  public LabelNode(final Label label) {
    super(-1);
    this.value = label;
  }

  @Override
  public int getType() {
    return LABEL;
  }

  /**
   * Returns the label encapsulated by this node. A new label is created and associated with this
   * node if it was created without an encapsulated label.
   *
   * @return the label encapsulated by this node.
   */
  public Label getLabel() {
    if (value == null) {
      value = new Label();
    }
    return value;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitLabel(getLabel());
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return clonedLabels.get(this);
  }

  public void resetLabel() {
    value = null;
  }
}
