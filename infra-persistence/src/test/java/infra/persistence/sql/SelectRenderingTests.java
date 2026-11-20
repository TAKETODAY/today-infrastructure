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

import java.util.List;

import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SelectRenderingTests {

  private final Platform platform = Platform.generic();

  @Test
  void rendersClausesInOrderAndPlacesJoinFiltersBeforeLocalFilters() {
    var columns = new SelectFragment().addColumn("o", "customer_id", "customer");
    var select = new Select().setSelectClause(columns).setFromClause("orders", "o")
            .setOuterJoins(" left outer join customers c on c.id=o.customer_id", " and c.active=1")
            .setWhereClause("o.region=?").setWhereClause("o.tenant_id=?")
            .setGroupByClause("o.customer_id").setOrderByClause("o.customer_id desc")
            .setComment("monthly orders").setForUpdate(true);

    assertThat(select.toStatementString(platform)).isEqualTo("/* monthly orders */ SELECT o.customer_id as customer FROM orders o"
            + " left outer join customers c on c.id=o.customer_id WHERE c.active=1 AND o.tenant_id=?"
            + " group by o.customer_id order by o.customer_id desc for update");
  }

  @Test
  void leavesNoDanglingConnectorAfterRemovingALocalPredicate() {
    var select = new Select().setSelectClause("o.id").setFromClause("orders o");
    assertThat(select.toStatementString(platform)).isEqualTo("SELECT o.id FROM orders o");

    select.setWhereClause("o.tenant_id=?");
    assertThat(select.toStatementString(platform)).isEqualTo("SELECT o.id FROM orders o WHERE o.tenant_id=?");

    select.setOuterJoins(", customers c", "o.customer_id=c.id");
    for (String localFilter : new String[] { "", null }) {
      assertThat(select.setWhereClause(localFilter).toStatementString(platform))
              .isEqualTo("SELECT o.id FROM orders o, customers c WHERE o.customer_id=c.id");
    }
  }

  @Test
  void rendersTokenizedQueryAndGroupsLocalDisjunctionAfterJoinFilters() {
    var select = new QuerySelect(platform);
    select.setComment("customer totals");
    select.setDistinct(true);
    select.addSelectFragmentString("");
    select.addSelectFragmentString(", o.customer_id");
    select.addSelectColumn("sum(o.amount)", "total");
    select.getJoinFragment().addJoins("orders o, customers c", " and o.customer_id=c.id");
    select.setWhereTokens(List.of("o", ".", "status", "=", "'A'", "'B'", "or", "o", ".", "priority", ">", "?").iterator());
    select.setGroupByTokens(List.of("o", ".", "customer_id").iterator());
    select.setHavingTokens(List.of("sum", "(", "o", ".", "amount", ")", ">", "?").iterator());
    select.setOrderByTokens(List.of("total", "desc").iterator());
    select.addOrderBy("o.customer_id");

    assertThat(select.toStatementString(platform))
            .isEqualToNormalizingWhitespace("/* customer totals */ SELECT DISTINCT o.customer_id, sum(o.amount) total"
                    + " FROM orders o, customers c WHERE o.customer_id=c.id AND (o.status='A''B' or o.priority>?)"
                    + " GROUP BY o.customer_id HAVING sum(o.amount)>? order by total desc, o.customer_id");
  }

  @Test
  void prependsPredicatesAndAllowsAnOrderWithoutOrderTokens() {
    var select = new QuerySelect(platform);
    select.addSelectFragmentString("o.id");
    select.getJoinFragment().addJoins(" INNER JOIN orders o", "");
    assertThat(select.toStatementString(platform)).isEqualToNormalizingWhitespace("SELECT o.id FROM orders o");

    select.prependWhereConditions("o.active=1");
    select.prependWhereConditions("o.tenant_id=?");
    select.addOrderBy("o.id");
    assertThat(select.toStatementString(platform))
            .isEqualToNormalizingWhitespace("SELECT o.id FROM orders o WHERE o.tenant_id=? and o.active=1 order by o.id");
  }

  @Test
  void rendersJoinPredicatesWithoutAnExtraAndWhenThereAreNoLocalFilters() {
    var select = new QuerySelect(platform);
    select.addSelectFragmentString("c.id");
    select.getJoinFragment().addCrossJoin("customers", "c");
    select.getJoinFragment().addCondition("c.active=1");

    assertThat(select.toStatementString(platform)).isEqualToNormalizingWhitespace("SELECT c.id FROM customers c WHERE c.active=1");
  }

  @Test
  void suffixesAliasesSkipsDuplicatesAndAppendsCaseExpression() {
    var discriminator = new ANSICaseFragment().addWhenColumnNotNull("p", "email", "'email'")
            .addWhenColumnNotNull("p", "phone", "'phone'");
    assertThat(discriminator.toFragmentString()).isEqualTo("case when p.email is not null then 'email' when p.phone is not null then 'phone' end");

    var fragment = new SelectFragment().setSuffix("_1").setUsedAliases(new String[] { "id" })
            .addColumns("p", new String[] { "id", "name" })
            .addColumns("p", new String[] { "display_name", null, "email" }, new String[] { "name", "unused", "[address]" })
            .setExtraSelectList(discriminator, "contact_kind");

    assertThat(fragment.getColumns()).containsExactly("p.id", "p.name", "p.display_name", "p.email");
    String expectedCase = "case when p.email is not null then 'email' when p.phone is not null then 'phone' end as contact_kind_1";
    assertThat(fragment.getExtraSelectList()).isEqualTo(expectedCase);
    assertThat(fragment.toFragmentString()).isEqualTo(", p.name as name_1, p.email as [address_1], " + expectedCase);
  }

  @Test
  void rejectsMissingPartsOfQualifiedNames() {
    assertThatNullPointerException().isThrownBy(() -> CaseFragment.qualify("p", null)).withMessageContaining("qualified name");
    assertThatNullPointerException().isThrownBy(() -> CaseFragment.qualify(null, "name")).withMessageContaining("qualified name");
  }

  @Test
  void truncatesAliasBeforeAddingSuffixAndPreservesIdentifierQuotes() {
    var alias = new Alias(7, "_2");
    String[] names = { "alphabet", "[category]", "\"name\"", "`key`" };

    assertThat(alias.toAliasStrings(names)).containsExactly("alpha_2", "[categ_2]", "\"name_2\"", "`key_2`");
    assertThat(alias.toUnquotedAliasStrings(names)).containsExactly("alpha_2", "categ_2", "name_2", "key_2");
    assertThat(new Alias(5, null).toUnquotedAliasString("[category]")).isEqualTo("categ");
  }

}
