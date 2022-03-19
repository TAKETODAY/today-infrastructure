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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.context.NoSuchMessageException;
import cn.taketoday.context.testfixture.AbstractApplicationContextTests;
import cn.taketoday.context.testfixture.beans.ACATester;
import cn.taketoday.context.testfixture.beans.BeanThatListens;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class StaticMessageSourceTests extends AbstractApplicationContextTests {

  protected static final String MSG_TXT1_US =
          "At '{1,time}' on \"{1,date}\", there was \"{2}\" on planet {0,number,integer}.";
  protected static final String MSG_TXT1_UK =
          "At '{1,time}' on \"{1,date}\", there was \"{2}\" on station number {0,number,integer}.";
  protected static final String MSG_TXT2_US =
          "This is a test message in the message catalog with no args.";
  protected static final String MSG_TXT3_US =
          "This is another test message in the message catalog with no args.";

  protected StaticApplicationContext sac;

  @Test
  @Override
  public void count() {
    assertCount(15);
  }

  @Test
  @Override
  @Disabled("Do nothing here since super is looking for errorCodes we do NOT have in the Context")
  public void messageSource() throws NoSuchMessageException {
  }

  @Test
  public void getMessageWithDefaultPassedInAndFoundInMsgCatalog() {
    // Try with Locale.US
    assertThat(sac.getMessage("message.format.example2", null, "This is a default msg if not found in MessageSource.", Locale.US)
            .equals("This is a test message in the message catalog with no args.")).as("valid msg from staticMsgSource with default msg passed in returned msg from msg catalog for Locale.US")
            .isTrue();
  }

  @Test
  public void getMessageWithDefaultPassedInAndNotFoundInMsgCatalog() {
    // Try with Locale.US
    assertThat(sac.getMessage("bogus.message", null, "This is a default msg if not found in MessageSource.", Locale.US)
            .equals("This is a default msg if not found in MessageSource.")).as("bogus msg from staticMsgSource with default msg passed in returned default msg for Locale.US").isTrue();
  }

  /**
   * We really are testing the AbstractMessageSource class here.
   * The underlying implementation uses a hashMap to cache messageFormats
   * once a message has been asked for.  This test is an attempt to
   * make sure the cache is being used properly.
   *
   * @see cn.taketoday.context.support.AbstractMessageSource for more details.
   */
  @Test
  public void getMessageWithMessageAlreadyLookedFor() {
    Object[] arguments = {
            7, new Date(System.currentTimeMillis()),
            "a disturbance in the Force"
    };

    // The first time searching, we don't care about for this test
    // Try with Locale.US
    sac.getMessage("message.format.example1", arguments, Locale.US);

    // Now msg better be as expected
    assertThat(sac.getMessage("message.format.example1", arguments, Locale.US).
            contains("there was \"a disturbance in the Force\" on planet 7.")).as("2nd search within MsgFormat cache returned expected message for Locale.US").isTrue();

    Object[] newArguments = {
            8, new Date(System.currentTimeMillis()),
            "a disturbance in the Force"
    };

    // Now msg better be as expected even with different args
    assertThat(sac.getMessage("message.format.example1", newArguments, Locale.US).
            contains("there was \"a disturbance in the Force\" on planet 8.")).as("2nd search within MsgFormat cache with different args returned expected message for Locale.US").isTrue();
  }

  /**
   * Example taken from the javadocs for the java.text.MessageFormat class
   */
  @Test
  public void getMessageWithNoDefaultPassedInAndFoundInMsgCatalog() {
    Object[] arguments = {
            7, new Date(System.currentTimeMillis()),
            "a disturbance in the Force"
    };

		/*
		 Try with Locale.US
		 Since the msg has a time value in it, we will use String.indexOf(...)
		 to just look for a substring without the time.  This is because it is
		 possible that by the time we store a time variable in this method
		 and the time the ResourceBundleMessageSource resolves the msg the
		 minutes of the time might not be the same.
		 */
    assertThat(sac.getMessage("message.format.example1", arguments, Locale.US).
            contains("there was \"a disturbance in the Force\" on planet 7.")).as("msg from staticMsgSource for Locale.US substituting args for placeholders is as expected").isTrue();

    // Try with Locale.UK
    assertThat(sac.getMessage("message.format.example1", arguments, Locale.UK).
            contains("there was \"a disturbance in the Force\" on station number 7.")).as("msg from staticMsgSource for Locale.UK substituting args for placeholders is as expected").isTrue();

    // Try with Locale.US - Use a different test msg that requires no args
    assertThat(sac.getMessage("message.format.example2", null, Locale.US)
            .equals("This is a test message in the message catalog with no args.")).as("msg from staticMsgSource for Locale.US that requires no args is as expected").isTrue();
  }

  @Test
  public void getMessageWithNoDefaultPassedInAndNotFoundInMsgCatalog() {
    // Try with Locale.US
    assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
            sac.getMessage("bogus.message", null, Locale.US));
  }

  @Test
  public void messageSourceResolvable() {
    // first code valid
    String[] codes1 = new String[] { "message.format.example3", "message.format.example2" };
    MessageSourceResolvable resolvable1 = new DefaultMessageSourceResolvable(codes1, null, "default");
    assertThat(MSG_TXT3_US.equals(sac.getMessage(resolvable1, Locale.US))).as("correct message retrieved").isTrue();

    // only second code valid
    String[] codes2 = new String[] { "message.format.example99", "message.format.example2" };
    MessageSourceResolvable resolvable2 = new DefaultMessageSourceResolvable(codes2, null, "default");
    assertThat(MSG_TXT2_US.equals(sac.getMessage(resolvable2, Locale.US))).as("correct message retrieved").isTrue();

    // no code valid, but default given
    String[] codes3 = new String[] { "message.format.example99", "message.format.example98" };
    MessageSourceResolvable resolvable3 = new DefaultMessageSourceResolvable(codes3, null, "default");
    assertThat("default".equals(sac.getMessage(resolvable3, Locale.US))).as("correct message retrieved").isTrue();

    // no code valid, no default
    String[] codes4 = new String[] { "message.format.example99", "message.format.example98" };
    MessageSourceResolvable resolvable4 = new DefaultMessageSourceResolvable(codes4);

    assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
            sac.getMessage(resolvable4, Locale.US));
  }

  @Override
  protected ConfigurableApplicationContext createContext() throws Exception {
    StaticApplicationContext parent = new StaticApplicationContext();

    Map<String, String> m = new HashMap<>();
    m.put("name", "Roderick");
    parent.registerPrototype("rod", cn.taketoday.beans.testfixture.beans.TestBean.class, new PropertyValues(m));
    m.put("name", "Albert");
    parent.registerPrototype("father", cn.taketoday.beans.testfixture.beans.TestBean.class, new PropertyValues(m));

    parent.refresh();
    parent.addApplicationListener(parentListener);

    this.sac = new StaticApplicationContext(parent);

    sac.registerSingleton("beanThatListens", BeanThatListens.class, new PropertyValues());

    sac.registerSingleton("aca", ACATester.class, new PropertyValues());

    sac.registerPrototype("aca-prototype", ACATester.class, new PropertyValues());

    cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader reader =
            new cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader(sac.getBeanFactory());

    reader.loadBeanDefinitions(new ClassPathResource("testBeans.properties", getClass()));
    sac.refresh();
    sac.addApplicationListener(listener);

    StaticMessageSource messageSource = sac.getStaticMessageSource();
    Map<String, String> usMessages = new HashMap<>(3);
    usMessages.put("message.format.example1", MSG_TXT1_US);
    usMessages.put("message.format.example2", MSG_TXT2_US);
    usMessages.put("message.format.example3", MSG_TXT3_US);
    messageSource.addMessages(usMessages, Locale.US);
    messageSource.addMessage("message.format.example1", Locale.UK, MSG_TXT1_UK);

    return sac;
  }

  @Test
  public void nestedMessageSourceWithParamInChild() {
    StaticMessageSource source = new StaticMessageSource();
    StaticMessageSource parent = new StaticMessageSource();
    source.setParentMessageSource(parent);

    source.addMessage("param", Locale.ENGLISH, "value");
    parent.addMessage("with.param", Locale.ENGLISH, "put {0} here");

    MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
            new String[] { "with.param" }, new Object[] { new DefaultMessageSourceResolvable("param") });

    assertThat(source.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("put value here");
  }

  @Test
  public void nestedMessageSourceWithParamInParent() {
    StaticMessageSource source = new StaticMessageSource();
    StaticMessageSource parent = new StaticMessageSource();
    source.setParentMessageSource(parent);

    parent.addMessage("param", Locale.ENGLISH, "value");
    source.addMessage("with.param", Locale.ENGLISH, "put {0} here");

    MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
            new String[] { "with.param" }, new Object[] { new DefaultMessageSourceResolvable("param") });

    assertThat(source.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("put value here");
  }

}
