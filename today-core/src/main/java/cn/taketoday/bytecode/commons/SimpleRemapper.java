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

import java.util.Collections;
import java.util.Map;

/**
 * A {@link Remapper} using a {@link Map} to define its mapping.
 *
 * @author Eugene Kuleshov
 */
public class SimpleRemapper extends Remapper {

  private final Map<String, String> mapping;

  /**
   * Constructs a new {@link SimpleRemapper} with the given mapping.
   *
   * @param mapping a map specifying a remapping as follows:
   * <ul>
   *   <li>for method names, the key is the owner, name and descriptor of the method (in the
   *       form &lt;owner&gt;.&lt;name&gt;&lt;descriptor&gt;), and the value is the new method
   *       name.
   *   <li>for invokedynamic method names, the key is the name and descriptor of the method (in
   *       the form .&lt;name&gt;&lt;descriptor&gt;), and the value is the new method name.
   *   <li>for field and attribute names, the key is the owner and name of the field or
   *       attribute (in the form &lt;owner&gt;.&lt;name&gt;), and the value is the new field
   *       name.
   *   <li>for internal names, the key is the old internal name, and the value is the new
   *       internal name.
   * </ul>
   */
  public SimpleRemapper(final Map<String, String> mapping) {
    this.mapping = mapping;
  }

  /**
   * Constructs a new {@link SimpleRemapper} with the given mapping.
   *
   * @param oldName the key corresponding to a method, field or internal name (see {@link
   * #SimpleRemapper(Map)} for the format of these keys).
   * @param newName the new method, field or internal name.
   */
  public SimpleRemapper(final String oldName, final String newName) {
    this.mapping = Collections.singletonMap(oldName, newName);
  }

  @Override
  public String mapMethodName(final String owner, final String name, final String descriptor) {
    String remappedName = map(owner + '.' + name + descriptor);
    return remappedName == null ? name : remappedName;
  }

  @Override
  public String mapInvokeDynamicMethodName(final String name, final String descriptor) {
    String remappedName = map('.' + name + descriptor);
    return remappedName == null ? name : remappedName;
  }

  @Override
  public String mapAnnotationAttributeName(final String descriptor, final String name) {
    String remappedName = map(descriptor + '.' + name);
    return remappedName == null ? name : remappedName;
  }

  @Override
  public String mapFieldName(final String owner, final String name, final String descriptor) {
    String remappedName = map(owner + '.' + name);
    return remappedName == null ? name : remappedName;
  }

  @Override
  public String map(final String key) {
    return mapping.get(key);
  }
}
