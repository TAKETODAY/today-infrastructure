/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.function.Consumer;

import cn.taketoday.buildpack.platform.docker.LogUpdateEvent;
import cn.taketoday.buildpack.platform.docker.TotalProgressEvent;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrintStreamBuildLog}.
 *
 * @author Phillip Webb
 * @author Rafael Ceccone
 */
class PrintStreamBuildLogTests {

  @Test
  void printsExpectedOutput() throws Exception {
    TestPrintStream out = new TestPrintStream();
    PrintStreamBuildLog log = new PrintStreamBuildLog(out);
    BuildRequest request = mock(BuildRequest.class);
    ImageReference name = ImageReference.of("my-app:latest");
    ImageReference builderImageReference = ImageReference.of("cnb/builder");
    Image builderImage = mock(Image.class);
    given(builderImage.getDigests()).willReturn(Collections.singletonList("00000001"));
    ImageReference runImageReference = ImageReference.of("cnb/runner");
    Image runImage = mock(Image.class);
    given(runImage.getDigests()).willReturn(Collections.singletonList("00000002"));
    given(request.getName()).willReturn(name);
    ImageReference tag = ImageReference.of("my-app:1.0");
    given(request.getTags()).willReturn(Collections.singletonList(tag));
    log.start(request);
    Consumer<TotalProgressEvent> pullBuildImageConsumer = log.pullingImage(builderImageReference,
            ImageType.BUILDER);
    pullBuildImageConsumer.accept(new TotalProgressEvent(100));
    log.pulledImage(builderImage, ImageType.BUILDER);
    Consumer<TotalProgressEvent> pullRunImageConsumer = log.pullingImage(runImageReference, ImageType.RUNNER);
    pullRunImageConsumer.accept(new TotalProgressEvent(100));
    log.pulledImage(runImage, ImageType.RUNNER);
    log.executingLifecycle(request, LifecycleVersion.parse("0.5"), Cache.volume(VolumeName.of("pack-abc.cache")));
    Consumer<LogUpdateEvent> phase1Consumer = log.runningPhase(request, "alphabet");
    phase1Consumer.accept(mockLogEvent("one"));
    phase1Consumer.accept(mockLogEvent("two"));
    phase1Consumer.accept(mockLogEvent("three"));
    Consumer<LogUpdateEvent> phase2Consumer = log.runningPhase(request, "basket");
    phase2Consumer.accept(mockLogEvent("spring"));
    phase2Consumer.accept(mockLogEvent("boot"));
    log.executedLifecycle(request);
    log.taggedImage(tag);
    String expected = FileCopyUtils.copyToString(new InputStreamReader(
            getClass().getResourceAsStream("print-stream-build-log.txt"), StandardCharsets.UTF_8));
    assertThat(out.toString()).isEqualToIgnoringNewLines(expected);
  }

  private LogUpdateEvent mockLogEvent(String string) {
    LogUpdateEvent event = mock(LogUpdateEvent.class);
    given(event.toString()).willReturn(string);
    return event;
  }

  static class TestPrintStream extends PrintStream {

    TestPrintStream() {
      super(new ByteArrayOutputStream());
    }

    @Override
    public String toString() {
      return this.out.toString();
    }

  }

}
