package tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds user information.
 */
public class User {
    private int rank; // 1: patient, 2: employee, 3: admin
    private int id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private Date dob;
    private boolean admin;
    private boolean signedIn;
    private Map<Integer, String> diets;
    public User() {
        rank = -1;
        id = -1;
        username = null;
        password = null;
        firstName = null;
        lastName = null;
        dob = null;
        admin = false;
        signedIn = false;
        diets = null;
    }

    public User(ResultSet resultSet, boolean isEmployee) {
        try {
            if (isEmployee) {
                id = resultSet.getInt("employee_id");
                admin = resultSet.getBoolean("admin");
                firstName = resultSet.getString("first_name");
                lastName = resultSet.getString("last_name");
                username = resultSet.getString("username");
                password = resultSet.getString("password");
                rank = admin ? 3 : 2;
                signedIn = true;
            } else { // patient sign-in
                id = resultSet.getInt("patient_id");
                firstName = resultSet.getString("first_name");
                lastName = resultSet.getString("last_name");
                username = resultSet.getString("username");
                password = resultSet.getString("password");
                dob = resultSet.getDate("dob");
                rank = 1;
                signedIn = true;
                diets = new HashMap<>();
            }
        } catch (SQLException e) {
            System.out.println("[!] An error occurred.");
            e.printStackTrace();
        }
    }

    public void setPatientDiets(ResultSet resultSet) {
        try {
            while (resultSet.next()) {
                int dietId = resultSet.getInt("diet_id");
                String dietName = resultSet.getString("name");
                diets.put(dietId, dietName);
            }
        } catch (SQLException e) {
            System.out.println("[!] An error occurred.");
        }
    }

    /**
     * Constructs a card with the users information
     * @return Formatted string with users information
     */
    public String userCard() {
        String result;

        switch (rank) {
            case 1 -> {
                SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
                String formattedDOB = formatter.format(dob);
                result = "[-] PATIENT #%d [%s]\n%s %s [%s] ".formatted(id, username, firstName, lastName, formattedDOB);
            }
            case 2 -> {
                result = "[=] EMPLOYEE #%d [%s] | %s %s".formatted(id, username, firstName, lastName);
            }
            case 3 -> {
                result = "[+] ADMIN #%d [%s] | %s %s".formatted(id, username, firstName, lastName);
            }
            default -> {
                result = "";
            }
        }
        return result;
    }

    public int getRank() {
        return rank;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return firstName + " " + lastName;
    }
    public boolean isSignedIn() {
        return signedIn;
    }

    public Map<Integer, String> getDiets() {
        return diets;
    }

    public String[] getDietNames() {
        String[] result = new String[diets.size()];
        int i = 0;
        for (Integer dietId : diets.keySet()) {
            result[i] = diets.get(dietId).toUpperCase();
            i++;
        }
        return result;
    }

    public String listDiets() {
        String result = "";
        String[] diets = getDietNames();
        for (int i=0; i < diets.length - 1; i++) {
            result += diets[i] + ", ";
        }
        return result + diets[diets.length - 1];
    }

    public int[] getDietIDs() {
        int[] result = new int[diets.size()];
        int i = 0;
        for (Integer dietId : diets.keySet()) {
            result[i] = dietId;
            i++;
        }
        return result;
    }
}