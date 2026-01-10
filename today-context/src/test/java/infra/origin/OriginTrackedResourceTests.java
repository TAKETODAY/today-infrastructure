/*
 * Copyright 2012-present the original author or authors.
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

package infra.origin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.context.testfixture.origin.MockOrigin;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.WritableResource;
import infra.origin.OriginTrackedResource.OriginTrackedWritableResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginTrackedResource}.
 *
 * @author Phillip Webb
 */
class OriginTrackedResourceTests {

  private Origin origin;

  private WritableResource resource;

  private OriginTrackedWritableResource tracked;

  @BeforeEach
  void setup() {
    this.origin = MockOrigin.of("test");
    this.resource = mock(WritableResource.class);
    this.tracked = OriginTrackedResource.from(this.resource, this.origin);
  }

  @Test
  void getInputStreamDelegatesToResource() throws IOException {
    this.tracked.getInputStream();
    then(this.resource).should().getInputStream();
  }

  @Test
  void existsDelegatesToResource() {
    this.tracked.exists();
    then(this.resource).should().exists();
  }

  @Test
  void isReadableDelegatesToResource() {
    this.tracked.isReadable();
    then(this.resource).should().isReadable();
  }

  @Test
  void isOpenDelegatesToResource() {
    this.tracked.isOpen();
    then(this.resource).should().isOpen();
  }

  @Test
  void isFileDelegatesToResource() {
    this.tracked.isFile();
    then(this.resource).should().isFile();
  }

  @Test
  void getURLDelegatesToResource() throws IOException {
    this.tracked.getURL();
    then(this.resource).should().getURL();
  }

  @Test
  void getURIDelegatesToResource() throws IOException {
    this.tracked.getURI();
    then(this.resource).should().getURI();
  }

  @Test
  void getFileDelegatesToResource() throws IOException {
    this.tracked.getFile();
    then(this.resource).should().getFile();
  }

  @Test
  void readableChannelDelegatesToResource() throws IOException {
    this.tracked.readableChannel();
    then(this.resource).should().readableChannel();
  }

  @Test
  void contentLengthDelegatesToResource() throws IOException {
    this.tracked.contentLength();
    then(this.resource).should().contentLength();
  }

  @Test
  void lastModifiedDelegatesToResource() throws IOException {
    this.tracked.lastModified();
    then(this.resource).should().lastModified();
  }

  @Test
  void createRelativeDelegatesToResource() throws IOException {
    this.tracked.createRelative("path");
    then(this.resource).should().createRelative("path");
  }

  @Test
  void getFilenameDelegatesToResource() {
    this.tracked.getName();
    then(this.resource).should().getName();
  }

  @Test
  void getOutputStreamDelegatesToResource() throws IOException {
    this.tracked.getOutputStream();
    then(this.resource).should().getOutputStream();
  }

  @Test
  void toStringDelegatesToResource() {
    Resource resource = new ClassPathResource("test");
    Resource tracked = OriginTrackedResource.from(resource, this.origin);
    assertThat(tracked).hasToString(resource.toString());
  }

  @Test
  void getOriginReturnsOrigin() {
    assertThat(this.tracked.getOrigin()).isEqualTo(this.origin);
  }

  @Test
  void getResourceReturnsResource() {
    assertThat(this.tracked.getDelegate()).isEqualTo(this.resource);
  }

  @Test
  void equalsAndHashCode() {
    Origin o1 = MockOrigin.of("o1");
    Origin o2 = MockOrigin.of("o2");
    Resource r1 = mock(Resource.class);
    Resource r2 = mock(Resource.class);
    OriginTrackedResource r1o1a = OriginTrackedResource.from(r1, o1);
    OriginTrackedResource r1o1b = OriginTrackedResource.from(r1, o1);
    OriginTrackedResource r1o2 = OriginTrackedResource.from(r1, o2);
    OriginTrackedResource r2o1 = OriginTrackedResource.from(r2, o1);
    OriginTrackedResource r2o2 = OriginTrackedResource.from(r2, o2);
    assertThat(r1o1a).isEqualTo(r1o1a).isEqualTo(r1o1a).isNotEqualTo(r1o2).isNotEqualTo(r2o1).isNotEqualTo(r2o2);
    assertThat(r1o1a.hashCode()).isEqualTo(r1o1b.hashCode());
  }

  @Test
  void ofReturnsOriginTrackedResource() {
    Resource resource = mock(Resource.class);
    Resource tracked = OriginTrackedResource.from(resource, this.origin);
    assertThat(tracked).isExactlyInstanceOf(OriginTrackedResource.class);
  }

  @Test
  void ofWhenWritableReturnsOriginTrackedWritableResource() {
    Resource resource = mock(WritableResource.class);
    Resource tracked = OriginTrackedResource.from(resource, this.origin);
    assertThat(tracked).isInstanceOf(WritableResource.class)
            .isExactlyInstanceOf(OriginTrackedWritableResource.class);
  }

}
