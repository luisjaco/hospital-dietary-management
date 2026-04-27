package tools;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import tools.Order;
/**
 * Handles all the projects tools.SQL connections and queries.
 */
public class SQL {

    private Connection connection;

    public SQL() {
        // won't do much...
    }

    public boolean establishConnection(String url, String root, String password) {
        if (isConnected()) {
            System.out.println("[X] Connection already established.");
            return true;
        }

        try {
            connection = DriverManager.getConnection(url, root, password);
            System.out.println("[!] Connection to database established.");
            return true;
        } catch (SQLException e) {
            System.out.println("[X] Connection to database failed.");
            return false;
        }
    }

    public boolean isConnected() {
        if (connection != null) {
            try {
                return !connection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }
        else {
            return false;
        }
    }

    public void closeConnection() {
        if (!isConnected()) {
            return;
        }

        try {
            connection.close();
            //TODO, remove user information
            System.out.println("[!] Connection successfully closed.");
        } catch (SQLException e) {
            System.out.println("[X] Error closing connection.");
        }
    }

    /**
     * Authenticates username and password of a user.
     * @param username Defined username.
     * @param password Defined password.
     * @param isEmployee If user is signing in as employee. (false-> patient sign in).
     * @return New User with user data.
     * Will return a User with default data if not signed in.
     */
    public User signIn(String username, String password, boolean isEmployee) {
        User user = new User();
        try {
            // either use employees or patients table.
            String q = "SELECT * FROM %s WHERE username=? AND password=?;".formatted( (isEmployee ? "employees" : "patients"));
            PreparedStatement query = connection.prepareStatement(q);
            query.setString(1, username);
            query.setString(2, password);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                user = new User(resultSet, isEmployee);
                System.out.println("[!] Sign in successful.");
            }
            if (user.getRank() == 1) {
                getDiets(user);
            }
        } catch (SQLException e) {
            System.out.println("[X] An error occurred.");
        }
        return user;
    }

    public void getDiets(User user) {
        if (user.getRank() != 1) {
            System.out.println("[!] An error occurred.");
            return;
        }

        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT
                    	patient_id,
                        patients_diets.diet_id,
                        diets.name
                    FROM patients_diets
                    JOIN diets
                    	ON patients_diets.diet_id = diets.diet_id
                    WHERE patient_id = ?;""");
            query.setInt(1, user.getId());
            ResultSet resultSet = query.executeQuery();

            user.setPatientDiets(resultSet);
        } catch (SQLException e) {
            System.out.println("[!] An error occurred.");
        }
    }

    /**
     * Returns a formatted string of patient orders, ordered by most recent.
     * Will return an empty string if there are no orders.
     * @param user Patient user
     * @return Formatted string containing order id, patient username,
     * employee username, date, and macros (calories, protein, carbohydrates, fat)
     */
    public String getPreviousOrders(User user) {
        String result = "";
        if (user.getRank() != 1) { // ensure user is patient
            System.out.println("[X] An error occurred.");
            return result;
        }

        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT
                        order_contents.order_id,
                        patients.patient_id,
                        patients.username AS 'patient',
                        employees.username AS 'employee',
                        meals.meal_id,
                        meals.name AS 'meal',
                        order_date,
                        SUM(foods.calories) AS 'calories',
                        SUM(foods.protein) AS 'protein',
                        SUM(foods.carbohydrates) AS 'carbohydrates',
                        SUM(foods.fat) AS 'fat'
                    FROM order_contents
                    JOIN orders
                    	ON order_contents.order_id = orders.order_id
                    JOIN meals
                    	ON orders.meal_id = meals.meal_id
                    LEFT JOIN employees
                    	ON orders.employee_id = employees.employee_id
                    JOIN patients
                    	ON orders.patient_id = patients.patient_id
                    JOIN foods
                    	ON order_contents.food_id = foods.food_id
                    GROUP BY order_contents.order_id
                    HAVING patients.patient_id=?
                    ORDER BY order_date DESC, meal_id DESC;
                    """);
            query.setInt(1, user.getId());

            ResultSet resultSet = query.executeQuery();

            while (resultSet.next()) {
                String order = String.format("""
                    .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`
                    [+] ORDER [#%d]
                    patient      | employee    | meal           | date       | calories | protein | carbohydrates | fat  |
                    %-12s | %-12s| %-15s| %-11s| %8d | %6dg | %12dg | %3dg |
                    """,
                        resultSet.getInt("order_id"),
                        resultSet.getString("patient"),
                        resultSet.getString("employee"),
                        resultSet.getString("meal"),
                        formattedDate(resultSet.getDate("order_date")),
                        resultSet.getInt("calories"),
                        resultSet.getInt("protein"),
                        resultSet.getInt("carbohydrates"),
                        resultSet.getInt("fat")
                );
                result += order;
                if (resultSet.isLast()) {
                    result += ".` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`";
                }
            }
        } catch (SQLException e) {
            System.out.println("[X] An error occurred.");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Returns an order
     * @param user Patient user
     * @param orderNumber Desired order number
     * @return Specified order, or empty order if invalid.
     */
    public Order getOrder(User user, int orderNumber) {
        Order order = new Order();
        if (user.getRank() != 1) {
            System.out.println("[!] An error occurred.");
            return order;
        }

        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT
                        order_contents.order_id,
                        orders.patient_id,
                        orders.employee_id,
                        orders.meal_id,
                        orders.order_date,
                        foods.food_id,
                        quantity,
                        foods.category_id,
                        foods.name as 'food',
                        foods.calories,
                        foods.protein,
                        foods.carbohydrates,
                        foods.fat,
                        foods.measurement,
                        units.name as 'unit'
                    FROM order_contents
                    JOIN foods
                        ON order_contents.food_id = foods.food_id
                    JOIN units
                        ON foods.unit_id = units.unit_id
                    JOIN orders
                        ON order_contents.order_id = orders.order_id
                    WHERE order_contents.order_id=? AND patient_id=?
                    ORDER BY foods.category_id ASC;
                    """);
            query.setInt(1, orderNumber);
            query.setInt(2, user.getId());
            ResultSet resultSet = query.executeQuery();

            order.importOrder(resultSet);

            } catch (SQLException e) {
                System.out.println("[!] An error occurred.");
                e.printStackTrace();
            }
            return order;
        }

    /**
    * Converts a Date into a string in the mm-dd-yyyy format.
    * @param date Date to format
    * @return Date as string in mm-dd-yyyy format
    */
    private String formattedDate(Date date) {
       SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
       return formatter.format(date);
    }

    /**
     * Retrieves a list of Foods. All foods will pertain to the users diet.
     * Will return an empty list if no foods match selection.
     * @param user Patient to base diets off of
     * @param category Category of foods desired
     * @return ArrayList of foods
     */
    public ArrayList<Food> getFoods(User user, int category) {
        ArrayList<Food> foods = new ArrayList<>();
        if (user.getRank() != 1) {
            return foods;
        }

        // Create a query based on the users diets.
        int[] dietIds = user.getDietIDs();
        String top = """
                    SELECT
                    	foods.food_id,
                        foods.name as 'food',
                        foods.category_id,
                    	measurement,
                        units.name as 'unit',
                        calories,
                        protein,
                        carbohydrates,
                        fat
                    FROM foods
                    JOIN units
                    	ON foods.unit_id = units.unit_id
                    """;
        String middle = "WHERE category_id = ?";
        String bottom = "GROUP BY foods.food_id;";
        // alter sql joins for the dietIds.
        for (int dietId : dietIds) {
            top += """
                        JOIN foods_diets fd%d
                            ON foods.food_id = fd%d.food_id
                        """.formatted(dietId, dietId);
            middle += " AND fd%d.diet_id=%d".formatted(dietId, dietId);
        }
        String q = top + middle + "\n" + bottom;

        try {
            PreparedStatement query = connection.prepareStatement(q);
            query.setInt(1, category);

            ResultSet resultSet = query.executeQuery();

            while (resultSet.next()) {
                Food food = new Food(resultSet);
                foods.add(food);
            }
        } catch (SQLException e) {
            System.out.println("[!] An error occurred.");
            e.printStackTrace();
        }
        return foods;
    }

    public void createOrder(Order order) {
        int id = getNextOrderId();
        if (order.isEmpty() || id == -1) {
            System.out.println("[!] An error occurred.");
        }

        // create query
        String createOrder = """
                INSERT INTO orders (order_id, patient_id, employee_id, meal_id, order_date)
                VALUES (?, ?, ?, ?, ?);""";

        String createOrderContents = "INSERT INTO order_contents (order_id, food_id, quantity) VALUES ";
        boolean firstValue = true;
        for (int foodId : order.getFoods().keySet()) {
            if (firstValue) {
                createOrderContents += "(%d, %d, %d)".formatted(id, foodId, order.getFoodQuantity(foodId));
                firstValue = false;
            } else {
                createOrderContents += ", (%d, %d, %d)".formatted(id, foodId, order.getFoodQuantity(foodId));
            }
        }

        try {
            // create order in orders
            PreparedStatement orderUpdate = connection.prepareStatement(createOrder);
            orderUpdate.setInt(1, id);
            orderUpdate.setInt(2, order.getPatientId());
            int employeeId = order.getEmployeeId();
            if ((employeeId != -1)) {
                orderUpdate.setInt(3, employeeId);
            } else {
                orderUpdate.setNull(3, Types.INTEGER);
            }
            orderUpdate.setInt(4, order.getMealId());
            Date sqlDate = new java.sql.Date(order.getOrderDate().getTime());
            orderUpdate.setDate(5, sqlDate);

            orderUpdate.executeUpdate();

            // fill order_contents
            PreparedStatement orderContentsUpdate = connection.prepareStatement(createOrderContents);
            orderContentsUpdate.executeUpdate();
            System.out.println("[!] Order successfully created.");
        } catch (SQLException e) {
            System.out.println("[X] An error occurred.");
        }
    }

    /**
     * Next unused order id.
     * Will return -1 if an error occurs.
     * @return Next order id
     */
    public int getNextOrderId() {
        try {
            PreparedStatement query = connection.prepareStatement("SELECT MAX(order_id) + 1 AS 'id' FROM orders;");
            ResultSet resultSet = query.executeQuery();
            resultSet.next();
            return resultSet.getInt("id");
        } catch (SQLException e) {
            System.out.println("[X] An error occurred.");
            e.printStackTrace();
        }
        return -1;
    }
}



