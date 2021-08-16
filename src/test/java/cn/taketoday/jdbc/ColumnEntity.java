package cn.taketoday.jdbc;

import cn.taketoday.beans.Property;

public class ColumnEntity {

  private int id;
  @Property("text_col")
  private String text;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

}
