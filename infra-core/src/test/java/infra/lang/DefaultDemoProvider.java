package infra.lang;

import java.util.Objects;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/3 21:04
 */
public class DefaultDemoProvider implements DemoProvider {

  int pro;

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DefaultDemoProvider that))
      return false;
    return pro == that.pro;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pro);
  }
}
