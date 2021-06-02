package cn.taketoday.jdbc.data;

/**
 * Represents a result set column
 */
public class Column {

  private final String name;
  private final Integer index;
  private final String type;

  public Column(String name, Integer index, String type) {
    this.name = name;
    this.index = index;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public Integer getIndex() {
    return index;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return getName() + " (" + getType() + ")";
  }
}
