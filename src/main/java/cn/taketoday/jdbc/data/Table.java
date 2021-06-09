package cn.taketoday.jdbc.data;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

/**
 * Represents an offline result set with columns and rows and data.
 */
public class Table {
  private final String name;
  private final List<Row> rows;
  private final List<Column> columns;

  public Table(String name, List<Row> rows, List<Column> columns) {
    this.name = name;
    this.rows = rows;
    this.columns = columns;
  }

  public String getName() {
    return name;
  }

  public List<Row> rows() {
    return rows;
  }

  public List<Column> columns() {
    return columns;
  }

  public List<Map<String, Object>> asList() {
    return new AbstractList<Map<String, Object>>() {
      @Override
      public Map<String, Object> get(int index) {
        return rows.get(index).asMap();
      }

      @Override
      public int size() {
        return rows.size();
      }
    };
  }
}
