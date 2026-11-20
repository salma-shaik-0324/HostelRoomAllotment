public class Admin extends User {

    public Admin() {
        super(0, "Admin");
    }

    @Override
    public void showProfile() {
        System.out.println("Role: Admin | ID: " + getId() + " | Name: " + getName());
    }
}