package cn.taketoday.jdbc.data;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.result.AbstractResultSetIterator;
import cn.taketoday.jdbc.utils.JdbcUtils;

/**
 * @author aldenquimby@gmail.com
 */
public class TableResultSetIterator extends AbstractResultSetIterator<Row> {
  private final List<Column> columns;
  protected final boolean isCaseSensitive;
  private final ConversionService conversionService;
  private final Map<String, Integer> columnNameToIdxMap;

  public TableResultSetIterator(
          ResultSet rs, boolean isCaseSensitive, LazyTable lt, ConversionService conversionService) {
    super(rs);
    this.isCaseSensitive = isCaseSensitive;
    this.conversionService = conversionService;
    final ResultSetMetaData meta = JdbcUtils.getMetaData(rs);

    final ArrayList<Column> columns = new ArrayList<>();
    final HashMap<String, Integer> columnNameToIdxMap = new HashMap<>();
    try {
      lt.setName(meta.getTableName(1));
      lt.setColumns(columns);

      final int columnCount = meta.getColumnCount();
      for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
        String colName = getColumnName(meta, colIdx);
        String colType = meta.getColumnTypeName(colIdx);
        columns.add(new Column(colName, colIdx - 1, colType));

        String colMapName = isCaseSensitive ? colName : colName.toLowerCase();
        columnNameToIdxMap.put(colMapName, colIdx - 1);
      }
      this.columns = columns;
      this.columnNameToIdxMap = columnNameToIdxMap;
    }
    catch (SQLException e) {
      throw new PersistenceException("Error while reading metadata from database", e);
    }
  }

  protected String getColumnName(ResultSetMetaData meta, int colIdx) throws SQLException {
    return JdbcUtils.getColumnName(meta, colIdx);
  }

  @Override
  protected Row readNext() throws SQLException {
    final ResultSet rs = this.rs;
    final Row row = new Row(columnNameToIdxMap, columns.size(), isCaseSensitive, conversionService);
    for (Column column : columns) {
      final int index = column.getIndex();
      row.addValue(index, rs.getObject(index + 1));
    }
    return row;
  }
}
