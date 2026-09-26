package main;
import constant.ApplicationConstant;
import org.junit.jupiter.api.Test;
import pojo.UserType;
import utils.AppReader;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


public class UserPreferenceTest {

    @Test
    void validateUserPreferences() throws Exception {

        UserType ouser = AppReader.readuser();

        assertEquals(ApplicationConstant.EXPECTED_THEME,ouser.getPreferences().getTheme());

        assertEquals(ApplicationConstant.EXPECTED_NOTIFICATIONS,ouser.getPreferences().isNotification());



    }
}
