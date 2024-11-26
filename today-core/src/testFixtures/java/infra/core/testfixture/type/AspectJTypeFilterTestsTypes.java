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

import infra.core.testfixture.stereotype.Component;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link infra.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.core.type.AspectJTypeFilterTests
 * @since 4.0 2021/12/15 23:06
 */
public class AspectJTypeFilterTestsTypes {

  public interface SomeInterface {
  }

  public static class SomeClass {
  }

  public static class SomeClassExtendingSomeClass extends SomeClass {
  }

  public static class SomeClassImplementingSomeInterface implements SomeInterface {
  }

  public static class SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface
          extends SomeClassExtendingSomeClass implements SomeInterface {
  }

  @Component
  public static class SomeClassAnnotatedWithComponent {
  }

}
