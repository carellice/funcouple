package com.funcouple.app

/**
 * Categorie che la coppia può escludere. Ogni categoria riconosce i testi che la riguardano
 * tramite parole chiave, così vale per le carte, i premi, i dadi e le carte scritte a mano.
 */
enum class Limit(val title: String, val hint: String, val icon: FcIcon, pattern: String) {
    BLINDFOLD("Bende", "Giochi a occhi bendati", FcIcon.BLINDFOLD, "bend"),
    BONDAGE(
        "Legature",
        "Polsi legati, mani bloccate, manette",
        FcIcon.LOCK,
        """\bleg(a|hi|are|asse|ati|ate)\b|polsi|manette|mani legate|mani bloccate|mani dietro la schiena|tieni ferme le mani""",
    ),
    IMPACT("Sculacciate e morsi", "Sculacciate, morsi, graffi, succhiotti", FcIcon.FANGS, "sculacc|mors[oi]|mordicch|graff|succhiott"),
    ORAL(
        "Sesso orale",
        "Sfide di piacere con la bocca",
        FcIcon.TONGUE,
        """orale|\b69\b|sul tuo viso|con la bocca (dove|finché|e fermati|per due)|usa (solo )?la bocca (finché|per portare)""" +
            """|svegliarti con la bocca|piacere con la bocca|sola bocca|la sua bocca dove|punta della lingua, nel punto""",
    ),
    TOYS("Giocattoli", "Sfide che prevedono giocattoli erotici", FcIcon.SPARKLE, "giocattol|sexy shop"),
    FOOD("Ghiaccio e cibo", "Cubetti di ghiaccio, dolci, cibo sul corpo", FcIcon.SNOWFLAKE, "ghiaccio|qualcosa di dolce|qualcosa di buono|cibo"),
    SOLO(
        "Toccarsi davanti all'altro",
        "Darsi piacere mentre il partner guarda",
        FcIcon.HAND,
        "datti piacere|ti dai piacere|toccarsi|ti tocchi|ti toccavi|accarezzarsi",
    ),
    ;

    private val regex = Regex(pattern, RegexOption.IGNORE_CASE)

    fun matches(text: String) = regex.containsMatchIn(text)
}
