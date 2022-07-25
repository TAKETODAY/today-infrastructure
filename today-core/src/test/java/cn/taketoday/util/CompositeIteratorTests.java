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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author TODAY 2021/10/1 14:51
 */
class CompositeIteratorTests {

  @Test
  void noIterators() {
    CompositeIterator<String> it = new CompositeIterator<>();
    assertThat(it.hasNext()).isFalse();
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
            it::next);
  }

  @Test
  void singleIterator() {
    CompositeIterator<String> it = new CompositeIterator<>();
    it.add(Arrays.asList("0", "1").iterator());
    for (int i = 0; i < 2; i++) {
      assertThat(it.hasNext()).isTrue();
      assertThat(it.next()).isEqualTo(String.valueOf(i));
    }
    assertThat(it.hasNext()).isFalse();
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
            it::next);
  }

  @Test
  void multipleIterators() {
    CompositeIterator<String> it = new CompositeIterator<>();
    it.add(Arrays.asList("0", "1").iterator());
    it.add(Arrays.asList("2").iterator());
    it.add(Arrays.asList("3", "4").iterator());
    for (int i = 0; i < 5; i++) {
      assertThat(it.hasNext()).isTrue();
      assertThat(it.next()).isEqualTo(String.valueOf(i));
    }
    assertThat(it.hasNext()).isFalse();

    assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(it::next);
  }

  @Test
  void inUse() {
    List<String> list = Arrays.asList("0", "1");
    CompositeIterator<String> it = new CompositeIterator<>();
    it.add(list.iterator());
    it.hasNext();
    assertThatIllegalStateException()
            .isThrownBy(() -> it.add(list.iterator()));
    CompositeIterator<String> it2 = new CompositeIterator<>();
    it2.add(list.iterator());
    it2.next();
    assertThatIllegalStateException()
            .isThrownBy(() -> it2.add(list.iterator()));
  }

  @Test
  void duplicateIterators() {
    List<String> list = Arrays.asList("0", "1");
    Iterator<String> iterator = list.iterator();
    CompositeIterator<String> it = new CompositeIterator<>();
    it.add(iterator);
    it.add(list.iterator());
    assertThatIllegalArgumentException()
            .isThrownBy(() -> it.add(iterator));
  }

}
