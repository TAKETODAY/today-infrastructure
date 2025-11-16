/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  @Test
  void constructorWithNullExceptionThrowsException() {
    assertThatThrownBy(() -> new ExceptionDepthComparator((Throwable) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Target exception is required");
  }

  @Test
  void constructorWithNullExceptionTypeThrowsException() {
    assertThatThrownBy(() -> new ExceptionDepthComparator((Class<? extends Throwable>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Target exception type is required");
  }

  @Test
  void compareWithSameExceptionTypes() {
    ExceptionDepthComparator comparator = new ExceptionDepthComparator(TargetException.class);
    int result = comparator.compare(TargetException.class, TargetException.class);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void compareWithDifferentExceptionTypesAtSameDepth() {
    ExceptionDepthComparator comparator = new ExceptionDepthComparator(TargetException.class);
    int result = comparator.compare(SameDepthException.class, SameDepthException.class);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void compareWithParentAndChildExceptionTypes() {
    ExceptionDepthComparator comparator = new ExceptionDepthComparator(TargetException.class);
    int result = comparator.compare(HighestDepthException.class, TargetException.class);
    assertThat(result).isGreaterThan(0);
  }

  @Test
  void compareWithChildAndParentExceptionTypes() {
    ExceptionDepthComparator comparator = new ExceptionDepthComparator(TargetException.class);
    int result = comparator.compare(TargetException.class, HighestDepthException.class);
    assertThat(result).isLessThan(0);
  }

  @Test
  void findClosestMatchWithEmptyCollectionThrowsException() {
    assertThatThrownBy(() -> ExceptionDepthComparator.findClosestMatch(
            java.util.Collections.emptyList(), new TargetException()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Exception types must not be empty");
  }

  @Test
  void findClosestMatchWithSingleExceptionType() {
    Collection<Class<? extends Throwable>> exceptionTypes = Arrays.asList(TargetException.class);
    Class<? extends Throwable> result = ExceptionDepthComparator.findClosestMatch(
            exceptionTypes, new SameDepthException());
    assertThat(result).isEqualTo(TargetException.class);
  }

  @Test
  void findClosestMatchReturnsMostSpecificException() {
    Collection<Class<? extends Throwable>> exceptionTypes = Arrays.asList(
            Throwable.class, Exception.class, TargetException.class, HighestDepthException.class);
    Class<? extends Throwable> result = ExceptionDepthComparator.findClosestMatch(
            exceptionTypes, new TargetException());
    assertThat(result).isEqualTo(TargetException.class);
  }

  @Test
  void findClosestMatchReturnsClosestWhenExactMatchNotFound() {
    Collection<Class<? extends Throwable>> exceptionTypes = Arrays.asList(
            Throwable.class, HighestDepthException.class);
    Class<? extends Throwable> result = ExceptionDepthComparator.findClosestMatch(
            exceptionTypes, new TargetException());
    assertThat(result).isEqualTo(HighestDepthException.class);
  }

  @Test
  void getDepthReturnsMaxValueForUnrelatedException() {
    ExceptionDepthComparator comparator = new ExceptionDepthComparator(TargetException.class);
    // Using reflection to test private method
    int depth = comparator.compare(Throwable.class, TargetException.class);
    assertThat(depth).isGreaterThan(0);
  }

  @Test
  void compareWithThrowableClass() {
    ExceptionDepthComparator comparator = new ExceptionDepthComparator(TargetException.class);
    int result = comparator.compare(TargetException.class, Throwable.class);
    assertThat(result).isLessThan(0);
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