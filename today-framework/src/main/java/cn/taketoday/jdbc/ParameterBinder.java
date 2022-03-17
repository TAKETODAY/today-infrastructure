package cn.taketoday.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter Setter
 *
 * @author TODAY 2021/1/7 15:09
 */
public abstract class ParameterBinder {

  /**
   * Bind a value to statement
   *
   * @param statement statement
   * @param paramIdx parameter index
   * @throws SQLException parameter set error
   */
  public abstract void bind(PreparedStatement statement, int paramIdx)
          throws SQLException;

  /**
   * null setter
   */
  public static final ParameterBinder null_binder = new ParameterBinder() {
    @Override
    public void bind(final PreparedStatement statement, final int paramIdx) throws SQLException {
      statement.setObject(paramIdx, null);
    }
  };

}
