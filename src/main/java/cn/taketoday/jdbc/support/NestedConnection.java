package cn.taketoday.jdbc.support;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY
 */
final class NestedConnection extends WrappedConnection {
  private final static Logger log = LoggerFactory.getLogger(NestedConnection.class);

  private boolean autocommit = true;

  NestedConnection(Connection source) {
    super(source);
  }

  private boolean commited = false;

  @Override
  public void commit() throws SQLException {
    commited = true;
    //do nothing, parent connection should be committed
  }

  @Override
  public void rollback() throws SQLException {
    if (!commited) {
      log.warn("rollback of nested transaction leads to rollback of parent transaction. Maybe it is not wat you want.");
      super.rollback(); //probably it's worth to use savepoints
    }
  }

  @Override
  public void close() throws SQLException {
    //do nothing, parent connection should be closed by someone who cares
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    //do nothing, parent connection should be configured
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return autocommit;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    this.autocommit = autoCommit;
  }

}
