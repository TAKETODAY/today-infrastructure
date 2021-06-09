package cn.taketoday.jdbc.data;

/**
 * Represents a result set column
 */
public class Column {

  private final String name;
  private final int index;
  private final String type;

  public Column(String name, int index, String type) {
    this.name = name;
    this.index = index;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public int getIndex() {
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
