package cn.taketoday.jdbc;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * Created by lars on 16.09.2014.
 */
public class JndiDataSource {

  private static final Logger logger = LoggerFactory.getLogger(JndiDataSource.class);

  static DataSource getJndiDatasource(String jndiLookup) {
    Context ctx = null;
    DataSource datasource;

    try {
      ctx = new InitialContext();
      datasource = (DataSource) ctx.lookup(jndiLookup);
    }
    catch (NamingException e) {
      throw new RuntimeException(e);
    }
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        }
        catch (Throwable e) {
          logger.warn("error closing context", e);
        }
      }
    }

    return datasource;
  }
}
