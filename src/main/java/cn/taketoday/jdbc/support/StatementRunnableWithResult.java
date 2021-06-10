package cn.taketoday.jdbc.support;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.JdbcOperations;

/**
 * Represents a method with a {@link JdbcConnection} and an optional argument.
 * Implementations of this interface be used as a parameter to one of the
 * {@link JdbcOperations#runInTransaction(StatementRunnableWithResult<V>)}
 * Sql2o.runInTransaction} overloads, to run code safely in a transaction.
 */
public interface StatementRunnableWithResult<V> {

  V run(JdbcConnection connection, Object argument) throws Throwable;
}
