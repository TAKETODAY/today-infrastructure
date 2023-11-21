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

package cn.taketoday.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link ComponentDefinition} implementation that holds one or more nested
 * {@link ComponentDefinition} instances, aggregating them into a named group
 * of components.
 *
 * @author Juergen Hoeller
 * @see #getNestedComponents()
 * @since 4.0
 */
public class CompositeComponentDefinition extends AbstractComponentDefinition {

  private final String name;

  @Nullable
  private final Object source;

  private final List<ComponentDefinition> nestedComponents = new ArrayList<>();

  /**
   * Create a new CompositeComponentDefinition.
   *
   * @param name the name of the composite component
   * @param source the source element that defines the root of the composite component
   */
  public CompositeComponentDefinition(String name, @Nullable Object source) {
    Assert.notNull(name, "Name is required");
    this.name = name;
    this.source = source;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

  /**
   * Add the given component as nested element of this composite component.
   *
   * @param component the nested component to add
   */
  public void addNestedComponent(ComponentDefinition component) {
    Assert.notNull(component, "ComponentDefinition is required");
    this.nestedComponents.add(component);
  }

  /**
   * Return the nested components that this composite component holds.
   *
   * @return the array of nested components, or an empty array if none
   */
  public ComponentDefinition[] getNestedComponents() {
    return this.nestedComponents.toArray(new ComponentDefinition[0]);
  }

}
