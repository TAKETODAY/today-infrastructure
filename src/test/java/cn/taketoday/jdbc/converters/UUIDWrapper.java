package cn.taketoday.jdbc.converters;

import java.util.Objects;
import java.util.UUID;

/**
 * @author aldenquimby@gmail.com
 * @since 4.0/14
 */
public class UUIDWrapper {
  private UUID text;

  public UUIDWrapper() { }

  public UUIDWrapper(UUID text) {
    this.text = text;
  }

  public UUID getText() {
    return text;
  }

  public void setText(UUID text) {
    this.text = text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UUIDWrapper that = (UUIDWrapper) o;
    return Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return text != null ? text.hashCode() : 0;
  }
}
