package infra.freemarker.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 20:28
 */
class FreeMarkerPropertiesTests {

  @Test
  void defaultContentType() {
    assertThat(new FreeMarkerProperties().getContentType()).hasToString("text/html;charset=UTF-8");
  }

  @Test
  void customContentTypeDefaultCharset() {
    FreeMarkerProperties properties = new FreeMarkerProperties();
    properties.setContentType(MimeTypeUtils.parseMimeType("text/plain"));
    assertThat(properties.getContentType()).hasToString("text/plain;charset=UTF-8");
  }

  @Test
  void defaultContentTypeCustomCharset() {
    FreeMarkerProperties properties = new FreeMarkerProperties();
    properties.setCharset(StandardCharsets.UTF_16);
    assertThat(properties.getContentType()).hasToString("text/html;charset=UTF-16");
  }

  @Test
  void customContentTypeCustomCharset() {
    FreeMarkerProperties properties = new FreeMarkerProperties();
    properties.setContentType(MimeTypeUtils.parseMimeType("text/plain"));
    properties.setCharset(StandardCharsets.UTF_16);
    assertThat(properties.getContentType()).hasToString("text/plain;charset=UTF-16");
  }

  @Test
  void customContentTypeWithPropertyAndCustomCharset() {
    FreeMarkerProperties properties = new FreeMarkerProperties();
    properties.setContentType(MimeTypeUtils.parseMimeType("text/plain;foo=bar"));
    properties.setCharset(StandardCharsets.UTF_16);
    assertThat(properties.getContentType()).hasToString("text/plain;charset=UTF-16;foo=bar");
  }

}