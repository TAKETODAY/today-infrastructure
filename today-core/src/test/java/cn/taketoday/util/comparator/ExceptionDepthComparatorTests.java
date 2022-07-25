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

package cn.taketoday.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 10:41
 */
class ExceptionDepthComparatorTests {

  @Test
  void targetBeforeSameDepth() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(TargetException.class, SameDepthException.class);
    assertThat(foundClass).isEqualTo(TargetException.class);
  }

  @Test
  void sameDepthBeforeTarget() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(SameDepthException.class, TargetException.class);
    assertThat(foundClass).isEqualTo(TargetException.class);
  }

  @Test
  void lowestDepthBeforeTarget() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(LowestDepthException.class, TargetException.class);
    assertThat(foundClass).isEqualTo(TargetException.class);
  }

  @Test
  void targetBeforeLowestDepth() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(TargetException.class, LowestDepthException.class);
    assertThat(foundClass).isEqualTo(TargetException.class);
  }

  @Test
  void noDepthBeforeTarget() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(NoDepthException.class, TargetException.class);
    assertThat(foundClass).isEqualTo(TargetException.class);
  }

  @Test
  void noDepthBeforeHighestDepth() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(NoDepthException.class, HighestDepthException.class);
    assertThat(foundClass).isEqualTo(HighestDepthException.class);
  }

  @Test
  void highestDepthBeforeNoDepth() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(HighestDepthException.class, NoDepthException.class);
    assertThat(foundClass).isEqualTo(HighestDepthException.class);
  }

  @Test
  void highestDepthBeforeLowestDepth() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(HighestDepthException.class, LowestDepthException.class);
    assertThat(foundClass).isEqualTo(LowestDepthException.class);
  }

  @Test
  void lowestDepthBeforeHighestDepth() throws Exception {
    Class<? extends Throwable> foundClass = findClosestMatch(LowestDepthException.class, HighestDepthException.class);
    assertThat(foundClass).isEqualTo(LowestDepthException.class);
  }

  @SafeVarargs
  private Class<? extends Throwable> findClosestMatch(
          Class<? extends Throwable>... classes) {
    return ExceptionDepthComparator.findClosestMatch(Arrays.asList(classes), new TargetException());
  }

  @SuppressWarnings("serial")
  public class HighestDepthException extends Throwable {
  }

  @SuppressWarnings("serial")
  public class LowestDepthException extends HighestDepthException {
  }

  @SuppressWarnings("serial")
  public class TargetException extends LowestDepthException {
  }

  @SuppressWarnings("serial")
  public class SameDepthException extends LowestDepthException {
  }

  @SuppressWarnings("serial")
  public class NoDepthException extends TargetException {
  }

}