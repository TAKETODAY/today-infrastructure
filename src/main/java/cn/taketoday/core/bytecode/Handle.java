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

package cn.taketoday.core.bytecode;

/**
 * A reference to a field or a method.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public final class Handle {

  /**
   * The kind of field or method designated by this Handle. Should be {@link Opcodes#H_GETFIELD},
   * {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC}, {@link
   * Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL},
   * {@link Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   */
  private final int tag;

  /** The internal name of the class that owns the field or method designated by this handle. */
  private final String owner;

  /** The name of the field or method designated by this handle. */
  private final String name;

  /** The descriptor of the field or method designated by this handle. */
  private final String descriptor;

  /** Whether the owner is an interface or not. */
  private final boolean isInterface;

  /**
   * Constructs a new field or method handle.
   *
   * @param tag the kind of field or method designated by this Handle. Must be {@link
   * Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link
   * Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
   * {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or {@link
   * Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of the class that owns the field or method designated by this
   * handle.
   * @param name the name of the field or method designated by this handle.
   * @param descriptor the descriptor of the field or method designated by this handle.
   */
  public Handle(final int tag, final String owner, final String name, final String descriptor) {
    this(tag, owner, name, descriptor, tag == Opcodes.H_INVOKEINTERFACE);
  }

  /**
   * Constructs a new field or method handle.
   *
   * @param tag the kind of field or method designated by this Handle. Must be {@link
   * Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link
   * Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
   * {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or {@link
   * Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of the class that owns the field or method designated by this
   * handle.
   * @param name the name of the field or method designated by this handle.
   * @param descriptor the descriptor of the field or method designated by this handle.
   * @param isInterface whether the owner is an interface or not.
   */
  public Handle(
          final int tag,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    this.tag = tag;
    this.owner = owner;
    this.name = name;
    this.descriptor = descriptor;
    this.isInterface = isInterface;
  }

  /**
   * Returns the kind of field or method designated by this handle.
   *
   * @return {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD},
   * {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link
   * Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL}, {@link
   * Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   */
  public int getTag() {
    return tag;
  }

  /**
   * Returns the internal name of the class that owns the field or method designated by this handle.
   *
   * @return the internal name of the class that owns the field or method designated by this handle.
   */
  public String getOwner() {
    return owner;
  }

  /**
   * Returns the name of the field or method designated by this handle.
   *
   * @return the name of the field or method designated by this handle.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the descriptor of the field or method designated by this handle.
   *
   * @return the descriptor of the field or method designated by this handle.
   */
  public String getDesc() {
    return descriptor;
  }

  /**
   * Returns true if the owner of the field or method designated by this handle is an interface.
   *
   * @return true if the owner of the field or method designated by this handle is an interface.
   */
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof Handle handle)) {
      return false;
    }
    return tag == handle.tag
            && isInterface == handle.isInterface
            && owner.equals(handle.owner)
            && name.equals(handle.name)
            && descriptor.equals(handle.descriptor);
  }

  @Override
  public int hashCode() {
    return tag
            + (isInterface ? 64 : 0)
            + owner.hashCode() * name.hashCode() * descriptor.hashCode();
  }

  /**
   * Returns the textual representation of this handle. The textual representation is:
   *
   * <ul>
   *   <li>for a reference to a class: owner "." name descriptor " (" tag ")",
   *   <li>for a reference to an interface: owner "." name descriptor " (" tag " itf)".
   * </ul>
   */
  @Override
  public String toString() {
    return owner + '.' + name + descriptor + " (" + tag + (isInterface ? " itf" : "") + ')';
  }
}
