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

package infra.core.testfixture.type;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link infra.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @see infra.core.type.AnnotationTypeFilterTests
 */
public class AnnotationTypeFilterTestsTypes {

  @InheritedAnnotation
  public static class SomeComponent {
  }

  @InheritedAnnotation
  public interface SomeComponentInterface {
  }

  @SuppressWarnings("unused")
  public static class SomeClassWithSomeComponentInterface implements Cloneable, SomeComponentInterface {
  }

  @SuppressWarnings("unused")
  public static class SomeSubclassOfSomeComponent extends SomeComponent {
  }

  @NonInheritedAnnotation
  public static class SomeClassMarkedWithNonInheritedAnnotation {
  }

  @SuppressWarnings("unused")
  public static class SomeSubclassOfSomeClassMarkedWithNonInheritedAnnotation extends SomeClassMarkedWithNonInheritedAnnotation {
  }

  @SuppressWarnings("unused")
  public static class SomeNonCandidateClass {
  }

}
