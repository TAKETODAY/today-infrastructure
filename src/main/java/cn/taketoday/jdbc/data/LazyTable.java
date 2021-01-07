package cn.taketoday.jdbc.data;

import java.util.List;

import cn.taketoday.jdbc.ResultSetIterable;

/**
 * @author aldenquimby@gmail.com
 */
public class LazyTable implements AutoCloseable {
  private String name;
  private ResultSetIterable<Row> rows;
  private List<Column> columns;

  public String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  public Iterable<Row> rows() {
    return rows;
  }

  public void setRows(ResultSetIterable<Row> rows) {
    this.rows = rows;
  }

  public List<Column> columns() {
    return columns;
  }

  void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  public void close() {
    this.rows.close();
  }
}
