/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JoinFragmentTests {

  @ParameterizedTest
  @CsvSource({
          "0, INNER_JOIN, inner, inner join",
          "1, LEFT_OUTER_JOIN, left, left outer join",
          "2, RIGHT_OUTER_JOIN, right, right outer join",
          "4, FULL_JOIN, full, full outer join"
  })
  void rendersEachAnsiJoinTypeWithCompositeKeys(int code, JoinType type, String token, String joinKeyword) {
    assertThat(JoinType.parse(code)).isSameAs(type);
    assertThat(type.getJoinTypeValue()).isEqualTo(code);
    assertThat(type.getSqlText()).isEqualTo(token);

    var join = new ANSIJoinFragment();
    String[] lhs = { "p.tenant_id", "p.account_id" };
    String[] rhs = { "tenant_id", "id" };
    join.addJoin("accounts", "a", lhs, rhs, type);
    assertThat(join.toFromFragmentString()).isEqualTo(" " + joinKeyword + " accounts a on p.tenant_id=a.tenant_id and p.account_id=a.id");
    assertThat(join.toWhereFragmentString()).isEmpty();

    var alternatives = new ANSIJoinFragment();
    alternatives.addJoin("accounts", "a", new String[][] { lhs }, rhs, type, "a.enabled=1");
    assertThat(alternatives.toFromFragmentString()).isEqualTo(join.toFromFragmentString() + " and a.enabled=1");
  }

  @Test
  void rejectsUndefinedJoinTypesWithoutChangingTheFragment() {
    assertThat(JoinType.parse(-1)).isSameAs(JoinType.NONE);
    assertThatIllegalStateException().isThrownBy(() -> JoinType.parse(3)).withMessageContaining("unknown join type: 3");

    var join = new ANSIJoinFragment();
    String[] lhs = { "p.account_id" };
    String[] rhs = { "id" };
    assertThatIllegalArgumentException().isThrownBy(() -> join.addJoin("accounts", "a", lhs, rhs, JoinType.NONE))
            .withMessageContaining("undefined join type");
    assertThatIllegalArgumentException().isThrownBy(() -> join.addJoin("accounts", "a", new String[][] { lhs }, rhs, JoinType.NONE, null))
            .withMessageContaining("undefined join type");
    assertThat(join.toFromFragmentString()).isEmpty();
  }

  @Test
  void groupsAlternativeKeysBeforeApplyingAnAdditionalOnCondition() {
    var join = new ANSIJoinFragment();
    join.addJoin("accounts", "a", new String[][] { { "p.tenant_id", "p.owner_id" }, { "p.tenant_id", "p.editor_id" } },
            new String[] { "tenant_id", "id" }, JoinType.LEFT_OUTER_JOIN, " and a.enabled=1");

    assertThat(join.toFromFragmentString()).isEqualTo(" left outer join accounts a on "
            + "(p.tenant_id=a.tenant_id and p.owner_id=a.id or p.tenant_id=a.tenant_id and p.editor_id=a.id) and a.enabled=1");
  }

  @Test
  void keepsAnsiFiltersSeparateFromTheFromFragment() {
    var join = new ANSIJoinFragment();
    join.addFromFragmentString("projects p");
    join.addCrossJoin("regions", "r");
    join.addJoins(" inner join teams t on t.id=p.team_id", "");
    var snapshot = join.copy();

    assertThat(join.addCondition(null)).isFalse();
    assertThat(join.addCondition("r.enabled=1")).isTrue();
    join.addCondition("p", new String[] { "archived_at", "deleted_at" }, " is null");
    join.addCrossJoin("accounts", "a");

    assertThat(snapshot.toFromFragmentString()).isEqualTo("projects p, regions r inner join teams t on t.id=p.team_id");
    assertThat(join.toFromFragmentString()).isEqualTo(snapshot.toFromFragmentString() + ", accounts a");
    assertThat(join.toWhereFragmentString()).isEqualTo(" and r.enabled=1 and p.archived_at is null and p.deleted_at is null");
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> join.addCondition("a", new String[] { "p.account_id" }, new String[] { "id" }));
  }

  @Test
  void usesWherePredicatesForThetaInnerJoinsButKeepsOuterJoinsAnsi() {
    var join = new QueryJoinFragment(Platform.generic(), true);
    join.addFromFragmentString("projects p");
    join.addJoin("accounts", "a", new String[] { "p.tenant_id", "p.account_id" },
            new String[] { "tenant_id", "id" }, JoinType.INNER_JOIN, "a.enabled=1");
    join.addJoin("teams", "t", new String[] { "p.team_id" }, new String[] { "id" }, JoinType.LEFT_OUTER_JOIN);
    join.addJoin("users", "u", new String[][] { { "p.owner_id" } }, new String[] { "id" }, JoinType.RIGHT_OUTER_JOIN);

    assertThat(join.toFromFragmentString())
            .isEqualTo("projects p, accounts a left outer join teams t on p.team_id=t.id right outer join users u on p.owner_id=u.id");
    assertThat(join.toWhereFragmentString()).isEqualTo(" and p.tenant_id=a.tenant_id and p.account_id=a.id and a.enabled=1");
  }

  @Test
  void groupsThetaAlternativesAndOmitsDuplicateConditions() {
    var join = new QueryJoinFragment(Platform.generic(), true);
    join.addJoin("accounts", "a", new String[][] { { "p.tenant_id", "p.owner_id" }, { "p.tenant_id", "p.editor_id" } },
            new String[] { "tenant_id", "id" }, JoinType.INNER_JOIN, " and a.enabled=1");
    join.addJoin("teams", "t", new String[][] { { "p.team_id" } }, new String[] { "id" }, JoinType.INNER_JOIN);
    join.addCondition("p", new String[] { "deleted_at" }, " is null");

    assertThat(join.addCondition("accounts a")).isFalse();
    assertThat(join.addCondition("a.enabled=1")).isFalse();
    assertThat(join.addCondition("")).isFalse();
    assertThat(join.toFromFragmentString()).isEqualTo(", accounts a, teams t");
    assertThat(join.toWhereFragmentString()).isEqualTo(" and (p.tenant_id=a.tenant_id and p.owner_id=a.id "
            + "or p.tenant_id=a.tenant_id and p.editor_id=a.id) and a.enabled=1 and p.team_id=t.id and p.deleted_at is null");
  }

  @Test
  void copiesBothQueryJoinFragmentsAndPropagatesThetaMetadataWhenMerging() {
    var source = new QueryJoinFragment(Platform.generic(), true);
    source.addJoins(", accounts a", " and p.account_id=a.id");
    source.setHasThetaJoins(true);
    source.setHasFilterCondition(true);
    assertThat(source.hasFilterCondition()).isTrue();

    var merged = new QueryJoinFragment(Platform.generic(), false);
    merged.addFragment(source);
    assertThat(merged.hasThetaJoins()).isTrue();
    var snapshot = merged.copy();
    merged.clearWherePart();
    merged.addCrossJoin("teams", "t");

    assertThat(snapshot.toFromFragmentString()).isEqualTo(", accounts a");
    assertThat(snapshot.toWhereFragmentString()).isEqualTo(" and p.account_id=a.id");
    assertThat(merged.toWhereFragmentString()).isEmpty();
  }

}
