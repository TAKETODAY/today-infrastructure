package cn.taketoday.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author TODAY
 * @date 2021/1/7 15:09
 */
@FunctionalInterface
public interface ParameterSetter {

  default int getParameterCount() {
    return 1;
  }

  void setParameter(PreparedStatement statement, int paramIdx) throws SQLException;

  /**
   * null setter
   */
  ParameterSetter null_setter = new ParameterSetter() {
    @Override
    public void setParameter(final PreparedStatement statement, final int paramIdx) throws SQLException {
      statement.setObject(paramIdx, null);
    }
  };

}
