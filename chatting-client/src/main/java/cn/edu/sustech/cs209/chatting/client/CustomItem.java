package cn.edu.sustech.cs209.chatting.client;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class CustomItem extends HBox {
    private CheckBox checkBox;
    private Label label;
    private boolean online;


    public CustomItem(String labelText) {
        checkBox = new CheckBox();
        checkBox.setVisible(false);
        label = new Label(labelText);
        getChildren().addAll(checkBox, label);
    }
    public void setOnline(boolean online) {
        this.online = online;
        if (online) {
            label.setStyle("-fx-text-fill: green;");
        } else {
            label.setStyle("-fx-text-fill: red;");
        }
    }

    public boolean isSelected() {
        return checkBox.isSelected();
    }
    public void setCheckBoxVisible(boolean tq) {
        checkBox.setVisible(tq);
    }

    public void setSelected(boolean selected) {
        checkBox.setSelected(selected);
    }

    public String getText() {
        return label.getText();
    }
}
