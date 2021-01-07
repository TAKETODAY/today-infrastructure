package cn.taketoday.jdbc;

import cn.taketoday.jdbc.Query;

/**
 * Created by zsoltjanos on 01/08/15.
 */
public interface UserInserter {

  public void insertUser(Query insertQuery, int idx);
}
