package com.wordweave.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.wordweave.models.RoleModel;
import com.wordweave.models.UserModel;
import com.wordweave.services.RoleService;
import com.wordweave.services.UserService;
import com.wordweave.utils.FormUtils;
import com.wordweave.utils.ImageUtil;
import com.wordweave.utils.PasswordUtil;
import com.wordweave.utils.ValidationUtil;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RegisterController handles user registration requests.
 * It validates form input, processes uploaded profile pictures, hashes passwords,
 * and persists new users into the database.
 */
@MultipartConfig
@WebServlet(asyncSupported = true, urlPatterns = { "/register" })
public class RegisterController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final UserService userService = new UserService();
	private final ImageUtil imageUtil = new ImageUtil();
	private final RoleService roleService = new RoleService();

    public RegisterController() {
        super();
    }
    
    /**
	 * Handles GET requests to show the registration form.
	 * If user is already logged in, redirects to home.
	 */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String username = (String) request.getAttribute("username");

    	if (username != null) {
    		response.sendRedirect(request.getContextPath() + "/");
    		return;
    	}
    	
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/pages/client/register.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Map<String, String> errors = validateRegistrationForm(request);
            if (!errors.isEmpty()) {
                handleError(request, response, errors);
                return;
            }

            UserModel userModel = extractUserModel(request);
            Boolean isAdded = userService.registerUser(userModel);

            if (isAdded == null) {
                errors.put("error", "Our server is under maintenance. Please try again later!");
                handleError(request, response, errors);
            } else if (isAdded) {
            	request.getSession().setAttribute("success", "Account Successfully Created, Please Login.");
                response.sendRedirect("/wordweave/login");
            } else {
                errors.put("error", "Could not register your account. Please try again later!");
                handleError(request, response, errors);
            }
        } catch (Exception e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "An unexpected error occurred. Please try again later!");
            handleError(request, response, errors);
            e.printStackTrace();
        }
    }

    /**
	 * Validates the registration form inputs and returns a map of error messages.
	 */
    private Map<String, String> validateRegistrationForm(HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();

        try {
            String fullname = FormUtils.getFormField(req, "fullname");
            String email = FormUtils.getFormField(req, "email");
            String username = FormUtils.getFormField(req, "username");
            String password = FormUtils.getFormField(req, "password");
            String cPassword = FormUtils.getFormField(req, "cPassword");

            if (ValidationUtil.isNullOrEmpty(fullname)) errors.put("error_fullname", "Fullname is required.");
            if (ValidationUtil.isNullOrEmpty(username)) errors.put("error_username", "Username is required.");
            if (ValidationUtil.isNullOrEmpty(email)) errors.put("error_email", "Email is required.");
            if (ValidationUtil.isNullOrEmpty(password)) errors.put("error_password", "Password is required.");
            if (ValidationUtil.isNullOrEmpty(cPassword)) errors.put("error_cpassword", "Please retype the password.");

            if (!errors.containsKey("error_username") && !ValidationUtil.isAlphanumericStartingWithLetter(username)) {
                errors.put("error_username", "Username must start with a letter and contain only letters and numbers.");
            }

            if (!errors.containsKey("error_email") && !ValidationUtil.isValidEmail(email)) {
                errors.put("error_email", "Invalid email format.");
            }

            if (!errors.containsKey("error_password") && !ValidationUtil.isValidPassword(password)) {
                errors.put("error_password", "Password must be at least 8 characters long, with 1 uppercase letter, 1 number, and 1 symbol.");
            }

            if (!errors.containsKey("error_cpassword") && !ValidationUtil.doPasswordsMatch(password, cPassword)) {
                errors.put("error_cpassword", "Passwords do not match.");
            }
            
            UserModel user = userService.getUserByUsername(username);
            
            if (user != null) {
            	errors.put("error_username", "The user with this username already exists.");
            }

        } catch (Exception e) {
            errors.put("error", "Invalid input.");
        }

        return errors;
    }


    /**
	 * Extracts and builds a UserModel from request parameters and uploaded image.
	 */
    private UserModel extractUserModel(HttpServletRequest req) throws Exception {
        String fullname = FormUtils.getFormField(req, "fullname");
        String email = FormUtils.getFormField(req, "email");
        String username = FormUtils.getFormField(req, "username");
        String password = FormUtils.getFormField(req, "password");

        RoleModel userRole = roleService.getRole("user");
        int roleId = userRole.getRole_id();

        String profilePicture = null;
        password = PasswordUtil.encrypt(username, password);

        boolean isImageUploaded = imageUtil.uploadImage(req, "profile_picture");
        if (isImageUploaded) {
            profilePicture = "/images/" + req.getPart("profile_picture").getSubmittedFileName();
        }

        return new UserModel(fullname, email, username, password, roleId, profilePicture);
    }

    /**
	 * Handles forwarding back to the registration page with error messages and prefilled inputs.
	 */
    private void handleError(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errors)
            throws ServletException, IOException {
        
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }

        req.setAttribute("fullname", FormUtils.getFormField(req, "fullname"));
        req.setAttribute("username", FormUtils.getFormField(req, "username"));
        req.setAttribute("email", FormUtils.getFormField(req, "email"));

        req.getRequestDispatcher("/WEB-INF/pages/client/register.jsp").forward(req, resp);
    }


}
