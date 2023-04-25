package cn.edu.sustech.cs209.chatting.server;

import java.util.Objects;

public class ConversationKey {
    private String user1;
    private String user2;

    public ConversationKey(String user1, String user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    public String getUser1() {
        return user1;
    }

    public String getUser2() {
        return user2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationKey that = (ConversationKey) o;
        return (Objects.equals(user1, that.user1) && Objects.equals(user2, that.user2)) ||
                (Objects.equals(user1, that.user2) && Objects.equals(user2, that.user1));
    }

    @Override
    public int hashCode() {
        return Objects.hash(user1) + Objects.hash(user2);
    }
}
