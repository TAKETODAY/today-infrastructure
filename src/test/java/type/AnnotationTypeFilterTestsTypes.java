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

package type;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link cn.taketoday.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @see cn.taketoday.core.type.AnnotationTypeFilterTests
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
