package us.pente.graph.auth;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.UserNameAuthorizationContext;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public class Login implements ParameterizedHandler {

    private final UserRepository userRepository;

    @Inject
    public Login(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            HttpServletRequest request,
            @Required(name = "username") String username,
            @Required(name = "password") String password
    ) throws Exception {
        username = username.trim();
        password = password.trim();

        if (isValidPenteOrgUser(username, password)) {
            User user = findOrCreateUser(username);
            UserNameAuthorizationContext authorizationContext =
                    new UserNameAuthorizationContext(username, AuthenticationHandler.getRemoteAddr(request));
            userRepository.updateUser(user, authorizationContext);
            CurrentUser.set(request, user.getUserId(), user.getUsername());
            JSONObject json = new JSONObject();
            json.put("status", "OK");
            return json;
        } else {
            throw new VisalloAccessDeniedException("", null, null);
        }
    }

    private User findOrCreateUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            String randomPassword = UserRepository.createRandomPassword();
            user = userRepository.findOrAddUser(username, username, null, randomPassword);
        }
        return user;
    }

    private boolean isValidPenteOrgUser(String username, String password) {
        try {
            List<String> cookieHeaders = SiteLogin.login(username, password);
            return cookieHeaders.stream().anyMatch(header -> header.contains(String.format("name2=\"%s\"",username)));
        } catch (IOException e) {
            return false;
        }
    }
}
