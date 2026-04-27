package controller;

import data.GalleryDataParser;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import javafx.scene.text.Font;
import javafx.stage.Popup;
import model.Room;

import java.io.IOException;
import java.util.*;

public class MainUIController {
    public AnchorPane rootAnchor;
    public MenuButton searchButton;
    public Button generatePathButton;
    public AnchorPane selectionList;
    public ScrollPane scrollPane;
    public VBox controlsVBOX;
    public AnchorPane scrollPaneAnchor;
    public Pane overlayPane;
    public ImageView imageView;
    public StackPane stackPane;
    public Group group;

    private LinkedList<Integer> selectedNodes = new LinkedList<>();
    private final GalleryDataParser galleryData = new GalleryDataParser();
    private DoubleBinding displayedImageWidth, displayedImageHeight, offsetX, offsetY, markerScale;
    private static final double BASE_IMAGE_WIDTH  = 675;
    private static final double BASE_IMAGE_HEIGHT = 693;


    public void initialize() {

        StackPane.setAlignment(imageView, Pos.CENTER);
        StackPane.setAlignment(overlayPane, Pos.CENTER);

        overlayPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        overlayPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        imageView.preserveRatioProperty().set(true);
        imageView.fitWidthProperty().bind(stackPane.widthProperty());
        imageView.fitHeightProperty().bind(stackPane.heightProperty());
        imageView.setImage(new Image(
                Objects.requireNonNull(
                        getClass().getResource("/Images/Floor2Layout.png")
                ).toExternalForm()
        ));

        displayedImageWidth = Bindings.createDoubleBinding(() -> {
            Image img = imageView.getImage();
            if (img == null) return 0.0;

            double iw = img.getWidth();
            double ih = img.getHeight();
            double vw = imageView.getBoundsInParent().getWidth();
            double vh = imageView.getBoundsInParent().getHeight();

            double scale = Math.min(vw / iw, vh / ih);
            return iw * scale;
        }, imageView.boundsInParentProperty(), imageView.imageProperty());

        displayedImageHeight = Bindings.createDoubleBinding(() -> {
            Image img = imageView.getImage();
            if (img == null) return 0.0;

            double iw = img.getWidth();
            double ih = img.getHeight();
            double vw = imageView.getBoundsInParent().getWidth();
            double vh = imageView.getBoundsInParent().getHeight();

            double scale = Math.min(vw / iw, vh / ih);
            return ih * scale;
        }, imageView.boundsInParentProperty(), imageView.imageProperty());

        offsetX = Bindings.createDoubleBinding(
                () -> (imageView.getBoundsInParent().getWidth() - displayedImageWidth.get()) / 2,
                displayedImageWidth, imageView.boundsInParentProperty()
        );
        offsetY = Bindings.createDoubleBinding(
                () -> (imageView.getBoundsInParent().getHeight() - displayedImageHeight.get()) / 2,
                displayedImageHeight, imageView.boundsInParentProperty()
        );
        markerScale = Bindings.createDoubleBinding(
                () -> displayedImageWidth.get() / BASE_IMAGE_WIDTH,
                displayedImageWidth
        );


        overlayPane.prefWidthProperty().bind(displayedImageWidth);
        overlayPane.prefHeightProperty().bind(displayedImageHeight);
        overlayPane.translateXProperty().bind(offsetX);
        overlayPane.translateYProperty().bind(offsetY);
        setupMarkers();

        overlayPane.setOnMouseClicked(e -> {
            Image img = imageView.getImage();
            if (img == null) return;

            double mouseX = e.getX();
            double mouseY = e.getY();

            // Ignore clicks outside the rendered image
            if (mouseX < offsetX.get() ||
                    mouseX > offsetX.get() + displayedImageWidth.get() ||
                    mouseY < offsetY.get() ||
                    mouseY > offsetY.get() + displayedImageHeight.get()) {
                return;
            }

            double imageX =
                    (mouseX - offsetX.get()) / displayedImageWidth.get() * img.getWidth();

            double imageY =
                    (mouseY - offsetY.get()) / displayedImageHeight.get() * img.getHeight();

            System.out.printf(
                    "imageX=%d imageY=%d%n",
                    Math.round(imageX),
                    Math.round(imageY)
            );
        });

    }

    private void setupMarkers() {
        double imageHeight = imageView.getImage().getHeight();
        double imageWidth = imageView.getImage().getWidth();
        for (Room room : galleryData.getRooms()) {
            double xRatio = room.getX() / imageWidth;
            double yRatio = room.getY() / imageHeight;
            Circle circle = new Circle(10);
            circle.radiusProperty().bind(
                    markerScale.multiply(10)
            );
            Label text = new Label(room.getId());
            text.setMouseTransparent(true);
            text.setAlignment(Pos.CENTER);
            text.setTextFill(Color.WHITE);
            text.fontProperty().bind(
                    Bindings.createObjectBinding(
                            () -> Font.font(12 * markerScale.get()),
                            markerScale
                    )
            );
            StackPane marker = new StackPane(circle, text);
            marker.setPickOnBounds(false);
            marker.translateXProperty().bind(
                    marker.widthProperty().divide(-2)
            );
            marker.translateYProperty().bind(
                    marker.heightProperty().divide(-2)
            );
            marker.layoutXProperty().bind(
                    displayedImageWidth.multiply(xRatio)
            );
            marker.layoutYProperty().bind(
                    displayedImageHeight.multiply(yRatio)
            );
            marker.setUserData(room);
            createAssociatedPopup(marker);
            overlayPane.getChildren().add(marker);
        }
    }

    private void createAssociatedPopup(StackPane marker) {
        Parent content;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/roomInfoPopoutContent.fxml"));
            content = loader.load();
            RoomInfoController popupController = loader.getController();
            popupController.setRoom((Room) marker.getUserData());
        } catch(Exception e){
            System.out.println(e);
            return;
        }

        Popup popup = new Popup();
        popup.getContent().add(content);
        popup.setAutoHide(true);

        marker.setOnMouseEntered(e -> {
            popup.show(
                    marker,
                    e.getScreenX() + 14,
                    e.getScreenY() + 14
            );
        });
        marker.setOnMouseMoved(e -> {
            if (popup.isShowing()) {
                popup.setX(e.getScreenX() + 14);
                popup.setY(e.getScreenY() + 14);
            }
        });
        marker.setOnMouseExited(e -> popup.hide());
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
}
