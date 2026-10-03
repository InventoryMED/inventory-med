package br.com.inventorymed.hospitals;

import br.com.inventorymed.security.InventoryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administration/hospitals/{hospitalId}/structure")
@PreAuthorize("hasRole('ADMIN_SISTEMA')")
public class HospitalStructureAdministrationController {

    private final HospitalStructureService service;

    public HospitalStructureAdministrationController(HospitalStructureService service) {
        this.service = service;
    }

    @GetMapping
    public HospitalStructureResponse structure(@PathVariable UUID hospitalId) {
        return service.structure(hospitalId);
    }

    @PostMapping("/care-units")
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalStructureResponse.CareUnitResponse createCareUnit(
        @PathVariable UUID hospitalId,
        @Valid @RequestBody CareUnitSaveRequest body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.createCareUnit(hospitalId, body, principal, ip(request), agent(request));
    }

    @PutMapping("/care-units/{careUnitId}")
    public HospitalStructureResponse.CareUnitResponse updateCareUnit(
        @PathVariable UUID hospitalId,
        @PathVariable UUID careUnitId,
        @Valid @RequestBody CareUnitSaveRequest body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.updateCareUnit(hospitalId, careUnitId, body, principal, ip(request), agent(request));
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalStructureResponse.RoomResponse createRoom(
        @PathVariable UUID hospitalId,
        @Valid @RequestBody RoomSaveRequest body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.createRoom(hospitalId, body, principal, ip(request), agent(request));
    }

    @PutMapping("/rooms/{roomId}")
    public HospitalStructureResponse.RoomResponse updateRoom(
        @PathVariable UUID hospitalId,
        @PathVariable UUID roomId,
        @Valid @RequestBody RoomSaveRequest body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.updateRoom(hospitalId, roomId, body, principal, ip(request), agent(request));
    }

    @PostMapping("/beds")
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalStructureResponse.BedResponse createBed(
        @PathVariable UUID hospitalId,
        @Valid @RequestBody BedSaveRequest body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.createBed(hospitalId, body, principal, ip(request), agent(request));
    }

    @PutMapping("/beds/{bedId}")
    public HospitalStructureResponse.BedResponse updateBed(
        @PathVariable UUID hospitalId,
        @PathVariable UUID bedId,
        @Valid @RequestBody BedSaveRequest body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.updateBed(hospitalId, bedId, body, principal, ip(request), agent(request));
    }

    private String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String agent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
