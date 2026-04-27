package tools;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Menu {
    private Input input;
    private boolean connectedToDB;
    private boolean active;
    private SQL sql;
    private User user;

    public Menu() {
        active = false;
        connectedToDB = false;
        user = new User(); // default user, will have signedIn=false.
    }

    /**
     * Begins menu execution.
     * Use setup() and establish connection before using start().
     */
    public void run() {
        active = true;
        if (!connectedToDB) {
            System.out.println("[X] Please connect database before starting menu.");
            return;
        }
        System.out.println("[!] WELCOME TO THE DIETARY DATABASE.");

        while (active) {
            signIn();

            switch (user.getRank()) {
                case -1 -> close(); // user signed out, and used the "exit program option"
                case 1 -> patientMenu();
            }
            // if user reached here, means they've used the "Sign out" option.
            signOut();
        }
    }

    /**
     * Will connect to a database. Connection required before using start().
     */
    public void setup(String url, String user, String password) {
        input = new Input();
        input.open();
        sql = new SQL();
        connectedToDB = sql.establishConnection(url, user, password);
    }

    /**
     * Closes Scanner and SQL classes. Effectively resets the menu.
     */
    private void close() {
        if (active) {
            System.out.println("[!] Closing program.");
            active = false;
            sql.closeConnection();
            input.close();
        }
    }

    private void signIn() {
        while (active && !user.isSignedIn()) {
            System.out.print("""
                    x--------------------------------------------x
                                    PLEASE SIGN IN!
                    [1] Patient sign-in
                    [0] Exit program
                    x--------------------------------------------x
                    """);
            int response = input.getInt(0, 2);

            switch (response) {
                case 2, 1 -> {
                    System.out.println("[!] Enter username:");
                    String username = input.getString();
                    System.out.println("[!] Enter password:");
                    String password = input.getString();
                    boolean isEmployee = (response == 2);

                    User user = sql.signIn(username, password, isEmployee);

                    if (user.isSignedIn()) { // loop if not signed in
                        this.user = user;
                    }
                    else {
                        System.out.println("[!] The username or password is not correct.");
                    }
                }
                case 0 -> close();
            }
        }
    }

    private void signOut() {
        if (user.isSignedIn()) {
            user = new User(); // new default user. Old user is lost.
            System.out.println("[!] User successfully signed out.");
        }
    }

    private void patientMenu() {
        boolean patientMenuActive = true;
        while (active && patientMenuActive) {
            System.out.printf("""
                    x--------------------------------------------x
                                        HOME
                    %s
                    
                    [2] Previous orders
                    [1] Order meal
                    [0] Sign out
                    x--------------------------------------------x
                    """,
                    user.userCard());

            int response = input.getInt(0, 2);
            switch (response) {
                case 2 -> getOrders(user);
                case 1 -> orderMealPatient();
                case 0 -> {
                    signOut();
                    patientMenuActive = false;
                }
            }
        }
    }

    private void getOrders(User user) {
        String orders = sql.getPreviousOrders(user);
        System.out.println("[+] ORDERS FOR [%s]:".formatted(user.getName()));
        if (orders.equals("")) { // if no orders
            System.out.print("""
                    .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`
                    [!] There is no order history.
                    .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`
                    """);
        }
        else {
            System.out.println(orders);
            viewOrdersMenu(user);
        }
    }

    private void viewOrdersMenu(User user) {
        boolean viewOrdersMenuActive = true;
        while (active && viewOrdersMenuActive) {
            System.out.print("""
                x--------------------------------------------x
                [1] View order
                [0] Go Back
                x--------------------------------------------x
                """);
            int response = input.getInt(0, 1);

            switch (response) {
                case 1 -> {
                    System.out.println("[!] Enter order #:");
                    int orderNumber = input.getInt();
                    Order order = sql.getOrder(user, orderNumber);

                    if (order.getId() <= 0) {
                        System.out.println("[!] Invalid order number provided.");
                    }
                    else {
                        System.out.println(order.orderCard());
                    }
                }
                case 0 -> viewOrdersMenuActive = false;
            }
        }
    }

    private void orderMealPatient() {
        // patient orders meal for themselves
        // pick meal, go back  -> pick categories, cancel order -> pick foods, go back

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        Date date = Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        String dateString = currentDate.format(formatter);

        System.out.printf("""
                    x--------------------------------------------x
                             ORDER MEAL ON [%s]
                    [3] Dinner
                    [2] Lunch
                    [1] Breakfast
                    [0] Go back
                    x--------------------------------------------x
                    """,
                dateString);
        int response = input.getInt(0, 7);
        switch (response) {
            case 3 -> createOrder(5, user, null, date); // meal id for dinner
            case 2 -> createOrder(3, user, null, date); // meal id for lunch
            case 1 -> createOrder(1, user, null, date); // meal id for breakfast
        }
        // case 0 will simply end function
    }

    private void createOrder(int meal_id, User patient, User employee, Date orderDate) {
        // pick categories, cancel order -> pick foods, go back
        Order order;

        if (employee != null) {
            order = new Order(patient.getId(), employee.getId(), meal_id, orderDate);
        }
        else {
            order = new Order(patient.getId(), -1, meal_id, orderDate);
        }

        boolean createOrderMenuActive = true;
        while (active && createOrderMenuActive) {
            System.out.printf("""
                    x--------------------------------------------x
                                   PICK CATEGORY
                    FOR DIETS: %s
                    [1] Appetizer | [2] Entree     | [3] Vegetable
                    [4] Bread     | [5] Hot drink  | [6] Cold drink
                    [7] Fruit     | [8] Dessert    | [9] Condiment
                    [10] Place order
                    [0] Go back
                    x--------------------------------------------x
                    """, user.listDiets());

            int response = input.getInt(0, 10);
            switch (response) {
                case 1, 2, 3, 4, 5, 6, 7, 8, 9 -> {
                    pickFoodMenu(order, user, response);
                    System.out.printf("""
                        [!] YOUR CURRENT ORDER
                        %s
                        """, order.orderCard());
                }
                case 10 -> {
                    System.out.printf("""
                        [!] YOUR CURRENT ORDER
                        %s
                        """, order.orderCard());
                    System.out.print("""
                        [!] Are you sure you want to place this order?
                        [1] Yes
                        [0] No
                        """);
                    int placeOrderResponse = input.getInt(0, 1);
                    if (placeOrderResponse == 1) {
                        sql.createOrder(order);
                        createOrderMenuActive = false;
                    }
                }
                case 0 -> {
                    System.out.print("""
                        [!] Are you sure you want to exit? All data will be lost.
                        [1] Yes
                        [0] No
                        """);
                    int exitResponse = input.getInt(0, 1);
                    createOrderMenuActive = (exitResponse == 0); // will turn false if 1 (user exits).
                }
            }
        }
    }

    private void pickFoodMenu(Order order, User user, int category) {
        ArrayList<Food> foods = sql.getFoods(user, category);
        if (foods.isEmpty()) {
            System.out.println("""
                        .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`
                        [!] No foods within diet available for this category.
                        .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`
                        """);
        }
        else {
            System.out.print("""
                    .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`
                    [!] FOOD OPTIONS:
                    id | food                                | portion     | calories | protein | carbohydrates | fat |
                    """);
            for (Food food : foods) {
                System.out.println(food.getFoodRow());
            }
            System.out.println(".` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`");

            boolean pickFoodMenuActive = true;
            System.out.println("[!] Enter food id to add or [0] to go back:");
            while (active && pickFoodMenuActive) {
                int response = input.getInt();

                if (response == 0) {
                    pickFoodMenuActive = false; // return to category select.
                } else {
                    Food foodToAdd = getFoodByID(response, foods);
                    if (foodToAdd != null) { // will loop if null
                        System.out.println("[!] Enter quantity of food [1-10]: ");
                        int quantity = input.getInt(1, 10);

                        order.addFood(response, foodToAdd, quantity);
                        System.out.println("[!] Successfully added food to order.");
                        pickFoodMenuActive = false;
                    } else {
                        System.out.println("[!] Please enter valid food id or [0] to go back.");
                    }
                }
            }
        }
    }
    private Food getFoodByID(int foodID, ArrayList<Food> foods) { // will return null if not present
        for (Food food : foods) {
            if (food.getId() == foodID) return food;
        }
        return null;
    }

}
