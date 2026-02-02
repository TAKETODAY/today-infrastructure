package infra.web.client;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/2 13:59
 */
class DefaultApiVersionInserterTests {

  @Test
  void insertVersionViaPathPreservesExistingEncoding() {
    URI result = ApiVersionInserter.forPathSegment(0).insertVersion("1", URI.create("/path?q=%20"));
    assertThat(result.toString()).isEqualTo("/1/path?q=%20");
  }

  @Test
  void insertVersionViaQueryParamPreservesEncoding() {
    URI result = ApiVersionInserter.forQueryParam("version").insertVersion("1", URI.create("/path?q=%20"));
    assertThat(result.toString()).isEqualTo("/path?q=%20&version=1");
  }

}