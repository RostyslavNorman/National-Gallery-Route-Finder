package controller;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import model.Room;

import java.util.List;
import java.util.Objects;

public class PermutationsViewer {

    public StackPane stackPane;
    public ImageView imageView;
    public Pane overlayPane;
    public Slider slider;

    private final Polyline pixelPath = new Polyline();
    private List<List<Room>> dfsPermutations;
    private List<Room> currentRooms;

    private static final double IMAGE_WIDTH  = 675;
    private static final double IMAGE_HEIGHT = 693;

    public void initialize() {
        setupImage();
        setupOverlay();
        setupPathMarker();
        setupResizeListener();
    }
    private void setupImage() {
        StackPane.setAlignment(imageView, Pos.CENTER);
        StackPane.setAlignment(overlayPane, Pos.CENTER);

        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(stackPane.widthProperty());
        imageView.fitHeightProperty().bind(stackPane.heightProperty());

        imageView.setImage(new Image(
                Objects.requireNonNull(
                        getClass().getResource("/Images/Floor2Layout.png")
                ).toExternalForm()
        ));
    }

    private void setupOverlay() {
        overlayPane.setMouseTransparent(true);
        overlayPane.setMinSize(0, 0);
        overlayPane.getChildren().add(pixelPath);
    }

    private void setupPathMarker() {
        pixelPath.setStroke(Color.BLUEVIOLET);
        pixelPath.setStrokeWidth(2);
        pixelPath.setStrokeLineCap(StrokeLineCap.ROUND);
        pixelPath.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    private void redrawPath() {
        pixelPath.getPoints().clear();

        if (currentRooms == null || imageView.getImage() == null) {
            return;
        }

        Bounds imageBounds = imageView.getBoundsInLocal();
        Bounds displayed = imageView.localToParent(imageBounds);

        double scaleX = displayed.getWidth() / IMAGE_WIDTH;
        double scaleY = displayed.getHeight() / IMAGE_HEIGHT;

        double offsetX = displayed.getMinX();
        double offsetY = displayed.getMinY();

        for (Room r : currentRooms) {
            pixelPath.getPoints().addAll(
                    r.getX() * scaleX + offsetX,
                    r.getY() * scaleY + offsetY
            );
        }
    }

    private void setupResizeListener() {
        imageView.boundsInParentProperty().addListener((obs, o, n) -> redrawPath());
    }

    private void setupSlider() {
        slider.setMin(0);
        slider.setMax(dfsPermutations.size() - 1);

        slider.valueProperty().addListener((obs, o, n) -> {
            int index = n.intValue();
            slider.setValue(index);
            currentRooms = dfsPermutations.get(index);
            redrawPath();
        });

        currentRooms = dfsPermutations.get(0);
        redrawPath();
    }

    public void setList(List<List<Room>> list) {
        this.dfsPermutations = list;
        setupSlider();
    }
}