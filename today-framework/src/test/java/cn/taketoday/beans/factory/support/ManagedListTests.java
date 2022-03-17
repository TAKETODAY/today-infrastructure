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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ManagedListTests {

  @Test
  public void mergeSunnyDay() {
    ManagedList parent = new ManagedList();
    parent.add("one");
    parent.add("two");
    ManagedList child = new ManagedList();
    child.add("three");
    child.setMergeEnabled(true);
    List mergedList = child.merge(parent);
    assertThat(mergedList.size()).as("merge() obviously did not work.").isEqualTo(3);
  }

  @Test
  public void mergeWithNullParent() {
    ManagedList child = new ManagedList();
    child.add("one");
    child.setMergeEnabled(true);
    assertThat(child.merge(null)).isSameAs(child);
  }

  @Test
  public void mergeNotAllowedWhenMergeNotEnabled() {
    ManagedList child = new ManagedList();
    assertThatIllegalStateException().isThrownBy(() ->
            child.merge(null));
  }

  @Test
  public void mergeWithNonCompatibleParentType() {
    ManagedList child = new ManagedList();
    child.add("one");
    child.setMergeEnabled(true);
    assertThatIllegalArgumentException().isThrownBy(() ->
            child.merge("hello"));
  }

  @Test
  public void mergeEmptyChild() {
    ManagedList parent = new ManagedList();
    parent.add("one");
    parent.add("two");
    ManagedList child = new ManagedList();
    child.setMergeEnabled(true);
    List mergedList = child.merge(parent);
    assertThat(mergedList.size()).as("merge() obviously did not work.").isEqualTo(2);
  }

  @Test
  public void mergeChildValuesOverrideTheParents() {
    // doesn't make much sense in the context of a list...
    ManagedList parent = new ManagedList();
    parent.add("one");
    parent.add("two");
    ManagedList child = new ManagedList();
    child.add("one");
    child.setMergeEnabled(true);
    List mergedList = child.merge(parent);
    assertThat(mergedList.size()).as("merge() obviously did not work.").isEqualTo(3);
  }

}
