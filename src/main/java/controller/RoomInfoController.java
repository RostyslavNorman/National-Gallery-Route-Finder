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
//        System.out.println("/Images/paintings/" + room.getPaintings().get(0).getImageFilename());
        String imagePath = "";
        roomName.setText(room.getName());
        if(room.getPaintings().isEmpty()) {
            imagePath = "/Images/No_Image_Available.jpg";
        }
        else{
            imagePath = "/Images/paintings/" + room.getPaintings().get(0).getImageFilename();
        }
        image.setImage(new Image(
                Objects.requireNonNull(
                        getClass().getResource(imagePath) //only supporting one at the moment
                ).toExternalForm()
        ));
        description.setText(room.getPaintings().get(0).getDescription());
    }
}
