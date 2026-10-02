package br.com.inventorymed.auth;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/select-hospital")
    public HospitalSelectionResponse selectHospital(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody SelectHospitalRequest request
    ) {
        return authService.selectHospital(UUID.fromString(jwt.getSubject()), request.hospitalId());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(
            UUID.fromString(jwt.getSubject()),
            selectedHospitalId(jwt)
        );
    }

    private UUID selectedHospitalId(Jwt jwt) {
        String hospitalId = jwt.getClaimAsString("hospital_id");
        return hospitalId == null ? null : UUID.fromString(hospitalId);
    }
}
