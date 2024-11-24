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

package infra.bytecode;

/**
 * Information about the input stack map frame at the "current" instruction of a method. This is
 * implemented as a Frame subclass for a "basic block" containing only one instruction.
 *
 * @author Eric Bruneton
 */
final class CurrentFrame extends Frame {

  CurrentFrame(final Label owner) {
    super(owner);
  }

  /**
   * Sets this CurrentFrame to the input stack map frame of the next "current" instruction, i.e. the
   * instruction just after the given one. It is assumed that the value of this object when this
   * method is called is the stack map frame status just before the given instruction is executed.
   */
  @Override
  void execute(
          final int opcode, final int arg, final Symbol symbolArg, final SymbolTable symbolTable) {
    super.execute(opcode, arg, symbolArg, symbolTable);
    Frame successor = new Frame(null);
    merge(symbolTable, successor, 0);
    copyFrom(successor);
  }
}
