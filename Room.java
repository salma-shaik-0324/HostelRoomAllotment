public class Room {
    public static final int BEDS_PER_ROOM = 4;

    private final int id;
    private final String roomNumber;

    public Room(int id, String roomNumber) {
        this.id = id;
        this.roomNumber = roomNumber;
    }

    public int getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void showDetails() {
        System.out.println("Room ID: " + id + " | Room: " + roomNumber
                + " | Beds: " + BEDS_PER_ROOM);
    }
}