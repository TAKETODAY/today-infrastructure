/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.codec.multipart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    Path path = Files.createTempFile("spring", "test");
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
                        .consumeNextWith(path2 -> assertThat(path2).isNotEqualTo(path1))
                        .verifyComplete();
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            })
            .verifyComplete();
  }

}
