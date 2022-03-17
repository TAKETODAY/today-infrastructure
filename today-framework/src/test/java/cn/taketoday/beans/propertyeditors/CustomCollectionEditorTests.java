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

package cn.taketoday.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link CustomCollectionEditor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class CustomCollectionEditorTests {

  @Test
  public void testCtorWithNullCollectionType() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new CustomCollectionEditor(null));
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testCtorWithNonCollectionType() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new CustomCollectionEditor((Class) String.class));
  }

  @Test
  public void testWithCollectionTypeThatDoesNotExposeAPublicNoArgCtor() throws Exception {
    CustomCollectionEditor editor = new CustomCollectionEditor(CollectionTypeWithNoNoArgCtor.class);
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setValue("1"));
  }

  @Test
  public void testSunnyDaySetValue() throws Exception {
    CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
    editor.setValue(new int[] { 0, 1, 2 });
    Object value = editor.getValue();
    assertThat(value).isNotNull();
    boolean condition = value instanceof ArrayList;
    assertThat(condition).isTrue();
    List<?> list = (List<?>) value;
    assertThat(list.size()).as("There must be 3 elements in the converted collection").isEqualTo(3);
    assertThat(list.get(0)).isEqualTo(0);
    assertThat(list.get(1)).isEqualTo(1);
    assertThat(list.get(2)).isEqualTo(2);
  }

  @Test
  public void testWhenTargetTypeIsExactlyTheCollectionInterfaceUsesFallbackCollectionType() throws Exception {
    CustomCollectionEditor editor = new CustomCollectionEditor(Collection.class);
    editor.setValue("0, 1, 2");
    Collection<?> value = (Collection<?>) editor.getValue();
    assertThat(value).isNotNull();
    assertThat(value.size()).as("There must be 1 element in the converted collection").isEqualTo(1);
    assertThat(value.iterator().next()).isEqualTo("0, 1, 2");
  }

  @Test
  public void testSunnyDaySetAsTextYieldsSingleValue() throws Exception {
    CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
    editor.setValue("0, 1, 2");
    Object value = editor.getValue();
    assertThat(value).isNotNull();
    boolean condition = value instanceof ArrayList;
    assertThat(condition).isTrue();
    List<?> list = (List<?>) value;
    assertThat(list.size()).as("There must be 1 element in the converted collection").isEqualTo(1);
    assertThat(list.get(0)).isEqualTo("0, 1, 2");
  }

  @SuppressWarnings({ "serial", "unused" })
  private static final class CollectionTypeWithNoNoArgCtor extends ArrayList<Object> {
    public CollectionTypeWithNoNoArgCtor(String anArg) {
    }
  }

}
