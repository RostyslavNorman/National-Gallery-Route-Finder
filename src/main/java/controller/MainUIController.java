package controller;

import data.GalleryDataParser;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Shape;

import javafx.scene.shape.Rectangle;

import java.util.*;

public class MainUIController {
    public AnchorPane rootAnchor;
    public MenuButton searchButton;
    public Button generatePathButton;
    public AnchorPane selectionList;
    public ScrollPane scrollPane;
    public VBox controlsVBOX;
    public AnchorPane scrollPaneAnchor;
    public Pane imagePane;
    public ImageView imageView;

    private ArrayList<Shape> interactables = new ArrayList<>();
    private LinkedList<Integer> selectedNodes = new LinkedList<>();
    private final GalleryDataParser galleryData = new GalleryDataParser();;

    public void initialize(){
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());
        imagePane.widthProperty().addListener((obs, oldW, newW) -> {
            redrawInteractables(imagePane.getHeight());
        });
        imageView.setImage(new Image(Objects.requireNonNull(getClass().getResource("/Images/Floor2Layout.png")).toExternalForm()));
        Rectangle rect = new Rectangle(10, 10);
        interactables.add(rect);
        redrawInteractables(imageView.fitHeightProperty().doubleValue());
    }

    private void checkCanGeneratePath(){
        if(!searchButton.getText().equals("Search"))
            generatePathButton.setDisable(false);
        if(selectedNodes.isEmpty())
            generatePathButton.setDisable(true);
    }

    public void generatePath(ActionEvent actionEvent) {

    }

    public void selectSearch(ActionEvent actionEvent) {
        MenuItem item = (MenuItem) actionEvent.getSource();
        searchButton.setText(item.getText());
        checkCanGeneratePath();
        generatePathButton.setDisable(false);
    }

    private void redrawInteractables(double sideLength) {
        imagePane.getChildren().removeAll(interactables);
        for(Shape shape : interactables){
            shape.setLayoutX(sideLength/2);
            shape.setLayoutY(sideLength/2);
        }
        imagePane.getChildren().addAll(interactables);
    }
}
