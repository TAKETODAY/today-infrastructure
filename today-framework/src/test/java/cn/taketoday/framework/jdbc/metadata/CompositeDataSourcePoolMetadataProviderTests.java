/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.jdbc.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CompositeDataSourcePoolMetadataProvider}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class CompositeDataSourcePoolMetadataProviderTests {

  @Mock
  private DataSourcePoolMetadataProvider firstProvider;

  @Mock
  private DataSourcePoolMetadata first;

  @Mock
  private DataSource firstDataSource;

  @Mock
  private DataSourcePoolMetadataProvider secondProvider;

  @Mock
  private DataSourcePoolMetadata second;

  @Mock
  private DataSource secondDataSource;

  @Mock
  private DataSource unknownDataSource;

  @BeforeEach
  void setup() {
    given(this.firstProvider.getDataSourcePoolMetadata(this.firstDataSource)).willReturn(this.first);
    given(this.firstProvider.getDataSourcePoolMetadata(this.secondDataSource)).willReturn(this.second);
  }

  @Test
  void createWithProviders() {
    CompositeDataSourcePoolMetadataProvider provider = new CompositeDataSourcePoolMetadataProvider(
            Arrays.asList(this.firstProvider, this.secondProvider));
    assertThat(provider.getDataSourcePoolMetadata(this.firstDataSource)).isSameAs(this.first);
    assertThat(provider.getDataSourcePoolMetadata(this.secondDataSource)).isSameAs(this.second);
    assertThat(provider.getDataSourcePoolMetadata(this.unknownDataSource)).isNull();
  }

}
