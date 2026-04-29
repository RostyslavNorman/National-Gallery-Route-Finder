package data;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import model.Artist;
import model.Doorway;
import model.Painting;
import model.Room;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryDataParser {
    private Document data;
    private final List<Artist> artists = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Painting> paintings = new ArrayList<>();
    private final Map<Artist, List<Painting>> artistPaintingsHashMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> connections = new HashMap<>();

    public GalleryDataParser() {
        loadFile("");
    }

    public GalleryDataParser(String path) {
        loadFile(path);
    }

    private void loadFile(String path){
        if(path == null || path.isEmpty())
            path = "src/main/resources/gallery_data.xml";
        try{
            File galleryData = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            data = dBuilder.parse(galleryData);
            data.getDocumentElement().normalize();

            readArtists();
            readRoomsPaintingsAndConnectionsAndDoorways(); // paintings & connections are stored in room, I don't want to reprocess the doc multiple times
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readArtists(){
        NodeList rawArtists = data.getElementsByTagName("artists");
        Element artistsElement = (Element) rawArtists.item(0);
        NodeList list = artistsElement.getElementsByTagName("artist");

        String name, period, nationality;
        Element e;
        NodeList children;
        for(int i = 0; i < list.getLength(); i++){
            e = (Element) list.item(i);
            children = e.getChildNodes();
            name = children.item(0).getTextContent();
            nationality = children.item(1).getTextContent();
            period = children.item(2).getTextContent();
            artists.add(new Artist(name, period, nationality));
        }
        for(Artist artist : artists)
            artistPaintingsHashMap.put(artist, new ArrayList<>());
    }

    private void readRoomsPaintingsAndConnectionsAndDoorways(){
        // id (int), n (string), x (int), y (int), connections[roomid, distance] (int)
        // paintings[painting(title, artist, imageFilename, description)] (string)
        NodeList rawRooms = data.getElementsByTagName("rooms");
        Element roomsElement = (Element) rawRooms.item(0);
        NodeList list = roomsElement.getElementsByTagName("room");
        // Room(String id, String name, int x, int y)
        int x, y;
        String id, name;
        Element e;
        for(int i = 0; i < list.getLength(); i++){
            e =  (Element) list.item(i);
            id = e.getElementsByTagName("id").item(0).getTextContent();
            name = e.getElementsByTagName("n").item(0).getTextContent();
            x = Integer.parseInt(e.getElementsByTagName("x").item(0).getTextContent());
            y = Integer.parseInt(e.getElementsByTagName("y").item(0).getTextContent());
            Room room = new Room(id, name, x, y);
            rooms.add(room);

            readPaintings(e.getElementsByTagName("paintings"), room);

            if(connections.containsKey(id))
                throw new IllegalArgumentException("ID already registered for connections Map!");
            else
                connections.put(id, new HashMap<>());
            readConnections(e.getElementsByTagName("connections"), id, room);
        }
    }

    private String checkId(String id){
        if(!id.matches("\\d+")) { //51a and the like are a problem
            id = id.replaceAll("[a-zA-Z]+", "");
            id += "1";
        }
        return id;
    }

    private void readPaintings(NodeList paintingsList, Room room){
        Element paintingsElement = (Element) paintingsList.item(0);
        NodeList list = paintingsElement.getElementsByTagName("painting");
        List<Painting> roomPaintings = new ArrayList<>();
        String title, artist, imageFileName, description;
        for(int i = 0; i < list.getLength(); i++){
            Element e = (Element) list.item(i);
            //Painting(String title, String artist, String imageFilename, String description)
            title = e.getElementsByTagName("title").item(0).getTextContent();
            artist = e.getElementsByTagName("artist").item(0).getTextContent();
            imageFileName = e.getElementsByTagName("imageFilename").item(0).getTextContent();
            description = e.getElementsByTagName("description").item(0).getTextContent();
            Painting painting = new Painting(title, artist, imageFileName, description);
            roomPaintings.add(painting);
        }
        paintings.addAll(roomPaintings);
        room.setPaintings(roomPaintings);
        for(Painting painting : paintings)
            for(Artist artist1 : artists)
                if(artist1.getName().equalsIgnoreCase(painting.getArtist())) {
                    artistPaintingsHashMap.get(artist1).add(painting);
                    break;
                }
    }

    private void readConnections(NodeList connectionsList, String key, Room room){
        Element connectionsElement = (Element) connectionsList.item(0);
        NodeList list = connectionsElement.getElementsByTagName("connection");
        String connectionKey;
        int connectionValue;
        List<Doorway> doorways = new ArrayList<>();
        for(int i = 0; i < list.getLength(); i++){
            Element e = (Element) list.item(i);
            connectionKey = e.getElementsByTagName("roomId").item(0).getTextContent();
            connectionValue = Integer.parseInt(e.getElementsByTagName("distance").item(0).getTextContent());

            NodeList throughpoints = e.getElementsByTagName("throughpoint");
            if(throughpoints.getLength() > 0) {
                Element tp = (Element) throughpoints.item(0);
                int x = Integer.parseInt(tp.getElementsByTagName("x").item(0).getTextContent());
                int y = Integer.parseInt(tp.getElementsByTagName("y").item(0).getTextContent());
                doorways.add(new Doorway(x, y, connectionKey));
            }

            connections.get(key).put(connectionKey, connectionValue);
        }
        room.setDoorways(doorways);
    }

    public List<Artist> getArtists(){
        return artists;
    }

    public List<Room> getRooms(){
        return rooms;
    }

    public List<Painting> getPaintings(){
        return paintings;
    }

    public Map<Artist, List<Painting>> getArtistPaintingsMap(){
        return artistPaintingsHashMap;
    }

    public Map<String, Map<String, Integer>> getConnections(){
        return connections;
    }
}
