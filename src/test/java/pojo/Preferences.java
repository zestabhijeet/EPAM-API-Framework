package pojo;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Preferences {

    @JsonAlias({"notifications", "notification"})
    private boolean notification;
    private String  theme;

    public Preferences() {
    }

    public Preferences(boolean notification, String theme) {
        this.notification = notification;
        this.theme = theme;
    }

    public boolean isNotification() {
        return notification;
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
    @Override
    public String toString() {
        return "Preferences{" +
                "notification=" + notification +
                ", theme='" + theme + '\'' +
                '}';
    }


}
