package br.com.inventorymed.hospitals;

import java.util.List;
import java.util.UUID;

public record HospitalStructureResponse(List<CareUnitResponse> careUnits) {
    public record CareUnitResponse(
        UUID id,
        String name,
        String code,
        int displayOrder,
        boolean active,
        List<RoomResponse> rooms
    ) {}

    public record RoomResponse(
        UUID id,
        UUID careUnitId,
        String name,
        String code,
        String floorName,
        int displayOrder,
        boolean active,
        List<BedResponse> beds
    ) {}

    public record BedResponse(
        UUID id,
        UUID roomId,
        String code,
        BedStatus status,
        int displayOrder,
        boolean active
    ) {}
}
