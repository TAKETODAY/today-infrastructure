/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.loader.net.protocol.jar;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import infra.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Handler}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class HandlerTests {

  private final Handler handler = new Handler();

  @Test
  void indexOfSeparator() {
    String spec = "jar:nested:foo!bar!/some/entry#foo";
    assertThat(Handler.indexOfSeparator(spec, 0, spec.indexOf('#'))).isEqualTo(spec.lastIndexOf("!/"));
  }

  @Test
  void indexOfSeparatorWhenHasStartAndLimit() {
    String spec = "a!/jar:nested:foo!bar!/some/entry#foo!/b";
    int beginIndex = 3;
    int endIndex = spec.length() - 4;
    String substring = spec.substring(beginIndex, endIndex);
    assertThat(Handler.indexOfSeparator(spec, 0, spec.indexOf('#')))
            .isEqualTo(substring.lastIndexOf("!/") + beginIndex);
  }

  @Test
  void parseUrlWhenAbsoluteParses() throws MalformedURLException {
    URL url = createJarUrl("");
    String spec = "jar:file:example.jar!/entry.txt";
    this.handler.parseURL(url, spec, 4, spec.length());
    assertThat(url.toExternalForm()).isEqualTo(spec);
  }

  @Test
  void parseUrlWhenAbsoluteWithAnchorParses() throws MalformedURLException {
    URL url = createJarUrl("");
    String spec = "jar:file:example.jar!/entry.txt";
    this.handler.parseURL(url, spec + "#foo", 4, spec.length());
    assertThat(url.toExternalForm()).isEqualTo(spec + "#foo");
  }

  @Test
  void parseUrlWhenAbsoluteWithNoSeparatorThrowsException() throws MalformedURLException {
    URL url = createJarUrl("");
    String spec = "jar:file:example.jar!\\entry.txt";
    assertThatIllegalStateException().isThrownBy(() -> this.handler.parseURL(url, spec, 4, spec.length()))
            .withMessage("no !/ in spec");
  }

  @Test
  void parseUrlWhenAbsoluteWithMalformedInnerUrlThrowsException() throws MalformedURLException {
    URL url = createJarUrl("");
    String spec = "jar:example.jar!/entry.txt";
    assertThatIllegalStateException().isThrownBy(() -> this.handler.parseURL(url, spec, 4, spec.length()))
            .withMessage(
                    "invalid url: jar:example.jar!/entry.txt (java.net.MalformedURLException: no protocol: example.jar)");
  }

  @Test
  void parseUrlWhenRelativeWithLeadingSlashParses() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar!/entry.txt");
    String spec = "/other.txt";
    this.handler.parseURL(url, spec, 0, spec.length());
    assertThat(url.toExternalForm()).isEqualTo("jar:file:example.jar!/other.txt");
  }

  @Test
  void parseUrlWhenRelativeWithLeadingSlashAndAnchorParses() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar!/entry.txt");
    String spec = "/other.txt";
    this.handler.parseURL(url, spec + "#relative", 0, spec.length());
    assertThat(url.toExternalForm()).isEqualTo("jar:file:example.jar!/other.txt#relative");
  }

  @Test
  void parseUrlWhenRelativeWithLeadingSlashAndNoSeparator() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar/entry.txt");
    String spec = "/other.txt";
    assertThatIllegalStateException().isThrownBy(() -> this.handler.parseURL(url, spec, 0, spec.length()))
            .withMessage("malformed context url:jar:file:example.jar/entry.txt: no !/");
  }

  @Test
  void parseUrlWhenRelativeWithoutLeadingSlashParses() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar!/foo/");
    String spec = "bar.txt";
    this.handler.parseURL(url, spec, 0, spec.length());
    assertThat(url.toExternalForm()).isEqualTo("jar:file:example.jar!/foo/bar.txt");
  }

  @Test
  void parseUrlWhenRelativeWithoutLeadingSlashAndWithoutTrailingSlashParses() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar!/foo/baz");
    String spec = "bar.txt";
    this.handler.parseURL(url, spec, 0, spec.length());
    assertThat(url.toExternalForm()).isEqualTo("jar:file:example.jar!/foo/bar.txt");
  }

  @Test
  void parseUrlWhenRelativeWithoutLeadingSlashAndWithoutContextSlashThrowsException() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar");
    String spec = "bar.txt";
    assertThatIllegalStateException().isThrownBy(() -> this.handler.parseURL(url, spec, 0, spec.length()))
            .withMessage("malformed context url:jar:file:example.jar");
  }

  @Test
  void parseUrlWhenAnchorOnly() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar!/entry.txt");
    String spec = "#runtime";
    this.handler.parseURL(url, spec, 0, 0);
    assertThat(url.toExternalForm()).isEqualTo("jar:file:example.jar!/entry.txt#runtime");
  }

  @Test
    // gh-38524
  void parseUrlWhenSpecIsEmpty() throws MalformedURLException {
    URL url = createJarUrl("nested:gh-38524.jar/!APP-INF/classes/!/");
    String spec = "";
    this.handler.parseURL(url, spec, 0, 0);
    assertThat(url.toExternalForm()).isEqualTo("jar:nested:gh-38524.jar/!APP-INF/classes/!/");

  }

  @Test
  void hashCodeGeneratesHashCode() throws MalformedURLException {
    URL url = createJarUrl("file:example.jar!/entry.txt");
    assertThat(this.handler.hashCode(url)).isEqualTo(1873709601);
  }

  @Test
  void hashCodeWhenMalformedInnerUrlGeneratesHashCode() throws MalformedURLException {
    URL url = createJarUrl("example.jar!/entry.txt");
    assertThat(this.handler.hashCode(url)).isEqualTo(1870566566);
  }

  @Test
  void sameFileWhenSameReturnsTrue() throws MalformedURLException {
    URL url1 = createJarUrl("file:example.jar!/entry.txt");
    URL url2 = createJarUrl("file:example.jar!/entry.txt");
    assertThat(this.handler.sameFile(url1, url2)).isTrue();
  }

  @Test
  void sameFileWhenMissingSeparatorReturnsFalse() throws MalformedURLException {
    URL url1 = createJarUrl("file:example.jar!/entry.txt");
    URL url2 = createJarUrl("file:example.jar/entry.txt");
    assertThat(this.handler.sameFile(url1, url2)).isFalse();
  }

  @Test
  void sameFileWhenDifferentEntryReturnsFalse() throws MalformedURLException {
    URL url1 = createJarUrl("file:example.jar!/entry1.txt");
    URL url2 = createJarUrl("file:example.jar!/entry2.txt");
    assertThat(this.handler.sameFile(url1, url2)).isFalse();
  }

  @Test
  void sameFileWhenDifferentInnerUrlReturnsFalse() throws MalformedURLException {
    URL url1 = createJarUrl("file:example1.jar!/entry.txt");
    URL url2 = createJarUrl("file:example2.jar!/entry.txt");
    assertThat(this.handler.sameFile(url1, url2)).isFalse();
  }

  @Test
  void sameFileWhenSameMalformedInnerUrlReturnsTrue() throws MalformedURLException {
    URL url1 = createJarUrl("example.jar!/entry.txt");
    URL url2 = createJarUrl("example.jar!/entry.txt");
    assertThat(this.handler.sameFile(url1, url2)).isTrue();
  }

  @Test
  void sameFileWhenDifferentMalformedInnerUrlReturnsFalse() throws MalformedURLException {
    URL url1 = createJarUrl("example1.jar!/entry.txt");
    URL url2 = createJarUrl("example2.jar!/entry.txt");
    assertThat(this.handler.sameFile(url1, url2)).isFalse();
  }

  private URL createJarUrl(String file) throws MalformedURLException {
    return new URL("jar", null, -1, file, this.handler);
  }

}
