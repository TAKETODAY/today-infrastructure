package cn.taketoday.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter Setter
 *
 * @author TODAY 2021/1/7 15:09
 */
public abstract class ParameterSetter {

  public abstract void setParameter(PreparedStatement statement, int paramIdx)
          throws SQLException;

  /**
   * null setter
   */
  public static final ParameterSetter null_setter = new ParameterSetter() {
    @Override
    public void setParameter(final PreparedStatement statement, final int paramIdx) throws SQLException {
      statement.setObject(paramIdx, null);
    }
  };

}
