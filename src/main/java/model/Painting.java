package model;
import java.util.Objects;
/**
 * Represents a painting displayed in the room
 */
public class Painting {

    //Title of a Painting
    private String title;
    //Full Name of Artist (not a reference to an Artist object)
    private String artist;
    // The UI resolves the full path
    private String imageFilename;
    //Small description shown in the UI when a visitor (optional)
    private String description;

    // No-arg constructor required by XStream for XML deserialization.
    public Painting(){}

    //Full constructor
    //Null guards on title and artist only.
    //These two are mandatory for the app to function — an untitled or authorless painting breaks the routing algorithm.
    public Painting(String title, String artist, String imageFilename, String description) {
        if (title == null)  throw new IllegalArgumentException("Title must not be null.");
        if (artist == null) throw new IllegalArgumentException("Artist must not be null.");
        this.title = title;
        this.artist = artist;
        this.imageFilename = imageFilename;
        this.description = description;
    }

     //Minimal constructor for creating a Painting with just title and artist.
     //Useful in tests and when image/description data is not yet available
    public Painting(String title, String artist) {
        if (title == null)  throw new IllegalArgumentException("Title must not be null.");
        if (artist == null) throw new IllegalArgumentException("Artist must not be null.");
        this.title = title;
        this.artist = artist;
    }

    //Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Hash code based on {@code title} and {@code artist}, consistent with {@link #equals}. */
    @Override
    public int hashCode() {
        int result = title  != null ? title.hashCode()  : 0;
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Painting)) return false;
        Painting other = (Painting) o;
        return Objects.equals(this.title, other.title)
                && Objects.equals(this.artist, other.artist);
    }
}
