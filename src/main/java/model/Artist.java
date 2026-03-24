package model;

/**
 * Artist is a visitor preference object.
 * The assignment says the "most interesting route" is based on "a list of artists a visitor is particularly interested in".
 * So Artist serves two purposes:
 *
 * A record in the database — stored in the XML so the UI can present a selectable list of artists to the visitor
 * A preference entry — the visitor picks artists from that list, and SearchAlgorithms uses them to weight rooms during Dijkstra's
 *
 * Important: the {name} field must exactly match the {artist}
 *  * string stored in each {Painting}. This is the join key used by the routing
 *  * algorithm to determine whether a room contains a painting by a preferred artist.
 *  * Any inconsistency between these two values will silently break artist-preference
 *  * matching — keep them in sync in the XML database.
 */

public class Artist {
    //Full name of the artist, exactly as it appears in {Painting#getArtist()}
    //This is the join key between {@code Artist} and {@code Painting}.
    // Example: {"Johannes Vermeer"}.
    private String name;

    //might add some optional field such as nationality or period
    //left commented for now (feels redundant)

    private String period;
    private String nationality;

    public Artist(){}

    public Artist(String name) {
        if (name == null) throw new IllegalArgumentException("Artist name cannot be null");
        this.name = name;
    }

    //optional full constructor
//    public Artist(String name, String period, String nationality){
//        if (name == null) throw new IllegalArgumentException("Artist name cannot be null");
//        this.name = name;
//        this.period = period;
//        this.nationality = nationality;
//    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public String getPeriod() {
//        return period;
//    }
//
//    public void setPeriod(String period) {
//        this.period = period;
//    }
//
//    public String getNationality() {
//        return nationality;
//    }
//
//    public void setNationality(String nationality) {
//        this.nationality = nationality;
//    }


    /**
     * Two artists are equal if they share the same {@code name}.
     * This is consistent with how the routing algorithm matches artist preferences
     * against painting records — by name alone.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist)) return false;
        Artist other = (Artist) o;
        return name != null && name.equals(other.name);
    }

    /** Hash code based solely on {@code name}, consistent with {@link #equals}. */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
