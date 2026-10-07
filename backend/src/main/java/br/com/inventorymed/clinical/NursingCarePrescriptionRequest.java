package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NursingCarePrescriptionRequest(
    @Valid Positioning positioning,
    @Valid HygieneSkin hygieneSkin,
    @Valid DressingsDrains dressingsDrains,
    @Valid Procedures procedures
) {
    public record Positioning(
        @Size(max = 180) String headPosition,
        @Size(max = 120) String repositioningFrequency,
        @Size(max = 3) List<@Size(max = 160) String> pressureProtection
    ) {}

    public record HygieneSkin(
        @Size(max = 160) String bath,
        @Size(max = 180) String oralHygiene,
        @Size(max = 3) List<@Size(max = 160) String> skinCare
    ) {}

    public record DressingsDrains(
        @Size(max = 240) String catheterDressing,
        @Size(max = 240) String acuteWoundCare,
        @Size(max = 180) String complexWoundCoverage,
        @Size(max = 100) String dressingChangeFrequency,
        @Size(max = 3) List<@Size(max = 180) String> drainCare
    ) {}

    public record Procedures(
        @Size(max = 180) String airwaySuction,
        @Size(max = 4) List<@Size(max = 220) String> deviceCare,
        @Size(max = 180) String fluidBalance
    ) {}
}
