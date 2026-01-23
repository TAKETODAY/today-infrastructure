package infra.http.client.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.http.client.HttpClientSettings;
import infra.http.client.HttpRedirects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 22:35
 */
class HttpClientSettingsPropertyMapperTests {

  @Test
  void mapWhenPropertiesIsNullAndBaseSettingsIsNullReturnsDefaults() {
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
    HttpClientSettings result = mapper.map(null);
    assertThat(result).isEqualTo(HttpClientSettings.defaults());
  }

  @Test
  void mapWhenPropertiesIsNullReturnsBaseSettings() {
    HttpClientSettings baseSettings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(10));
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, baseSettings);
    HttpClientSettings result = mapper.map(null);
    assertThat(result).isEqualTo(baseSettings);
  }

  @Test
  void mapMapsRedirects() {
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
    TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
    properties.redirects = (HttpRedirects.DONT_FOLLOW);
    HttpClientSettings result = mapper.map(properties);
    assertThat(result.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
  }

  @Test
  void mapMapsConnectTimeout() {
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
    TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
    properties.connectTimeout = (Duration.ofSeconds(5));
    HttpClientSettings result = mapper.map(properties);
    assertThat(result.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void mapMapsReadTimeout() {
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
    TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
    properties.readTimeout = (Duration.ofSeconds(30));
    HttpClientSettings result = mapper.map(properties);
    assertThat(result.readTimeout()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void mapMapsSslBundle() {
    SslBundle sslBundle = mock(SslBundle.class);
    SslBundles sslBundles = mock(SslBundles.class);
    given(sslBundles.getBundle("test-bundle")).willReturn(sslBundle);
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(sslBundles, null);
    TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
    properties.ssl.bundle = ("test-bundle");
    HttpClientSettings result = mapper.map(properties);
    assertThat(result.sslBundle()).isSameAs(sslBundle);
  }

  @Test
  void mapUsesBaseSettingsForMissingProperties() {
    HttpClientSettings baseSettings = new HttpClientSettings(HttpRedirects.FOLLOW_WHEN_POSSIBLE,
            Duration.ofSeconds(15), Duration.ofSeconds(25), null);
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, baseSettings);
    TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
    properties.connectTimeout = (Duration.ofSeconds(5));
    HttpClientSettings result = mapper.map(properties);
    assertThat(result.redirects()).isEqualTo(HttpRedirects.FOLLOW_WHEN_POSSIBLE);
    assertThat(result.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(result.readTimeout()).isEqualTo(Duration.ofSeconds(25));
  }

  @Test
  void mapWhenSslBundleRequestedButSslBundlesIsNullThrowsException() {
    HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
    TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
    properties.ssl.bundle = ("test-bundle");
    assertThatIllegalStateException().isThrownBy(() -> mapper.map(properties))
            .withMessage("No 'sslBundles' available");
  }

  static class TestHttpClientSettingsProperties extends HttpClientSettingsProperties {

  }

}