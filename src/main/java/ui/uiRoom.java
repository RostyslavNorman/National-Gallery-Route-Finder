package ui;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Shape;
import model.Room;

public class uiRoom {
    private final Room room;
    private final StackPane marker;
    private double[] coorindateRatios = new double[2];

    public uiRoom(Room room, StackPane marker, double[] coordinates) {
        this.room = room;
        this.marker = marker;
        this.coorindateRatios[0] = coordinates[0];
        this.coorindateRatios[1] = coordinates[1];
        marker.setUserData(room);
        marker.setOnMouseClicked(event -> System.out.println(room.getId()));
    }

    public Room getRoom() {
        return room;
    }

    public StackPane getMarker() {
        return marker;
    }

    public double[] getCoorindates() {
        return coorindateRatios;
    }
}
