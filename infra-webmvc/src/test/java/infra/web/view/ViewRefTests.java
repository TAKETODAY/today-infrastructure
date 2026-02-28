package infra.web.view;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/28 18:17
 */
class ViewRefTests {

  @Test
  void forViewName() {
    ViewRef viewRef = ViewRef.forViewName("demo", null);
    assertThat(viewRef.getViewName()).isEqualTo("demo");
    assertThat(viewRef.getLocale()).isNull();
  }
}