package authSerivce.util;

import authSerivce.model.UserInfoDto;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    public static String validate(UserInfoDto dto) {

        if (dto == null) {
            return "Request body is missing";
        }

        // -------- Username --------
        String username = dto.getUsername();
        if (username == null || username.isBlank()) {
            return "Username is required";
        }

        username = username.trim();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username must be 3–20 characters (letters, numbers, underscore only)";
        }

        // -------- Email --------
        String email = dto.getEmail();
        if (email == null || email.isBlank()) {
            return "Email is required";
        }

        email = email.trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Invalid email format";
        }

        // -------- Password --------
        String password = dto.getPassword();

        if (password == null || password.isBlank()) {
            return "Password is required";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }

        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }

        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }

        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number";
        }

        if (!password.matches(".*[@#$%^&+=!].*")) {
            return "Password must contain at least one special character";
        }

        return null;
    }
}
