package model;

import java.util.*;

/**
 * Represents a single room (node/vertex)
 */
public class Room {
    //unique id of a room (matches gallery floor pan numbering)
    private int id;

    // Human readable display
    private String name;

    // positioning route markers
    private int x;
    private int y;

    // current painting in the room, initialize in constructor -> safe iteration
    private List<Painting> paintings;

    //No-arg constructor required by XStream for XML deserialisation.
    public Room(){
        this.paintings = new ArrayList<>();
    }

    public Room(int id, String name, int x, int y){
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.paintings = new ArrayList<>();
    }
    //Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public List<Painting> getPaintings() {
        return paintings;
    }

    public void setPaintings(List<Painting> paintings) {
        this.paintings = paintings;
    }
    //Add painting
    public void addPainting(Painting painting) {
        if (painting == null) throw new IllegalArgumentException("Painting must not be null.");
        paintings.add(painting);
    }

     // Two rooms are equal if and only if they share the same {@code id}.
     // This is consistent with how rooms are looked up and compared in the graph.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room other = (Room) o;
        return this.id == other.id;
    }

    // Hash code based solely on {@code id}, consistent with {@link #equals}.
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
