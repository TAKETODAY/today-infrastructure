package cn.taketoday.framework.server.light;

/**
 * @author TODAY 2021/4/17 15:08
 */
public class FieldRequestPart implements RequestPart {
  private String value;
  private final byte[] bytes;
  private final String name;

  protected FieldRequestPart(byte[] bytes, String name) {
    this.bytes = bytes;
    this.name = name;
  }

  public String getStringValue() {
    if (value == null) {
      value = new String(bytes);
    }
    return value;
  }

  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public String getName() {
    return name;
  }
}
