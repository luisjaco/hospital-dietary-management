package tools;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Food {
    private int id;
    private String name;
    private double measurement;
    private String unit;
    private int calories;
    private int protein;
    private int carbohydrates;
    private int fat;
    private int quantity;
    /**
     * Creates a new Food object based on a result set.
     * @param resultSet SQL result set
     */
    public Food(ResultSet resultSet) {
        try {
            id = resultSet.getInt("food_id");
            name = resultSet.getString("food");
            measurement = resultSet.getDouble("measurement");
            unit = resultSet.getString("unit");
            calories = resultSet.getInt("calories");
            protein = resultSet.getInt("protein");
            carbohydrates = resultSet.getInt("carbohydrates");
            fat = resultSet.getInt("fat");
            quantity = 0;
        } catch (SQLException e) {
            System.out.println("[!] An error occurred.");
            e.printStackTrace();
        }
    }

    public String getFoodRow() {
        return  "%2d | %-35s | %-5.1f %-5s | %8d | %6dg | %12dg | %2dg |".formatted(
                id,
                name,
                measurement,
                unit,
                calories,
                protein,
                carbohydrates,
                fat);
    }
    public int getQuantity() {
        return quantity;
    }
    public void addQuantity(int value) {
        quantity += value;
    }
    public void removeQuantity(int value) {
        quantity -= value;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getMeasurement() {
        return measurement;
    }

    public String getUnit() {
        return unit;
    }

    public int getCalories() {
        return calories;
    }

    public int getProtein() {
        return protein;
    }

    public int getCarbohydrates() {
        return carbohydrates;
    }

    public int getFat() {
        return fat;
    }
}
