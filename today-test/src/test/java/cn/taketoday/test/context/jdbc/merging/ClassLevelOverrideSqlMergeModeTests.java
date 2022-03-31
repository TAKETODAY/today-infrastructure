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

package cn.taketoday.test.context.jdbc.merging;

import org.junit.jupiter.api.Test;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.context.jdbc.SqlMergeMode;

import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

/**
 * Transactional integration tests that verify proper merging and overriding support
 * for class-level and method-level {@link Sql @Sql} declarations when
 * {@link SqlMergeMode @SqlMergeMode} is declared at the class level with
 * {@link SqlMergeMode.MergeMode#OVERRIDE OVERRIDE} mode.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 4.0
 */
@Sql({ "../recreate-schema.sql", "../data-add-catbert.sql" })
@SqlMergeMode(OVERRIDE)
class ClassLevelOverrideSqlMergeModeTests extends AbstractSqlMergeModeTests {

	@Test
	void classLevelScripts() {
		assertUsers("Catbert");
	}

	@Test
	@Sql("../data-add-dogbert.sql")
	@SqlMergeMode(MERGE)
	void merged() {
		assertUsers("Catbert", "Dogbert");
	}

	@Test
	@Sql({ "../recreate-schema.sql", "../data.sql", "../data-add-dogbert.sql", "../data-add-catbert.sql" })
	void overridden() {
		assertUsers("Dilbert", "Dogbert", "Catbert");
	}

}
