package controller;

import data.GalleryDataParser;
import data.MapColours;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import javafx.scene.text.Font;
import javafx.stage.Popup;
import model.Room;

import java.util.*;

public class MainUIController {
    private static final double BASE_IMAGE_WIDTH  = 675;
    private static final double BASE_IMAGE_HEIGHT = 693;
    public AnchorPane rootAnchor;
    public MenuButton searchButton;
    public Button generatePathButton;
    public VBox controlsVBOX;
    public AnchorPane scrollPaneAnchor;
    public Pane overlayPane;
    public ImageView imageView;
    public StackPane stackPane;

    private final GalleryDataParser galleryData = new GalleryDataParser();
    private DoubleBinding displayedImageWidth, displayedImageHeight, offsetX, offsetY, markerScale;
    private final ObservableList<Room> whitelist = FXCollections.observableArrayList();
    private final ObservableList<Room> blacklist = FXCollections.observableArrayList();
    public ListView<Room> whitelistView = new ListView<>(whitelist);;
    public ListView<Room> blacklistView  = new ListView<>(blacklist);;

    public void initialize() {
        setupUserLists();
        setupImageViewer();
        setupMarkers();
        setupMapPixelPrintout();
    }

    private void setupUserLists(){
        whitelistView.setItems(whitelist);
        blacklistView.setItems(blacklist);

        whitelistView.setCellFactory(lv -> {
            ListCell<Room> cell = new ListCell<>() {
                @Override
                protected void updateItem(Room item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.getName());
                }
            };

            cell.setOnDragDetected(e -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(cell.getItem().getId());
                    db.setContent(cc);
                    e.consume();
                }
            });

            cell.setOnDragOver(e -> {
                if (e.getGestureSource() != cell && e.getDragboard().hasString()) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
            });

            cell.setOnMouseClicked(e -> {
                if(e.getButton() == MouseButton.SECONDARY) {
                    whitelist.remove(whitelistView.getSelectionModel().getSelectedItem());
                    checkCanGeneratePath();
                }
            });

            cell.setOnDragDropped(e -> {
                if (!cell.isEmpty()) {
                    int draggedIdx = whitelist.indexOf(
                            whitelistView.getSelectionModel().getSelectedItem()
                    );
                    int thisIdx = cell.getIndex();

                    Room temp = whitelist.remove(draggedIdx);
                    whitelist.add(thisIdx, temp);
                    e.setDropCompleted(true);
                }
            });
            return cell;
        });

        blacklistView.setCellFactory(lv -> {
            ListCell<Room> cell = new ListCell<>() {
                @Override
                protected void updateItem(Room item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.getName());
                }
            };

            cell.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    blacklist.remove(blacklistView.getSelectionModel().getSelectedItem());
                }
            });

            return cell;
        });
    }

    private void setupImageViewer(){
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
    }

    private void setupMapPixelPrintout(){
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
            StackPane marker = createMarker(room.getId());
            setMarkerProperties(marker, xRatio, yRatio);
            marker.setUserData(room);
            createAssociatedPopup(marker);
            overlayPane.getChildren().add(marker);
        }
    }

    private StackPane createMarker(String roomId){
        Circle circle = new Circle(10);
        circle.radiusProperty().bind(
                markerScale.multiply(10)
        );
        circle.setFill(findColor(roomId));
        Label text = new Label(roomId);
        text.setMouseTransparent(true);
        text.setAlignment(Pos.CENTER);
        text.setTextFill(Color.WHITE);
        text.fontProperty().bind(
                Bindings.createObjectBinding(
                        () -> Font.font(12 * markerScale.get()),
                        markerScale
                )
        );
        return new StackPane(circle, text);
    }

    private Color findColor(String roomId) {
        int id = 0;
        if(!roomId.matches("\\d+")){
            StringBuilder newId = new StringBuilder();
            for(int i = 0; i < roomId.length(); i++){
                if(Character.isDigit(roomId.charAt(i))){
                    newId.append(roomId.charAt(i));
                }else{
                    break;
                }
            }
            id = Integer.parseInt(newId.toString());
        }else{
            id = Integer.parseInt(roomId);
        }
        if(id == 1)
            return MapColours.yellow;
        if(inRangeInclusive(id, 2, 14))
            return MapColours.pink;
        if(inRangeInclusive(id, 15, 32))
            return MapColours.purple;
        if(inRangeInclusive(id, 33, 37))
            return MapColours.blue;
        if(inRangeInclusive(id, 38, 46))
            return MapColours.green;
        return MapColours.orange;
    }

    private boolean inRangeInclusive(int num ,int min, int max){
        return num >= min && num <= max;
    }

    private void setMarkerProperties(StackPane marker, double xRatio, double yRatio){
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
        marker.setOnMouseClicked(e -> {
            Room waypoint = (Room) marker.getUserData();
            if (e.getButton() == MouseButton.PRIMARY) {
                if (!whitelist.contains(waypoint)) {
                    whitelist.add(waypoint);
                    blacklist.remove(waypoint);
                }
            } else if (e.getButton() == MouseButton.SECONDARY) {
                if (!blacklist.contains(waypoint) && !searchButton.getText().equals("BFS")) {
                    blacklist.add(waypoint);
                    whitelist.remove(waypoint);
                }
            }
            checkCanGeneratePath();
            e.consume();
        });
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
        generatePathButton.setDisable(false);
        if(searchButton.getText().equals("Search Type") || whitelist.size() < 2)
            generatePathButton.setDisable(true);
    }

    public void generatePath(ActionEvent actionEvent) {

    }

    public void selectSearch(ActionEvent actionEvent) {
        MenuItem item = (MenuItem) actionEvent.getSource();
        if(item.getText().equals("BFS"))
            imageView.setImage(new Image(
                    Objects.requireNonNull(
                            getClass().getResource("/Images/Floor2_filled_walls_structural_final.png")
                    ).toExternalForm()
            ));
        else
            imageView.setImage(new Image(
                    Objects.requireNonNull(
                            getClass().getResource("/Images/Floor2Layout.png")
                    ).toExternalForm()
            ));
        searchButton.setText(item.getText());
        checkCanGeneratePath();
    }
}
