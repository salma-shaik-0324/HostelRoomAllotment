public class Allotment {

    public enum AllotmentStatus {
        ACTIVE,
        CHECKED_OUT
    }

    private final int id;
    private final int studentId;
    private final int roomId;
    private final int bedNumber;
    private AllotmentStatus status;

    public Allotment(int id, int studentId, int roomId, int bedNumber, AllotmentStatus status) {
        if (bedNumber < 1 || bedNumber > Room.BEDS_PER_ROOM) {
            throw new IllegalArgumentException("Bed number must be 1 to " + Room.BEDS_PER_ROOM);
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.id = id;
        this.studentId = studentId;
        this.roomId = roomId;
        this.bedNumber = bedNumber;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getRoomId() {
        return roomId;
    }

    public int getBedNumber() {
        return bedNumber;
    }

    public AllotmentStatus getStatus() {
        return status;
    }

    /** Changes ACTIVE to CHECKED_OUT. A repeated checkout is refused. */
    public void checkout() {
        if (status != AllotmentStatus.ACTIVE) {
            throw new IllegalStateException("Allotment is already checked out");
        }
        status = AllotmentStatus.CHECKED_OUT;
    }
}