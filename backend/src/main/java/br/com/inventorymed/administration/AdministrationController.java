package br.com.inventorymed.administration;

import br.com.inventorymed.security.InventoryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administration")
@PreAuthorize("hasRole('ADMIN_SISTEMA')")
public class AdministrationController {

    private final HospitalAdministrationService hospitalService;
    private final UserAdministrationService userService;

    public AdministrationController(
        HospitalAdministrationService hospitalService,
        UserAdministrationService userService
    ) {
        this.hospitalService = hospitalService;
        this.userService = userService;
    }

    @GetMapping("/hospitals")
    public List<HospitalSummaryResponse> hospitals() {
        return hospitalService.list();
    }

    @PostMapping("/hospitals")
    public ResponseEntity<HospitalSummaryResponse> createHospital(
        @Valid @RequestBody HospitalCreateRequest createRequest,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        HospitalSummaryResponse created = hospitalService.create(
            createRequest,
            principal,
            request.getRemoteAddr(),
            request.getHeader("User-Agent")
        );
        return ResponseEntity.created(
            URI.create("/api/v1/administration/hospitals/" + created.id())
        ).body(created);
    }

    @PostMapping("/hospitals/{hospitalId}/provision")
    public HospitalSummaryResponse retryHospitalProvisioning(
        @PathVariable java.util.UUID hospitalId,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return hospitalService.retryProvisioning(
            hospitalId,
            principal,
            request.getRemoteAddr(),
            request.getHeader("User-Agent")
        );
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return userService.list();
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(
        @Valid @RequestBody UserCreateRequest createRequest,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        AdminUserResponse created = userService.create(
            createRequest,
            principal,
            request.getRemoteAddr(),
            request.getHeader("User-Agent")
        );
        return ResponseEntity.created(
            URI.create("/api/v1/administration/users/" + created.id())
        ).body(created);
    }
}
