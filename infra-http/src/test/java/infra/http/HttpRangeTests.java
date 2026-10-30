/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.core.io.ResourceRegion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author TODAY 2021/11/6 23:49
 */
class HttpRangeTests {

  private static final long LAST_MODIFIED = 1660000000000L;

  @Test
  void resolveRangesWithoutIfRange() {
    assertThat(HttpRange.resolveRanges(rangeRequestHeaders(), HttpHeaders.forWritable())).hasSize(1);
  }

  @Test
  void resolveRangesWithMatchingIfRangeETag() {
    assertThat(resolveWithIfRange("\"current\"", "\"current\"")).hasSize(1);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
          "stale"       | "current"
          W/"current"   | W/"current"
          "current"     | W/"current"
          """)
  void resolveRangesWithNonMatchingIfRangeETag(String ifRange, String eTag) {
    assertThat(resolveWithIfRange(ifRange, eTag)).isEmpty();
  }

  @Test
  void resolveRangesWithMatchingIfRangeDate() {
    HttpHeaders requestHeaders = rangeRequestHeaders();
    requestHeaders.setDate(HttpHeaders.IF_RANGE, LAST_MODIFIED);
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setLastModified(LAST_MODIFIED);
    assertThat(HttpRange.resolveRanges(requestHeaders, responseHeaders)).hasSize(1);
  }

  @Test
  void resolveRangesWithNonMatchingIfRangeDate() {
    HttpHeaders requestHeaders = rangeRequestHeaders();
    requestHeaders.setDate(HttpHeaders.IF_RANGE, LAST_MODIFIED - 1000);
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setLastModified(LAST_MODIFIED);
    assertThat(HttpRange.resolveRanges(requestHeaders, responseHeaders)).isEmpty();
  }

  @Test
  void resolveRangesIgnoresInvalidRangeWhenIfRangeDoesNotMatch() {
    HttpHeaders requestHeaders = HttpHeaders.forWritable();
    requestHeaders.set(HttpHeaders.RANGE, "invalid");
    requestHeaders.set(HttpHeaders.IF_RANGE, "\"stale\"");
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setETag("\"current\"");
    assertThat(HttpRange.resolveRanges(requestHeaders, responseHeaders)).isEmpty();
  }

  @Test
  void resolveRangesWithMultipleIfRangeValuesIgnoresRange() {
    HttpHeaders requestHeaders = rangeRequestHeaders();
    requestHeaders.set(HttpHeaders.IF_RANGE, List.of("\"current\"", "\"other\""));
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setETag("\"current\"");
    assertThat(HttpRange.resolveRanges(requestHeaders, responseHeaders)).isEmpty();
  }

  private static HttpHeaders rangeRequestHeaders() {
    HttpHeaders requestHeaders = HttpHeaders.forWritable();
    requestHeaders.setRange(List.of(HttpRange.createByteRange(0, 1)));
    return requestHeaders;
  }

  private static List<HttpRange> resolveWithIfRange(String ifRange, String eTag) {
    HttpHeaders requestHeaders = rangeRequestHeaders();
    requestHeaders.set(HttpHeaders.IF_RANGE, ifRange);
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setETag(eTag);
    return HttpRange.resolveRanges(requestHeaders, responseHeaders);
  }

  @Test
  public void invalidFirstPosition() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.createByteRange(-1));
  }

  @Test
  public void invalidLastLessThanFirst() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.createByteRange(10, 9));
  }

  @Test
  public void invalidSuffixLength() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.createSuffixRange(-1));
  }

  @Test
  public void byteRange() {
    HttpRange range = HttpRange.createByteRange(0, 499);
    assertThat(range.getRangeStart(1000)).isEqualTo(0);
    assertThat(range.getRangeEnd(1000)).isEqualTo(499);
  }

  @Test
  public void byteRangeWithoutLastPosition() {
    HttpRange range = HttpRange.createByteRange(9500);
    assertThat(range.getRangeStart(10000)).isEqualTo(9500);
    assertThat(range.getRangeEnd(10000)).isEqualTo(9999);
  }

  @Test
  public void byteRangeOfZeroLength() {
    HttpRange range = HttpRange.createByteRange(9500, 9500);
    assertThat(range.getRangeStart(10000)).isEqualTo(9500);
    assertThat(range.getRangeEnd(10000)).isEqualTo(9500);
  }

  @Test
  public void suffixRange() {
    HttpRange range = HttpRange.createSuffixRange(500);
    assertThat(range.getRangeStart(1000)).isEqualTo(500);
    assertThat(range.getRangeEnd(1000)).isEqualTo(999);
  }

  @Test
  public void suffixRangeShorterThanRepresentation() {
    HttpRange range = HttpRange.createSuffixRange(500);
    assertThat(range.getRangeStart(350)).isEqualTo(0);
    assertThat(range.getRangeEnd(350)).isEqualTo(349);
  }

  @Test
  public void parseRanges() {
    List<HttpRange> ranges = HttpRange.parseRanges("bytes=0-0,500-,-1");
    assertThat(ranges.size()).isEqualTo(3);
    assertThat(ranges.get(0).getRangeStart(1000)).isEqualTo(0);
    assertThat(ranges.get(0).getRangeEnd(1000)).isEqualTo(0);
    assertThat(ranges.get(1).getRangeStart(1000)).isEqualTo(500);
    assertThat(ranges.get(1).getRangeEnd(1000)).isEqualTo(999);
    assertThat(ranges.get(2).getRangeStart(1000)).isEqualTo(999);
    assertThat(ranges.get(2).getRangeEnd(1000)).isEqualTo(999);
  }

  @Test
  public void parseRangesValidations() {

    // 1. At limit..
    StringBuilder atLimit = new StringBuilder("bytes=0-0");
    for (int i = 0; i < 99; i++) {
      atLimit.append(',').append(i).append('-').append(i + 1);
    }
    List<HttpRange> ranges = HttpRange.parseRanges(atLimit.toString());
    assertThat(ranges.size()).isEqualTo(100);

    // 2. Above limit..
    StringBuilder aboveLimit = new StringBuilder("bytes=0-0");
    for (int i = 0; i < 100; i++) {
      aboveLimit.append(',').append(i).append('-').append(i + 1);
    }
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.parseRanges(aboveLimit.toString()));
  }

  @Test
  public void rangeToString() {
    List<HttpRange> ranges = new ArrayList<>();
    ranges.add(HttpRange.createByteRange(0, 499));
    ranges.add(HttpRange.createByteRange(9500));
    ranges.add(HttpRange.createSuffixRange(500));
    assertThat(HttpRange.toString(ranges)).as("Invalid Range header").isEqualTo("bytes=0-499, 9500-, -500");
  }

  @Test
  public void toResourceRegion() {
    byte[] bytes = "Infra Framework".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);
    HttpRange range = HttpRange.createByteRange(0, 5);
    ResourceRegion region = range.toResourceRegion(resource);
    assertThat(region.getResource()).isEqualTo(resource);
    assertThat(region.getPosition()).isEqualTo(0L);
    assertThat(region.getCount()).isEqualTo(6L);
  }

  @Test
  public void toResourceRegionInputStreamResource() {
    InputStreamResource resource = mock(InputStreamResource.class);
    HttpRange range = HttpRange.createByteRange(0, 9);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test
  public void toResourceRegionIllegalLength() {
    ByteArrayResource resource = mock(ByteArrayResource.class);
    given(resource.contentLength()).willReturn(-1L);
    HttpRange range = HttpRange.createByteRange(0, 9);
    assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test
  public void toResourceRegionExceptionLength() throws IOException {
    InputStreamResource resource = mock(InputStreamResource.class);
    given(resource.contentLength()).willThrow(IOException.class);
    HttpRange range = HttpRange.createByteRange(0, 9);
    assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test // gh-23576
  public void toResourceRegionStartingAtResourceByteCount() {
    byte[] bytes = "Infra Framework".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);
    HttpRange range = HttpRange.createByteRange(resource.contentLength());
    assertThatIllegalArgumentException().isThrownBy(() -> range.toResourceRegion(resource));
  }

  @Test
  public void toResourceRegionsValidations() {
    byte[] bytes = "12345".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);

    // 1. Below length
    List<HttpRange> belowLengthRanges = HttpRange.parseRanges("bytes=0-1,2-3");
    List<ResourceRegion> regions = HttpRange.toResourceRegions(belowLengthRanges, resource);
    assertThat(regions.size()).isEqualTo(2);

    // 2. At length
    List<HttpRange> atLengthRanges = HttpRange.parseRanges("bytes=0-1,2-4");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpRange.toResourceRegions(atLengthRanges, resource));
  }

}
