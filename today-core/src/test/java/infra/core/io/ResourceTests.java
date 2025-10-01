/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.io;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import infra.util.FileCopyUtils;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author TODAY 2021/3/9 20:14
 */
class ResourceTests {

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceIsValid(Resource resource) throws Exception {
    assertThat(resource.getName()).isEqualTo("ResourceTests.class");
    assertThat(resource.getURL().getFile()).endsWith("ResourceTests.class");
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isReadable()).isTrue();
    assertThat(resource.contentLength()).isGreaterThan(0);
    assertThat(resource.lastModified()).isGreaterThan(0);
    assertThat(resource.getContentAsByteArray()).containsExactly(Files.readAllBytes(Path.of(resource.getURI())));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelative(Resource resource) throws Exception {
    Resource relative1 = resource.createRelative("ClassPathResourceTests.class");
    assertThat(relative1.getName()).isEqualTo("ClassPathResourceTests.class");
    assertThat(relative1.getURL().getFile().endsWith("ClassPathResourceTests.class")).isTrue();
    assertThat(relative1.exists()).isTrue();
    assertThat(relative1.isReadable()).isTrue();
    assertThat(relative1.contentLength()).isGreaterThan(0);
    assertThat(relative1.lastModified()).isGreaterThan(0);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelativeWithFolder(Resource resource) throws Exception {
    Resource relative2 = resource.createRelative("PathMatchingPatternResourceLoaderTests.class");
    assertThat(relative2.getName()).isEqualTo("PathMatchingPatternResourceLoaderTests.class");
    assertThat(relative2.getURL().getFile()).endsWith("PathMatchingPatternResourceLoaderTests.class");
    assertThat(relative2.exists()).isTrue();
    assertThat(relative2.isReadable()).isTrue();
    assertThat(relative2.contentLength()).isGreaterThan(0);
    assertThat(relative2.lastModified()).isGreaterThan(0);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelativeWithDotPath(Resource resource) throws Exception {
    Resource relative3 = resource.createRelative("../../util/CollectionUtilsTests.class");
    assertThat(relative3.getName()).isEqualTo("CollectionUtilsTests.class");
    assertThat(relative3.getURL().getFile()).endsWith("CollectionUtilsTests.class");
    assertThat(relative3.exists()).isTrue();
    assertThat(relative3.isReadable()).isTrue();
    assertThat(relative3.contentLength()).isGreaterThan(0);
    assertThat(relative3.lastModified()).isGreaterThan(0);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelativeUnknown(Resource resource) throws Exception {
    Resource relative4 = resource.createRelative("X.class");
    assertThat(relative4.exists()).isFalse();
    assertThat(relative4.isReadable()).isFalse();
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::contentLength);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::lastModified);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::getInputStream);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::readableChannel);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::getContentAsByteArray);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> relative4.getContentAsString(UTF_8));
  }

  @Test
  void defaultsMethods() {
    Resource resource = new Resource() {
      @Nullable
      @Override
      public String getName() {
        return "";
      }

      @Override
      public long contentLength() throws IOException {
        return 0;
      }

      @Override
      public long lastModified() throws IOException {
        return 0;
      }

      @Override
      public URL getURL() throws IOException {
        return null;
      }

      @Override
      public URI getURI() throws IOException {
        return null;
      }

      @Override
      public File getFile() throws IOException {
        return null;
      }

      @Override
      public boolean exists() {
        return true;
      }

      @Override
      public Resource createRelative(String relativePath) throws IOException {
        return null;
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return null;
      }
    };
    assertThat(resource.isFile()).isFalse();
    assertThat(resource.isOpen()).isFalse();
    assertThat(resource.isReadable()).isTrue();
  }

  private static Stream<Arguments> resource() throws URISyntaxException {
    URL resourceClass = ResourceTests.class.getResource("ResourceTests.class");
    Path resourceClassFilePath = Paths.get(resourceClass.toURI());
    return Stream.of(
            arguments(named("ClassPathResource", new ClassPathResource("infra/core/io/ResourceTests.class"))),
            arguments(named("ClassPathResource with ClassLoader", new ClassPathResource("infra/core/io/ResourceTests.class", ResourceTests.class.getClassLoader()))),
            arguments(named("ClassPathResource with Class", new ClassPathResource("ResourceTests.class", ResourceTests.class))),
            arguments(named("FileSystemResource", new FileSystemResource(resourceClass.getFile()))),
            arguments(named("FileSystemResource with File", new FileSystemResource(new File(resourceClass.getFile())))),
            arguments(named("FileSystemResource with File path", new FileSystemResource(resourceClassFilePath))),
            arguments(named("UrlResource", new UrlResource(resourceClass)))
    );
  }

  @Nested
  class ByteArrayResourceTests {

    @Test
    void hasContent() throws Exception {
      String testString = "testString";
      byte[] testBytes = testString.getBytes();
      Resource resource = new ByteArrayResource(testBytes);
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isFalse();
      byte[] contentBytes = resource.getContentAsByteArray();
      assertThat(contentBytes).containsExactly(testBytes);
      String contentString = resource.getContentAsString(StandardCharsets.US_ASCII);
      assertThat(contentString).isEqualTo(testString);
      contentString = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
      assertThat(contentString).isEqualTo(testString);
      assertThat(new ByteArrayResource(testBytes)).isEqualTo(resource);
    }

    @Test
    void isNotOpen() {
      Resource resource = new ByteArrayResource("testString".getBytes());
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isFalse();
    }

    @Test
    void hasDescription() {
      Resource resource = new ByteArrayResource("testString".getBytes(), "my description");
      assertThat(resource.toString()).contains("my description");
    }
  }

  @Nested
  class InputStreamResourceTests {

    @Test
    void hasContent() throws Exception {
      String testString = "testString";
      byte[] testBytes = testString.getBytes();
      InputStream is = new ByteArrayInputStream(testBytes);
      Resource resource1 = new InputStreamResource(is);
      String content = FileCopyUtils.copyToString(new InputStreamReader(resource1.getInputStream()));
      assertThat(content).isEqualTo(testString);
      assertThat(new InputStreamResource(is)).isEqualTo(resource1);
      assertThat(new InputStreamResource(() -> is)).isNotEqualTo(resource1);
      assertThatIllegalStateException().isThrownBy(resource1::getInputStream);

      Resource resource2 = new InputStreamResource(new ByteArrayInputStream(testBytes));
      assertThat(resource2.getContentAsByteArray()).containsExactly(testBytes);
      assertThatIllegalStateException().isThrownBy(resource2::getContentAsByteArray);

      AtomicBoolean obtained = new AtomicBoolean();
      Resource resource3 = new InputStreamResource(() -> {
        obtained.set(true);
        return new ByteArrayInputStream(testBytes);
      });
      assertThat(obtained).isFalse();
      assertThat(resource3.getContentAsString(StandardCharsets.US_ASCII)).isEqualTo(testString);
      assertThat(obtained).isTrue();
      assertThatIllegalStateException().isThrownBy(() -> resource3.getContentAsString(StandardCharsets.US_ASCII));
    }

    @Test
    void isOpen() {
      InputStream is = new ByteArrayInputStream("testString".getBytes());
      Resource resource = new InputStreamResource(is);
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isTrue();

      resource = new InputStreamResource(() -> is);
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isTrue();
    }

    @Test
    void hasDescription() {
      InputStream is = new ByteArrayInputStream("testString".getBytes());
      Resource resource = new InputStreamResource(is, "my description");
      assertThat(resource.toString()).contains("my description");

      resource = new InputStreamResource(() -> is, "my description");
      assertThat(resource.toString()).contains("my description");
    }
  }

  @Nested
  class FileSystemResourceTests {

    @Test
    void sameResourceIsEqual() {
      String file = getClass().getResource("ResourceTests.class").getFile();
      Resource resource = new FileSystemResource(file);
      assertThat(resource).isEqualTo(new FileSystemResource(file));
    }

    @Test
    void sameResourceFromFileIsEqual() {
      File file = new File(getClass().getResource("ResourceTests.class").getFile());
      Resource resource = new FileSystemResource(file);
      assertThat(resource).isEqualTo(new FileSystemResource(file));
    }

    @Test
    void sameResourceFromFilePathIsEqual() throws Exception {
      Path filePath = Paths.get(getClass().getResource("ResourceTests.class").toURI());
      Resource resource = new FileSystemResource(filePath);
      assertThat(resource).isEqualTo(new FileSystemResource(filePath));
    }

    @Test
    void sameResourceFromDotPathIsEqual() {
      Resource resource = new FileSystemResource("core/io/ResourceTests.class");
      assertThat(new FileSystemResource("core/../core/io/./ResourceTests.class")).isEqualTo(resource);
    }

    @Test
    void relativeResourcesAreEqual() throws Exception {
      Resource resource = new FileSystemResource("dir/");
      Resource relative = resource.createRelative("subdir");
      assertThat(relative).isEqualTo(new FileSystemResource("dir/subdir"));
    }

    @Test
    void getFilePath() throws Exception {
      Path path = mock();
      given(path.normalize()).willReturn(path);
      given(path.toFile()).willThrow(new UnsupportedOperationException());
      Resource resource = new FileSystemResource(path);
      assertThat(resource.getFilePath()).isSameAs(path);
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(resource::getFile);
    }

    @Test
    void readableChannelProvidesContent() throws Exception {
      Resource resource = new FileSystemResource(getClass().getResource("ResourceTests.class").getFile());
      try (ReadableByteChannel channel = resource.readableChannel()) {
        ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
        channel.read(buffer);
        buffer.rewind();
        assertThat(buffer.limit()).isGreaterThan(0);
      }
    }

    @Test
    void urlAndUriAreNormalizedWhenCreatedFromFile() throws Exception {
      Path path = Path.of("src/test/resources/scanned-resources/resource#test1.txt").toAbsolutePath();
      assertUrlAndUriBehavior(new FileSystemResource(path.toFile()));
    }

    @Test
    void urlAndUriAreNormalizedWhenCreatedFromPath() throws Exception {
      Path path = Path.of("src/test/resources/scanned-resources/resource#test1.txt").toAbsolutePath();
      assertUrlAndUriBehavior(new FileSystemResource(path));
    }

    /**
     * The following assertions serve as regression tests for the lack of the
     * "authority component" (//) in the returned URI/URL. For example, we are
     * expecting file:/my/path (or file:/C:/My/Path) instead of file:///my/path.
     */
    private void assertUrlAndUriBehavior(Resource resource) throws IOException {
      assertThat(resource.getURL().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
      assertThat(resource.getURI().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
    }
  }

  @Nested
  class UrlResourceTests {

    private static final String LAST_MODIFIED = "Wed, 09 Apr 2014 09:57:42 GMT";

    private final MockWebServer server = new MockWebServer();

    @Test
    void sameResourceWithRelativePathIsEqual() throws Exception {
      Resource resource = new UrlResource("file:core/io/ResourceTests.class");
      assertThat(new UrlResource("file:core/../core/io/./ResourceTests.class")).isEqualTo(resource);
    }

    @Test
    void filenameIsExtractedFromFilePath() throws Exception {
      assertThat(new UrlResource("file:test?argh").getName()).isEqualTo("test");
      assertThat(new UrlResource("file:/test?argh").getName()).isEqualTo("test");
      assertThat(new UrlResource("file:test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(new UrlResource("file:/test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(new UrlResource("file:/dir/test?argh").getName()).isEqualTo("test");
      assertThat(new UrlResource("file:/dir/test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(new UrlResource("file:\\dir\\test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(new UrlResource("file:\\dir/test.txt?argh").getName()).isEqualTo("test.txt");
    }

    @Test
    void filenameIsExtractedFromURL() throws Exception {
      assertThat(new UrlResource(new URL("file:test?argh")).getName()).isEqualTo("test");
      assertThat(new UrlResource(new URL("file:/test?argh")).getName()).isEqualTo("test");
      assertThat(new UrlResource(new URL("file:test.txt?argh")).getName()).isEqualTo("test.txt");
      assertThat(new UrlResource(new URL("file:/test.txt?argh")).getName()).isEqualTo("test.txt");
      assertThat(new UrlResource(new URL("file:/dir/test?argh")).getName()).isEqualTo("test");
      assertThat(new UrlResource(new URL("file:/dir/test.txt?argh")).getName()).isEqualTo("test.txt");
      assertThat(new UrlResource(new URL("file:\\dir\\test.txt?argh")).getName()).isEqualTo("test.txt");
      assertThat(new UrlResource(new URL("file:\\dir/test.txt?argh")).getName()).isEqualTo("test.txt");
    }

    @Test
    void filenameContainingHashTagIsExtractedFromFilePathUnencoded() throws Exception {
      String unencodedPath = "/dir/test#1.txt";
      String encodedPath = "/dir/test%231.txt";

      URI uri = new URI("file", unencodedPath, null);
      URL url = uri.toURL();
      assertThat(uri.getPath()).isEqualTo(unencodedPath);
      assertThat(uri.getRawPath()).isEqualTo(encodedPath);
      assertThat(url.getPath()).isEqualTo(encodedPath);

      UrlResource urlResource = new UrlResource(url);
      assertThat(urlResource.getURI().getPath()).isEqualTo(unencodedPath);
      assertThat(urlResource.getName()).isEqualTo("test#1.txt");
    }

    @Test
    void factoryMethodsProduceEqualResources() throws Exception {
      Resource resource1 = new UrlResource("file:core/io/ResourceTests.class");
      Resource resource2 = UrlResource.from("file:core/io/ResourceTests.class");
      Resource resource3 = UrlResource.from(resource1.getURI());

      assertThat(resource2.getURL()).isEqualTo(resource1.getURL());
      assertThat(resource3.getURL()).isEqualTo(resource1.getURL());

      assertThat(UrlResource.from("file:core/../core/io/./ResourceTests.class")).isEqualTo(resource1);
      assertThat(UrlResource.from("file:/dir/test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(UrlResource.from("file:\\dir\\test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(UrlResource.from("file:\\dir/test.txt?argh").getName()).isEqualTo("test.txt");
    }

    @Test
    void relativeResourcesAreEqual() throws Exception {
      Resource resource = new UrlResource("file:dir/");
      Resource relative = resource.createRelative("subdir");
      assertThat(relative).isEqualTo(new UrlResource("file:dir/subdir"));
    }

    @Test
    void unusualRelativeResourcesAreEqual() throws Exception {
      Resource resource = new UrlResource("file:dir/");
      Resource relative = resource.createRelative("https://github.io");
      assertThat(relative).isEqualTo(new UrlResource("file:dir/https://github.io"));
    }

    @Test
    void missingRemoteResourceDoesNotExist() throws Exception {
      String baseUrl = startServer(true);
      UrlResource resource = new UrlResource(baseUrl + "/missing");
      assertThat(resource.exists()).isFalse();
    }

    @Test
    void remoteResourceExists() throws Exception {
      String baseUrl = startServer(true);
      UrlResource resource = new UrlResource(baseUrl + "/resource");
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isReadable()).isTrue();
      assertThat(resource.contentLength()).isEqualTo(6);
      assertThat(resource.lastModified()).isGreaterThan(0);
    }

    @Test
    void remoteResourceExistsFallback() throws Exception {
      String baseUrl = startServer(false);
      UrlResource resource = new UrlResource(baseUrl + "/resource");
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isReadable()).isTrue();
      assertThat(resource.contentLength()).isEqualTo(5);
      assertThat(resource.lastModified()).isGreaterThan(0);
    }

    @Test
    void canCustomizeHttpUrlConnectionForExists() throws Exception {
      String baseUrl = startServer(true);
      CustomResource resource = new CustomResource(baseUrl + "/resource");
      assertThat(resource.exists()).isTrue();
      RecordedRequest request = this.server.takeRequest();
      assertThat(request.getMethod()).isEqualTo("HEAD");
      assertThat(request.getHeader("Framework-Name")).isEqualTo("Infra");
    }

    @Test
    void canCustomizeHttpUrlConnectionForExistsFallback() throws Exception {
      String baseUrl = startServer(false);
      CustomResource resource = new CustomResource(baseUrl + "/resource");
      assertThat(resource.exists()).isTrue();
      RecordedRequest request = this.server.takeRequest();
      assertThat(request.getMethod()).isEqualTo("HEAD");
      assertThat(request.getHeader("Framework-Name")).isEqualTo("Infra");
    }

    @Test
    void canCustomizeHttpUrlConnectionForRead() throws Exception {
      String baseUrl = startServer(true);
      CustomResource resource = new CustomResource(baseUrl + "/resource");
      assertThat(resource.getInputStream()).hasContent("Infra");
      RecordedRequest request = this.server.takeRequest();
      assertThat(request.getMethod()).isEqualTo("GET");
      assertThat(request.getHeader("Framework-Name")).isEqualTo("Infra");
    }

    @Test
    void useUserInfoToSetBasicAuth() throws Exception {
      startServer(true);
      UrlResource resource = new UrlResource(
              "http://alice:secret@localhost:" + this.server.getPort() + "/resource");
      assertThat(resource.getInputStream()).hasContent("Infra");
      RecordedRequest request = this.server.takeRequest();
      String authorization = request.getHeader("Authorization");
      assertThat(authorization).isNotNull().startsWith("Basic ");
      assertThat(new String(Base64.getDecoder().decode(authorization.substring(6)),
              StandardCharsets.ISO_8859_1)).isEqualTo("alice:secret");
    }

    @Test
    void createUrlResourceWithFileProtocol() throws Exception {
      UrlResource resource = new UrlResource("file", "/path/to/file.txt");
      assertThat(resource.getURL().toString()).isEqualTo("file:/path/to/file.txt");
    }

    @Test
    void createUrlResourceWithHttpProtocol() throws Exception {
      UrlResource resource = new UrlResource("http", "//example.com/path");
      assertThat(resource.getURL().toString()).isEqualTo("http://example.com/path");
    }

    @Test
    void createUrlResourceWithNullProtocolThrowsException() {
      assertThrows(IllegalArgumentException.class, () -> new UrlResource(null, "location"));
    }

    @Test
    void createUrlResourceWithNullLocationThrowsException() {
      assertThrows(MalformedURLException.class, () -> new UrlResource("file", null));
    }

    @Test
    void createUrlResourceWithInvalidProtocolThrowsMalformedURLException() {
      assertThrows(MalformedURLException.class, () -> new UrlResource("invalid:", "/path"));
    }

    @Test
    void createUrlResourceWithSpecialCharactersInLocation() throws Exception {
      UrlResource resource = new UrlResource("file", "/path with spaces/file#1.txt");
      assertThat(resource.getURL().toString()).isEqualTo("file:/path%20with%20spaces/file%231.txt");
    }

    @Test
    void createUrlResourceWithEmptyLocationThrowsMalformedURLException() {
      assertThrows(MalformedURLException.class, () -> new UrlResource("file", ""));
    }

    @Test
    void getURIReturnsUnderlying() throws Exception {
      URI uri = new URI("file:/path/to/file.txt");
      UrlResource resource = new UrlResource(uri);
      assertThat(resource.getURI()).isSameAs(uri);
    }

    @Test
    void getURICreatesFromURL() throws Exception {
      URL url = new URL("file:/path/to/file.txt");
      UrlResource resource = new UrlResource(url);
      assertThat(resource.getURI()).isEqualTo(new URI("file:/path/to/file.txt"));
    }

    @Test
    void getFileForHttpURLThrowsException() throws MalformedURLException {
      UrlResource resource = new UrlResource("http", "//example.com/file.txt");
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getFile);
    }

    @Test
    void hashCodeEqualityBasedOnCleanedUrl() throws Exception {
      UrlResource resource1 = new UrlResource("file:dir/../dir/file.txt");
      UrlResource resource2 = new UrlResource("file:dir/file.txt");
      assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode());
    }

    @Test
    void toStringIncludesUnderlyingUri() throws Exception {
      URI uri = new URI("file:/path/test.txt");
      UrlResource resource = new UrlResource(uri);
      assertThat(resource.toString()).isEqualTo("URL [file:/path/test.txt]");
    }

    @Test
    void factoryMethodWrapsExceptionInUnchecked() {
      assertThrows(UncheckedIOException.class, () -> UrlResource.from(":::invalid:::"));
    }

    @AfterEach
    void shutdown() throws Exception {
      this.server.shutdown();
    }

    private String startServer(boolean withHeadSupport) throws Exception {
      this.server.setDispatcher(new ResourceDispatcher(withHeadSupport));
      this.server.start();
      return "http://localhost:" + this.server.getPort();
    }

    class CustomResource extends UrlResource {

      public CustomResource(String path) throws MalformedURLException {
        super(path);
      }

      @Override
      protected void customizeConnection(HttpURLConnection con) {
        con.setRequestProperty("Framework-Name", "Infra");
      }
    }

    class ResourceDispatcher extends Dispatcher {

      boolean withHeadSupport;

      public ResourceDispatcher(boolean withHeadSupport) {
        this.withHeadSupport = withHeadSupport;
      }

      @Override
      public MockResponse dispatch(RecordedRequest request) {
        if (request.getPath().equals("/resource")) {
          return switch (request.getMethod()) {
            case "HEAD" -> (this.withHeadSupport ?
                    new MockResponse()
                            .addHeader("Content-Type", "text/plain")
                            .addHeader("Content-Length", "6")
                            .addHeader("Last-Modified", LAST_MODIFIED) :
                    new MockResponse().setResponseCode(405));
            case "GET" -> new MockResponse()
                    .addHeader("Content-Type", "text/plain")
                    .addHeader("Content-Length", "6")
                    .addHeader("Last-Modified", LAST_MODIFIED)
                    .setBody("Infra");
            default -> new MockResponse().setResponseCode(404);
          };
        }
        return new MockResponse().setResponseCode(404);
      }
    }
  }

  @Nested
  class AbstractResourceTests {

    @Test
    void missingResourceIsNotReadable() {
      final String name = "test-resource";

      Resource resource = new AbstractResource() {
        @Override
        public String toString() {
          return name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
          throw new FileNotFoundException();
        }
      };

      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getURL)
              .withMessageContaining(name);
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getFile)
              .withMessageContaining(name);
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
              resource.createRelative("/testing")).withMessageContaining(name);
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getContentAsByteArray);
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
              () -> resource.getContentAsString(StandardCharsets.US_ASCII));
      assertThat(resource.getName()).isNull();
    }

    @Test
    void hasContentLength() throws Exception {
      AbstractResource resource = new AbstractResource() {
        @Override
        public InputStream getInputStream() {
          return new ByteArrayInputStream(new byte[] { 'a', 'b', 'c' });
        }

        @Override
        public String toString() {
          return "";
        }
      };
      assertThat(resource.contentLength()).isEqualTo(3L);
    }
  }

}
