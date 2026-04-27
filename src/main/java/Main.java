import tools.Menu;
public class Main {
    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:3306/dietary?serverTimezone=UTC";
        String user = "root";
        String password = "LJb78-Top";

        Menu menu = new Menu();
        menu.setup(url, user, password);
        menu.run();
    }
}