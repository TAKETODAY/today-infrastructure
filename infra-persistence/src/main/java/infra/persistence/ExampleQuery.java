/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence;

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.logging.LogMessage;
import infra.persistence.annotation.Connector;
import infra.persistence.annotation.Group;
import infra.persistence.annotation.GroupExpression;
import infra.persistence.annotation.GroupOR;
import infra.persistence.annotation.OR;
import infra.persistence.platform.Platform;
import infra.persistence.sql.LogicalOperator;
import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.OrderSpecSource;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;

/**
 * A query statement that builds SQL conditions based on a non-null example object.
 * <p>
 * This class scans the properties of the provided example instance. For each non-null property,
 * it applies configured {@link PropertyConditionStrategy} instances to generate corresponding
 * {@link Condition} objects, then assembles them into a {@link ConditionTree} for the WHERE
 * clause. It also resolves the ORDER BY clause for the query.
 *
 * <p>The ORDER BY clause is resolved in the following priority order:
 * <ol>
 *   <li>{@link OrderSpecSource} — a programmatic spec contributed by the example object itself;</li>
 *   <li>class-level {@link infra.persistence.annotation.OrderByClause @OrderByClause} — a raw SQL fragment;</li>
 *   <li>property-level {@link infra.persistence.annotation.OrderBy @OrderBy} — ordered by each key's
 *   {@link infra.persistence.annotation.OrderBy#order() precedence}.</li>
 * </ol>
 * An earlier source wins over later ones. The ID property is scanned like any other
 * property and may therefore be ordered as well.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OrderSpecSource
 * @see infra.persistence.annotation.OrderBy
 * @see infra.persistence.annotation.OrderByClause
 * @since 4.0 2024/2/19 19:56
 */
final class ExampleQuery extends SimpleSelectQueryStatement
        implements QueryCondition, DebugDescriptive, ValueNormalizer {

  private final Object example;

  private final EntityMetadata exampleMetadata;

  private final List<PropertyConditionStrategy> strategies;

  private final List<ValueNormalizer> valueNormalizers;

  private @Nullable ConditionTree predicates;

  ExampleQuery(Object example, EntityMetadata exampleMetadata, List<PropertyConditionStrategy> strategies) {
    this.example = example;
    this.exampleMetadata = exampleMetadata;
    this.strategies = strategies;
    this.valueNormalizers = Collections.emptyList();
  }

  ExampleQuery(EntityMetadataFactory factory, Object example, List<PropertyConditionStrategy> strategies) {
    this(factory, example, strategies, List.of());
  }

  ExampleQuery(EntityMetadataFactory factory, Object example,
          List<PropertyConditionStrategy> strategies, List<ValueNormalizer> valueNormalizers) {
    this.example = example;
    this.strategies = strategies;
    this.valueNormalizers = valueNormalizers;
    this.exampleMetadata = factory.getEntityMetadata(example.getClass());
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
    for (Condition condition : where()) {
      select.addRestriction(condition);
    }
    select.orderBy(resolveOrderByClause(metadata));
  }

  @Override
  public void collectRestrictions(EntityMetadata metadata, List<Restriction> restrictions) {
    restrictions.addAll(where());
  }

  @Override
  public OrderSpec resolveOrderByClause(EntityMetadata metadata) {
    // 1. programmatic source takes precedence
    if (example instanceof OrderSpecSource source) {
      OrderSpec spec = source.orderSpec();
      if (!spec.isEmpty()) {
        return spec;
      }
    }
    // 2. declarative ordering, cached on the example's own metadata
    OrderSpec spec = QueryCondition.super.resolveOrderByClause(exampleMetadata);
    if (!spec.isEmpty()) {
      return spec;
    }
    // 3. fall back to the target entity metadata
    return QueryCondition.super.resolveOrderByClause(metadata);
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    int idx = 1;
    for (Condition condition : where()) {
      idx = condition.setParameter(statement, idx);
    }
  }

  @Override
  public Object normalize(EntityProperty entityProperty, Object value) {
    for (ValueNormalizer normalizer : valueNormalizers) {
      value = normalizer.normalize(entityProperty, value);
    }
    return value;
  }

  @Override
  public String getDescription() {
    return "Query entities with example";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format("Query entity using example: {}", example);
  }

  /**
   * Return the assembled {@link ConditionTree WHERE} tree, computed once.
   *
   * <p>{@link Group @Group}-annotated properties that share a name are collapsed
   * into a nested {@link ConditionGroup} here; each top-level term carries the
   * connector that joins it to the preceding one.
   *
   * @return the tree, or {@code null} when no property took part in the query
   */
  private @Nullable ConditionTree predicates() {
    ConditionTree predicates = this.predicates;
    if (predicates == null) {
      predicates = buildPredicates();
      this.predicates = predicates;
    }
    return predicates;
  }

  private @Nullable ConditionTree buildPredicates() {
    // @GroupExpression takes precedence over property-level @Group
    var exprAnnot = MergedAnnotations.from(exampleMetadata.getEntityClass()).get(GroupExpression.class);
    if (exprAnnot.isPresent()) {
      return buildExpressionPredicates(exprAnnot.getStringValue());
    }

    EntityProperty[] entityProperties = exampleMetadata.getEntityProperties(true);
    GroupNode root = new GroupNode();
    boolean present = false;

    for (EntityProperty property : entityProperties) {
      Condition condition = resolveCondition(property);
      if (condition == null) {
        continue;
      }
      present = true;
      MergedAnnotation<Group> annotation = property.getAnnotation(Group.class);
      LogicalOperator memberConnector = resolveConnector(property, false);
      if (annotation.isPresent()) {
        root.addGroup(annotation.getStringValue(),
                resolveConnector(property, true), memberConnector, condition);
      }
      else {
        root.addOccurrence(memberConnector, condition);
      }
    }
    return present ? root.toTree() : null;
  }

  private @Nullable ConditionTree buildExpressionPredicates(String expression) {
    var root = GroupExpressionParser.parse(expression);
    if (root instanceof GroupExpressionParser.Literal lit) {
      Condition cond = resolveExpressionLeaf(lit.name());
      return cond != null ? ConditionTree.of(cond) : null;
    }
    GroupExpressionParser.Group group = (GroupExpressionParser.Group) root;
    if (group.parenthesized()) {
      List<ConditionTree.Occurrence> occurrences = new ArrayList<>();
      for (var child : group.children()) {
        Condition cond = buildExpressionCondition(child);
        if (cond != null) {
          occurrences.add(new ConditionTree.Occurrence(group.connector(), cond));
        }
      }
      if (occurrences.isEmpty()) {
        return null;
      }
      return ConditionTree.of(ConditionGroup.of(occurrences));
    }
    // non-parenthesized root: render flat without outer parentheses
    List<Condition> conditions = new ArrayList<>();
    for (var child : group.children()) {
      Condition cond = buildExpressionCondition(child);
      if (cond != null) {
        conditions.add(cond);
      }
    }
    if (conditions.isEmpty()) {
      return null;
    }
    if (conditions.size() == 1) {
      return ConditionTree.of(conditions.get(0));
    }
    return ConditionTree.of(new FlatCondition(group.connector(), conditions));
  }

  private @Nullable Condition buildExpressionCondition(GroupExpressionParser.Node node) {
    if (node instanceof GroupExpressionParser.Literal lit) {
      return resolveExpressionLeaf(lit.name());
    }
    GroupExpressionParser.Group group = (GroupExpressionParser.Group) node;
    List<ConditionTree.Occurrence> occurrences = new ArrayList<>();
    for (var child : group.children()) {
      Condition cond = buildExpressionCondition(child);
      if (cond != null) {
        occurrences.add(new ConditionTree.Occurrence(group.connector(), cond));
      }
    }
    if (occurrences.isEmpty()) {
      return null;
    }
    if (group.parenthesized()) {
      if (occurrences.size() == 1) {
        return new ParenthesizedCondition(occurrences.get(0).condition());
      }
      return ConditionGroup.of(occurrences);
    }
    if (occurrences.size() == 1) {
      return occurrences.get(0).condition();
    }
    return ConditionGroup.of(occurrences);
  }

  private @Nullable Condition resolveExpressionLeaf(String propertyName) {
    EntityProperty prop = exampleMetadata.findProperty(propertyName);
    if (prop == null) {
      throw new IllegalEntityException("Property '" + propertyName
              + "' referenced in @GroupExpression not found in "
              + exampleMetadata.getEntityClass());
    }
    Object value = prop.getValue(example);
    if (value == null) {
      return null;
    }
    return resolveCondition(prop);
  }

  /**
   * Resolve a connector from the property's {@link Connector @Connector} (or its
   * meta-annotations {@link OR @OR} / {@link GroupOR @GroupOR}), defaulting to
   * {@code AND}.
   *
   * @param property the entity property
   * @param group {@code true} to read the group connector, {@code false} the member connector
   * @return the resolved operator
   */
  private static LogicalOperator resolveConnector(EntityProperty property, boolean group) {
    MergedAnnotation<Connector> connector = property.getAnnotation(Connector.class);
    if (connector.isPresent() && connector.getBoolean("group") == group) {
      return connector.getEnum("value", LogicalOperator.class);
    }
    return LogicalOperator.AND;
  }

  private @Nullable Condition resolveCondition(EntityProperty property) {
    Object propertyValue = property.getValue(example);

    for (PropertyConditionStrategy strategy : strategies) {
      Condition condition = propertyValue == null
              ? strategy.resolve(property)
              : strategy.resolve(property, propertyValue, this);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

  /**
   * Resolve the conditions to render and bind.
   *
   * <p>The {@link ConditionTree} drives both rendering and {@link #setParameter},
   * so placeholders are bound in exactly the order they are rendered.
   *
   * @return the top-level conditions, never {@code null}
   */
  private List<Condition> where() {
    ConditionTree predicates = predicates();
    return predicates == null ? List.of() : List.of(predicates);
  }

  /**
   * A node in the tree of {@link Group @Group}s.
   *
   * <p>A group name may be a dot-separated path ({@code "outer.inner"}); every
   * segment becomes a level, so groups nest arbitrarily deep. Connectors are
   * uniform: a member joins the preceding one with {@code AND} unless it carries
   * {@link OR @OR}; the group's link to the preceding term is declared with
   * {@link GroupOR @GroupOR} or {@link Connector @Connector}.
   */
  private static final class GroupNode {

    // ordered children: a term or a nested group; type-safe, no casts
    final List<Member> children = new ArrayList<>();

    final Map<String, GroupNode> subgroups = new LinkedHashMap<>();

    /**
     * The connector joining this group to the preceding top-level term,
     * declared by the first {@link Group @Group} that names it; falls back to
     * {@code AND}. Kept {@code null} until a declaration supplies one.
     */
    @Nullable LogicalOperator connector;

    void addOccurrence(LogicalOperator connector, Condition condition) {
      children.add(new Term(condition, connector));
    }

    void addGroup(String path, LogicalOperator groupConnector,
            LogicalOperator memberConnector, Condition condition) {
      GroupNode node = this;
      for (String segment : path.split("\\.")) {
        GroupNode child = node.subgroups.get(segment);
        if (child == null) {
          child = new GroupNode();
          node.subgroups.put(segment, child);
          node.children.add(new Subgroup(child));
        }
        node = child;
      }
      if (node.connector == null) {
        node.connector = groupConnector;
      }
      node.children.add(new Term(condition, memberConnector));
    }

    ConditionTree toTree() {
      List<ConditionTree.Occurrence> occurrences = new ArrayList<>(children.size());
      for (Member member : children) {
        occurrences.add(occurrenceOf(member));
      }
      return ConditionTree.of(occurrences);
    }

    Condition toCondition() {
      List<ConditionTree.Occurrence> occurrences = new ArrayList<>(children.size());
      for (Member member : children) {
        occurrences.add(occurrenceOf(member));
      }
      return ConditionGroup.of(occurrences);
    }

    private ConditionTree.Occurrence occurrenceOf(Member member) {
      if (member instanceof Subgroup subgroup) {
        GroupNode node = subgroup.node;
        return new ConditionTree.Occurrence(
                node.connector != null ? node.connector : LogicalOperator.AND,
                node.toCondition());
      }
      Term term = (Term) member;
      return new ConditionTree.Occurrence(
              term.connector != null ? term.connector : LogicalOperator.AND,
              term.condition);
    }

    private sealed interface Member {
    }

    private record Term(Condition condition, @Nullable LogicalOperator connector) implements Member {
    }

    private record Subgroup(GroupNode node) implements Member {
    }

  }

  /**
   * A condition that always renders within parentheses, even when it wraps a
   * single member. Used to preserve an explicitly parenthesized group such as
   * {@code (a)} in a {@link GroupExpression @GroupExpression}.
   *
   * @param inner the wrapped condition
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  private record ParenthesizedCondition(Condition inner) implements Condition {

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      sqlBuffer.append('(');
      inner.render(platform, sqlBuffer);
      sqlBuffer.append(')');
    }

    @Override
    public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
      return inner.setParameter(ps, parameterIndex);
    }
  }

  /**
   * Renders its children flat, joined by the given connector, without any
   * outer parentheses. Used for the non-parenthesized root of a
   * {@link GroupExpression @GroupExpression}.
   *
   * @param connector the connector joining the children
   * @param children the ordered conditions
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  private record FlatCondition(LogicalOperator connector, List<Condition> children) implements Condition {

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      children.get(0).render(platform, sqlBuffer);
      for (int i = 1; i < children.size(); i++) {
        connector.render(platform, sqlBuffer);
        children.get(i).render(platform, sqlBuffer);
      }
    }

    @Override
    public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
      for (Condition child : children) {
        parameterIndex = child.setParameter(ps, parameterIndex);
      }
      return parameterIndex;
    }
  }

}