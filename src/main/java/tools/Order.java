package tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Order {
    private int id;
    private int mealId;
    private int patientId;
    private int employeeId;
    private Date orderDate;
    private Map<Integer, Food> foods; // food_id, food

    public Order() {
        id = -1;
        foods = new HashMap<>();
        mealId = -1;
        patientId = -1;
        employeeId = -1;
        orderDate = null;
    }

    public Order(int patientId, int employeeId, int mealId, Date orderDate) {
        id = -1;
        foods = new HashMap<>();
        this.mealId = mealId;
        this.patientId = patientId;
        this.employeeId = employeeId;
        this.orderDate = orderDate;
    }
    public void importOrder(ResultSet resultSet) {
        // will do nothing if empty result set.
        try {
            if (resultSet.next()) {
                id = resultSet.getInt("order_id");
                mealId = resultSet.getInt("meal_id");
                patientId = resultSet.getInt("patient_id");
                int employeeTemp = resultSet.getInt("employee_id");
                employeeId = (employeeTemp != 0) ? employeeTemp : -1;
                orderDate = resultSet.getDate("order_date");
                do {
                    Food food = new Food(resultSet);
                    food.setQuantity(resultSet.getInt("quantity"));
                    foods.put(food.getId(), food);
                } while (resultSet.next());
            }
        } catch (SQLException e) {
            System.out.println("[!] An error occurred.");
            e.printStackTrace();
        }
    }

    /**
     * Will either update a foods quantity value or add a new food.
     * @param foodId ID of the food to be added/updated
     * @param food Food object, when there is a food not in the order
     * @param quantity Quantity of food to be added/updated
     */
    public void addFood(int foodId, Food food, int quantity) {
        if (foods.containsKey(foodId)) {
            Food foodToChange = foods.get(foodId);
            foodToChange.addQuantity(quantity);
        }
        else {
            foods.put(foodId, food);
            food.setQuantity(quantity);
        }
    }

    /**
     * Will remove a given quantity of a specific Food.
     * Will do nothing if food is not in order.
     * Will remove food from order if new quantity <= 0.
     * @param foodId Specified food
     * @param quantityToRemove Number to remove
     */
    public void removeFood(int foodId, int quantityToRemove) {
        if (foods.containsKey(foodId)) {
            Food food = foods.get(foodId);
            if (food.getQuantity() <= quantityToRemove) {
                foods.remove(foodId);
            }
            else {
                food.removeQuantity(quantityToRemove);
            }
        }
    }

    /**
     * Will create a formatted string with order information.
     * Or an empty string if none are present.
     * @return Formatted order string.
     */
    public String orderCard() {
        if (foods.isEmpty()) { // when order is empty
            return "";
        }

        int totalCalories = 0;
        int totalProtein = 0;
        int totalCarbohydrates = 0;
        int totalFat = 0;
        String topOfHeader = ".` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`\n";
        String middleOfHeader = (id == -1) ? "[+] NEW ORDER\n" : "[+] ORDER [#%d]\n".formatted(id); // if order in progress or completed
        String bottomOfHeader = "quantity           | food                                | calories | protein | carbohydrates | fat |\n";
        String header = topOfHeader + middleOfHeader + bottomOfHeader;
        String body = "";

        for (int foodId: foods.keySet()) {
            Food food = foods.get(foodId);
            body += "%3dx [%5.1f %-5s] | %-35s | %8d | %6dg | %12dg | %2dg |\n".formatted(
                    food.getQuantity(),
                    food.getMeasurement(),
                    food.getUnit(),
                    food.getName(),
                    food.getCalories(),
                    food.getProtein(),
                    food.getCarbohydrates(),
                    food.getFat()
            );
            totalCalories += food.getCalories();
            totalProtein += food.getProtein();
            totalCarbohydrates += food.getCarbohydrates();
            totalFat += food.getFat();
        }


        String footer = """
                total:                                                   | %8d | %6dg | %12dg | %2dg |
                .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .` .`""".formatted(
                totalCalories,
                totalProtein,
                totalCarbohydrates,
                totalFat
        );

        return header + body + footer;
    }

    public int getId() {
        return id;
    }

    public boolean isEmpty() {
        return foods.isEmpty(); // empty if there are no foods.
    }

    public Map<Integer, Food> getFoods() {
        return foods;
    }

    public int getFoodQuantity(int foodId) {
        return foods.get(foodId).getQuantity();
    }

    public int getMealId() {
        return mealId;
    }

    public int getPatientId() {
        return patientId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public Date getOrderDate() {
        return orderDate;
    }
}
