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

package cn.taketoday.core.testfixture.type;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link cn.taketoday.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see cn.taketoday.core.type.AssignableTypeFilterTests
 */
public class AssignableTypeFilterTestsTypes {

	public static class TestNonInheritingClass {
	}

	public interface TestInterface {
	}

	public static class TestInterfaceImpl implements TestInterface {
	}

	public interface SomeDaoLikeInterface {
	}

	public static class SomeDaoLikeImpl extends SimpleJdbcDaoSupport implements SomeDaoLikeInterface {
	}

	public interface JdbcDaoSupport {
	}

	public static class SimpleJdbcDaoSupport implements JdbcDaoSupport {
	}

}
