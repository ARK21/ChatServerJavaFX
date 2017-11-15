package message;

import java.io.Serializable;

public class Message implements Serializable {


    private static final long serialVersionUID = 8918286458282047271L;

    private String text;
    public static final int EXIT = 0, MESSAGE = 1, WHOISONLINE = 2;
    private int type;

    public Message(String text, int type) {
        this.text = text;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return type;
    }

}
