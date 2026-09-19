/**
 * Annotations for declaring entity mappings and query conditions used by the
 * JDBC-based persistence support.
 *
 * <p>Entity mapping annotations describe how a class maps to a database table:
 * {@link Table} overrides the table name, {@link Id} and {@link GeneratedId} mark
 * the primary key, {@link Column} customizes column names, {@link Transient}
 * excludes properties from mapping, {@link Version} enables optimistic locking,
 * and {@link EntityRef} maps an entity onto the primary table of another entity.
 *
 * <p>Query condition annotations drive dynamic condition building for example
 * based queries: {@link Where}, {@link Trim}, {@link Like},
 * {@link PrefixLike}, {@link SuffixLike}, {@link OR} and {@link OrderBy}.
 *
 * <p>Update behavior is customized with {@link UpdateBy}, which marks the
 * properties used as the matching condition when updating an entity.
 *
 * @since 5.0
 */
@NullMarked
package infra.persistence.annotation;

import org.jspecify.annotations.NullMarked;
