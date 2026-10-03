package br.com.inventorymed.auth;

import br.com.inventorymed.security.InventoryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return new CsrfResponse(csrfToken.getHeaderName());
    }

    @PostMapping("/login")
    public LoginResponse login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return authService.login(loginRequest, request, response);
    }

    @PostMapping("/select-hospital")
    public HospitalSelectionResponse selectHospital(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        @Valid @RequestBody SelectHospitalRequest selectHospitalRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return authService.selectHospital(
            principal,
            selectHospitalRequest.hospitalId(),
            request,
            response
        );
    }

    @GetMapping("/me")
    public MeResponse me(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session
    ) {
        return authService.me(principal, session);
    }
}
