package main;

import constant.ApplicationConstant;
import org.testng.annotations.Test;
import pojo.UserType;
import utils.AppReader;

import static org.testng.Assert.assertEquals;

public class UserPreferenceTest {

    @Test
    public void validateUserPreferences() throws Exception {

        UserType ouser = AppReader.readuser();

        assertEquals(ouser.getPreferences().getTheme(), ApplicationConstant.EXPECTED_THEME);

        assertEquals(ouser.getPreferences().isNotification(), ApplicationConstant.EXPECTED_NOTIFICATIONS);

    }
}
