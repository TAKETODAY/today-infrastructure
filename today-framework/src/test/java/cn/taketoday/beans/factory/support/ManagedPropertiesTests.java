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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("rawtypes")
public class ManagedPropertiesTests {

  @Test
  public void mergeSunnyDay() {
    ManagedProperties parent = new ManagedProperties();
    parent.setProperty("one", "one");
    parent.setProperty("two", "two");
    ManagedProperties child = new ManagedProperties();
    child.setProperty("three", "three");
    child.setMergeEnabled(true);
    Map mergedMap = (Map) child.merge(parent);
    assertThat(mergedMap.size()).as("merge() obviously did not work.").isEqualTo(3);
  }

  @Test
  public void mergeWithNullParent() {
    ManagedProperties child = new ManagedProperties();
    child.setMergeEnabled(true);
    assertThat(child.merge(null)).isSameAs(child);
  }

  @Test
  public void mergeWithNonCompatibleParentType() {
    ManagedProperties map = new ManagedProperties();
    map.setMergeEnabled(true);
    assertThatIllegalArgumentException().isThrownBy(() ->
            map.merge("hello"));
  }

  @Test
  public void mergeNotAllowedWhenMergeNotEnabled() {
    ManagedProperties map = new ManagedProperties();
    assertThatIllegalStateException().isThrownBy(() ->
            map.merge(null));
  }

  @Test
  public void mergeEmptyChild() {
    ManagedProperties parent = new ManagedProperties();
    parent.setProperty("one", "one");
    parent.setProperty("two", "two");
    ManagedProperties child = new ManagedProperties();
    child.setMergeEnabled(true);
    Map mergedMap = (Map) child.merge(parent);
    assertThat(mergedMap.size()).as("merge() obviously did not work.").isEqualTo(2);
  }

  @Test
  public void mergeChildValuesOverrideTheParents() {
    ManagedProperties parent = new ManagedProperties();
    parent.setProperty("one", "one");
    parent.setProperty("two", "two");
    ManagedProperties child = new ManagedProperties();
    child.setProperty("one", "fork");
    child.setMergeEnabled(true);
    Map mergedMap = (Map) child.merge(parent);
    // child value for 'one' must override parent value...
    assertThat(mergedMap.size()).as("merge() obviously did not work.").isEqualTo(2);
    assertThat(mergedMap.get("one")).as("Parent value not being overridden during merge().").isEqualTo("fork");
  }

}
