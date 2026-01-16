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

package infra.codec.multipart;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.core.ApplicationTemp;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class FileStorageTests {

  @Test
  void fromPath() throws IOException {
    Path path = ApplicationTemp.instance.createFile(null, "spring", "test");
    FileStorage storage = FileStorage.fromPath(path);

    Mono<Path> directory = storage.directory();
    StepVerifier.create(directory)
            .expectNext(path)
            .verifyComplete();
  }

  @Test
  void tempDirectory() {
    FileStorage storage = FileStorage.tempDirectory(Schedulers::boundedElastic);

    Mono<Path> directory = storage.directory();
    StepVerifier.create(directory)
            .consumeNextWith(path -> {
              assertThat(path).exists();
              StepVerifier.create(directory)
                      .expectNext(path)
                      .verifyComplete();
            })
            .verifyComplete();
  }

  @Test
  void tempDirectoryDeleted() {
    FileStorage storage = FileStorage.tempDirectory(Schedulers::boundedElastic);

    Mono<Path> directory = storage.directory();
    StepVerifier.create(directory)
            .consumeNextWith(path1 -> {
              try {
                Files.delete(path1);
                StepVerifier.create(directory)
                        .consumeNextWith(path2 -> Assertions.assertThat(path2).isNotEqualTo(path1))
                        .verifyComplete();
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            })
            .verifyComplete();
  }

}
