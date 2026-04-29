package algorithms;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import model.PixelCoordinate;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import java.util.ArrayList;
import java.util.LinkedList;

public class BFS {
    private static final double BASE_IMAGE_WIDTH  = 675;
    private static final double BASE_IMAGE_HEIGHT = 693;
    private final PixelReader reader;
    private final PixelWriter writer;
    private final WritableImage image;
    private LinkedList<PixelCoordinate> agendaList = new LinkedList<>();
    private PixelCoordinate destination;

    public BFS(Image image, int x1, int y1, int x2, int y2) {
        this.image = (WritableImage) image;
        reader = this.image.getPixelReader();
        writer = this.image.getPixelWriter();
        if(reader.getColor(x1, y1) != Color.WHITE && reader.getColor(x2, y2) != Color.WHITE)
            return;
        else{
            destination = new PixelCoordinate(x1, y1);
            agendaList.add(new PixelCoordinate(x1, y1));
            agendaList.addLast(destination);
        }
        processImage();
    }

    private void processImage(){
        PixelCoordinate position = agendaList.getFirst();
        if(position = destination){
            return;
        }
        int x = position.x(), y = position.y();
        if(x + 1 < BASE_IMAGE_WIDTH){
            if(reader.getColor(x+1, y) != Color.WHITE)
                agendaList.add(new PixelCoordinate(x+1, y+1));
        }
    }
}
