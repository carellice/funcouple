# FunCouple

Gioco per coppie per Android, in italiano. **Contenuti per adulti (18+).**

App nativa in Kotlin e Jetpack Compose. Funziona offline, non chiede permessi di rete e tutti i dati restano sul dispositivo.

## Cosa c'è dentro

- **Verità o Obbligo** — 240 carte su quattro livelli, modalità Escalation, carte speciali, penitenze
- **Kamasutra** — 40 posizioni illustrate, preferite, traguardi e Maratona a tempo
- **Dadi del Piacere** — dadi 3D: cosa, dove, per quanto e come
- **Ruota del Desiderio** — dodici premi
- **Non ho mai**, **Roulette dei Preliminari**, **Buoni del Piacere**
- **Le vostre carte** — verità e obblighi scritti da voi
- **Preferenze e limiti**, blocco con PIN o biometria, suoni, "luci basse", effetto 3D

## Installazione

Scarica l'APK dall'ultima [release](../../releases/latest) e aprilo sul telefono (Android 8 o successivo).

## Compilare dal codice

Servono Android Studio (o JDK 17+) e l'SDK Android 36.

```bash
./gradlew :app:assembleDebug
```

- `tools/gen_positions.py` genera le posizioni del Kamasutra (`Positions.kt`)
- `tools/gen_sounds.py` genera gli effetti sonori
- `release.command` compila l'APK e lo pubblica nelle Releases

## Licenza

Codice sotto licenza [MIT](LICENSE). I font Playfair Display e Outfit sono distribuiti con licenza SIL Open Font License (testi in `tools/`).
