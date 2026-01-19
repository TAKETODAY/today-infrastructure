/**
 * Simplification layer for table inserts and stored procedure calls.
 *
 * <p>{@code SimpleJdbcInsert} and {@code SimpleJdbcCall} take advantage of database
 * meta-data provided by the JDBC driver to simplify the application code. Much of the
 * parameter specification becomes unnecessary since it can be looked up in the meta-data.
 */
@NullMarked
package infra.jdbc.core.simple;

import org.jspecify.annotations.NullMarked;
