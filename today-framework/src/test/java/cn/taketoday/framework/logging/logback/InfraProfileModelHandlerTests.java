/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InfraProfileModelHandler}.
 *
 * @author Andy Wilkinson
 */
class InfraProfileModelHandlerTests {

	private final Environment environment = mock(Environment.class);

	private final Context context = new ContextBase();

	private final InfraProfileModelHandler action = new InfraProfileModelHandler(this.context, this.environment);

	private final ModelInterpretationContext interpretationContext = new ModelInterpretationContext(this.context);

	@BeforeEach
	void setUp() {
		this.action.setContext(this.context);
	}

	@Test
	void environmentIsQueriedWithProfileFromModelName() throws ModelHandlerException {
		InfraProfileModel model = new InfraProfileModel();
		model.setName("dev");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev");
	}

	@Test
	void environmentIsQueriedWithMultipleProfilesFromCommaSeparatedModelName() throws ModelHandlerException {
		InfraProfileModel model = new InfraProfileModel();
		model.setName("dev,qa");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev", "qa");
	}

	@Test
	void environmentIsQueriedWithResolvedValueWhenModelNameUsesAPlaceholder() throws ModelHandlerException {
		InfraProfileModel model = new InfraProfileModel();
		model.setName("${profile}");
		this.context.putProperty("profile", "dev");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev");
	}

	@Test
	void environmentIsQueriedWithResolvedValuesFromCommaSeparatedNameNameAttributeWithPlaceholders()
			throws ModelHandlerException {
		InfraProfileModel model = new InfraProfileModel();
		model.setName("${profile1},${profile2}");
		this.context.putProperty("profile1", "dev");
		this.context.putProperty("profile2", "qa");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev", "qa");
	}

}
