package cn.edu.sustech.cs209.chatting.common;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;
    @Serial
    private static final long serialVersionUID = 1L;

    public Message(Long timestamp, String sentBy, String sendTo, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

}
