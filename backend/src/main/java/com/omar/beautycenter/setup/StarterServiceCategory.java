package com.omar.beautycenter.setup;

/** Fixed starter categories keep the first-run wizard Arabic-first and price-free. */
public enum StarterServiceCategory {
    HAIR("شعر", "Hair"),
    MAKEUP("مكياج", "Makeup"),
    SKIN_CARE("عناية بالبشرة", "Skin care"),
    NAILS("أظافر", "Nails"),
    HAIR_INSTALLATION("تركيب شعر", "Hair installation");

    private final String arabicName;
    private final String englishName;

    StarterServiceCategory(String arabicName, String englishName) {
        this.arabicName = arabicName;
        this.englishName = englishName;
    }

    public String arabicName() {
        return arabicName;
    }

    public String englishName() {
        return englishName;
    }
}
