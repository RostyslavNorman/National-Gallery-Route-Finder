package ui;

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

import java.util.LinkedList;
import java.util.Objects;

public class MainController {
    public AnchorPane rootAnchor;
    public MenuButton searchButton;
    public Button generatePathButton;
    public AnchorPane selectionList;
    public ScrollPane scrollPane;
    public VBox controlsVBOX;
    public AnchorPane scrollPaneAnchor;
    public Pane imagePane;
    public ImageView imageView;

    private LinkedList<Integer> selectedNodes = new LinkedList<>();

    public void initialize(){
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());
        imageView.setImage(new Image(Objects.requireNonNull(getClass().getResource("/Images/Floor2Layout.png")).toExternalForm()));
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
    }
}
