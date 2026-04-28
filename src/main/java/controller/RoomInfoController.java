package controller;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Room;

import java.util.Objects;

public class RoomInfoController {
    public Label roomName;
    public ImageView image;
    public Label description;

    public void setRoom(Room room) {
        roomName.setText(room.getName());
        image.setImage(new Image(
                Objects.requireNonNull(
                        getClass().getResource("/Images/Floor2Layout.png")
                ).toExternalForm()
        ));
        description.setText("Something");
    }
}
