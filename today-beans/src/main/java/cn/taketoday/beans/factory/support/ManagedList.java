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

package cn.taketoday.beans.factory.support;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.beans.Mergeable;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Tag collection class used to hold managed List elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @param <E> the element type
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

  @Nullable
  private Object source;

  @Nullable
  private String elementTypeName;

  private boolean mergeEnabled;

  public ManagedList() { }

  public ManagedList(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

  /**
   * Set the default element type name (class name) to be used for this list.
   */
  public void setElementTypeName(@Nullable String elementTypeName) {
    this.elementTypeName = elementTypeName;
  }

  /**
   * Return the default element type name (class name) to be used for this list.
   */
  @Nullable
  public String getElementTypeName() {
    return this.elementTypeName;
  }

  /**
   * Set whether merging should be enabled for this collection,
   * in case of a 'parent' collection value being present.
   */
  public void setMergeEnabled(boolean mergeEnabled) {
    this.mergeEnabled = mergeEnabled;
  }

  @Override
  public boolean isMergeEnabled() {
    return this.mergeEnabled;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<E> merge(@Nullable Object parent) {
    if (!this.mergeEnabled) {
      throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
    }
    if (parent == null) {
      return this;
    }
    if (!(parent instanceof List)) {
      throw new IllegalArgumentException("Cannot merge with object of type [%s]".formatted(parent.getClass()));
    }
    List<E> merged = new ManagedList<>();
    merged.addAll((List<E>) parent);
    merged.addAll(this);
    return merged;
  }

  /**
   * Return a new instance containing an arbitrary number of elements.
   *
   * @param elements the elements to be contained in the list
   * @param <E> the {@code List}'s element type
   * @return a {@code List} containing the specified elements
   */
  @SuppressWarnings({ "unchecked", "varargs" })
  public static <E> ManagedList<E> of(E... elements) {
    ManagedList<E> list = new ManagedList<>();
    CollectionUtils.addAll(list, elements);
    return list;
  }
}
