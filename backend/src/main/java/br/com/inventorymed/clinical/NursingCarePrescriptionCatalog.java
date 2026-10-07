package br.com.inventorymed.clinical;

import java.util.List;

public record NursingCarePrescriptionCatalog(
    List<String> headPositions,
    List<String> repositioningFrequencies,
    List<String> pressureProtection,
    List<String> baths,
    List<String> oralHygiene,
    List<String> skinCare,
    List<String> catheterDressings,
    List<String> acuteWoundCare,
    List<String> complexWoundCoverages,
    List<String> dressingChangeFrequencies,
    List<String> drainCare,
    List<String> airwaySuction,
    List<String> deviceCare,
    List<String> fluidBalance
) {}
