package cn.taketoday.jdbc.data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.jdbc.result.AbstractResultSetIterator;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.utils.JdbcUtils;

/**
 * @author aldenquimby@gmail.com
 */
public class TableResultSetIterator extends AbstractResultSetIterator<Row> {
  private final List<Column> columns;
  protected boolean isCaseSensitive;
  protected ResultSetMetaData meta;
  private final Map<String, Integer> columnNameToIdxMap;

  public TableResultSetIterator(ResultSet rs, boolean isCaseSensitive, LazyTable lt) {
    super(rs);
    this.isCaseSensitive = isCaseSensitive;
    this.columnNameToIdxMap = new HashMap<>();
    this.columns = new ArrayList<>();

    try {
      meta = rs.getMetaData();
    }
    catch (SQLException ex) {
      throw new PersistenceException("Database error: " + ex.getMessage(), ex);
    }
    try {
      lt.setName(meta.getTableName(1));

      final int columnCount = meta.getColumnCount();
      for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
        String colName = getColumnName(colIdx);
        String colType = meta.getColumnTypeName(colIdx);
        columns.add(new Column(colName, colIdx - 1, colType));

        String colMapName = isCaseSensitive ? colName : colName.toLowerCase();
        columnNameToIdxMap.put(colMapName, colIdx - 1);
      }
    }
    catch (SQLException e) {
      throw new PersistenceException("Error while reading metadata from database", e);
    }
    lt.setColumns(columns);
  }

  protected String getColumnName(int colIdx) throws SQLException {
    return JdbcUtils.getColumnName(meta, colIdx);
  }

  @Override
  protected Row readNext() throws SQLException {
    Row row = new Row(columnNameToIdxMap, columns.size(), isCaseSensitive);
    for (Column column : columns) {
      row.addValue(column.getIndex(), rs.getObject(column.getIndex() + 1));
    }
    return row;
  }
}
