package cn.taketoday.jdbc.support;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.JdbcOperations;

/**
 * Represents a method with a {@link JdbcConnection} and an optional argument.
 * Implementations of this interface be used as a parameter to one of the
 * {@link JdbcOperations#runInTransaction(StatementRunnable) Sql2o.runInTransaction}
 * overloads, to run code safely in a transaction.
 */
public interface StatementRunnable {

  void run(JdbcConnection connection, Object argument) throws Throwable;
}
