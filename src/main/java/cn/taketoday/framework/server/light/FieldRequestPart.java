package cn.taketoday.framework.server.light;

import cn.taketoday.web.http.HttpHeaders;

/**
 * @author TODAY 2021/4/17 15:08
 */
public class FieldRequestPart extends RequestPart {
  private String value;
  private final byte[] bytes;

  protected FieldRequestPart(byte[] bytes, HttpHeaders httpHeaders) {
    super(httpHeaders);
    this.bytes = bytes;
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
}
