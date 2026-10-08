#!/usr/bin/env python3
"""Sorgente delle posizioni del Kamasutra.

Genera app/src/main/java/com/funcouple/app/Positions.kt e, con --preview <cartella>,
un foglio SVG con tutte le pose per controllarle a colpo d'occhio.

Le pose vivono in uno spazio 100x100 (y verso il basso, pavimento a y=84). Ogni figura è
una lista di punti "x,y": testa, collo, bacino, gomito, mano, ginocchio1, piede1,
ginocchio2, piede2 (gamba 1 = quella più vicina a chi guarda). Un "!" iniziale inverte
il lato verso cui guarda la figura, se quello dedotto in automatico è sbagliato.
A è la figura rosa (chi accoglie), B quella viola (chi penetra).
"""
import math
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/java/com/funcouple/app/Positions.kt")

# --- Pose di base riutilizzate ---------------------------------------------------------------
SUP = "14,78 22,78 48,79 34,72 44,66 64,74 82,80 66,78 84,82"          # supino, gambe distese
SUPK = "16,78 24,78 48,79 32,70 42,66 60,62 70,78 64,66 76,80"         # supino, ginocchia piegate
SUP_UP = "14,78 22,78 46,78 30,72 38,68 52,60 62,46 54,62 64,48"       # supino, gambe in alto
TOP = "22,60 29,64 52,72 31,74 33,83 68,78 84,82 66,80 82,84"          # disteso sopra
RIDE = "45,37 46,46 48,70 40,58 34,70 36,80 54,83 38,82 56,84"         # a cavalcioni, verso sinistra
RIDE_R = "53,37 52,46 48,70 58,58 62,70 60,80 42,83 62,82 44,84"       # a cavalcioni, verso destra
ALL4 = "18,58 25,61 50,62 25,72 24,83 52,82 70,83 55,82 73,84"         # a quattro zampe
KNEEL = "55,29 56,38 60,62 54,50 50,60 62,82 80,83 65,82 83,84"        # in ginocchio, busto eretto
KNEEL_F = "53,40 56,48 60,70 48,60 42,76 62,82 80,83 65,82 83,84"      # in ginocchio, piegato avanti
STAND = "56,18 56,27 56,52 52,40 48,52 54,68 54,84 59,68 60,84"        # in piedi, verso sinistra
CHAIR = ["48,63,26,4", "71,34,4,30", "50,67,3,17", "69,67,3,17"]
SEATED = "64,27 64,36 64,60 58,46 50,50 46,62 46,82 48,64 48,83"       # seduto sulla sedia
BED = ["6,62,48,22"]

POSITIONS = [
    dict(id="missionario", name="Il Missionario", tag="Il grande classico, occhi negli occhi", d=1, i=5, x=2,
         desc="A si sdraia sulla schiena con le ginocchia piegate. B si distende sopra, sostenendosi sugli avambracci, petto contro petto. Il contatto visivo e i baci fanno tutto il resto.",
         tip="Un cuscino sotto il bacino di A cambia l'angolo e rende tutto molto più intenso.",
         a=SUPK, b=TOP, front="b"),
    dict(id="amazzone", name="L'Amazzone", tag="Chi sta sopra detta il ritmo", d=1, i=4, x=4,
         desc="B è sdraiato sulla schiena. A si mette a cavalcioni sul suo bacino, in ginocchio e con il busto eretto, le mani appoggiate sul petto di B, e controlla profondità e velocità.",
         tip="Alternate movimenti circolari lenti a spinte più decise: l'attesa è metà del piacere.",
         a=RIDE, b=SUP),
    dict(id="cucchiaio", name="Il Cucchiaio", tag="Lento, pigro e dolcissimo", d=1, i=5, x=2,
         desc="Entrambi sdraiati sul fianco, rivolti nella stessa direzione. B abbraccia A da dietro, i corpi aderiscono dalla testa ai piedi e i movimenti sono lenti e profondi.",
         tip="Perfetta al risveglio. La mano libera di B può esplorare tutto il corpo di A.",
         a="18,74 25,76 48,78 32,82 40,80 62,72 74,82 64,75 77,84",
         b="14,64 21,67 44,70 34,72 44,76 58,65 70,75 60,68 73,77"),
    dict(id="pecorina", name="La Pecorina", tag="Istinto puro", d=1, i=2, x=5,
         desc="A si mette a quattro zampe, con la schiena leggermente inarcata. B si inginocchia dietro, le afferra i fianchi e guida il movimento.",
         tip="Se A appoggia petto e avambracci sul letto, l'angolo diventa ancora più profondo.",
         a=ALL4, b=KNEEL, front="b"),
    dict(id="loto", name="Il Loto", tag="Abbraccio totale, respiro all'unisono", d=2, i=5, x=3,
         desc="B si siede a gambe incrociate. A si siede sul suo grembo, di fronte, avvolgendo gambe e braccia intorno al suo corpo. Ci si muove dondolando insieme.",
         tip="Sincronizzate il respiro e guardatevi negli occhi: è la posizione tantrica per eccellenza.",
         a="59,36 58,45 54,70 49,52 41,49 40,65 31,74 42,69 32,79",
         b="42,41 42,50 44,76 50,58 58,54 60,78 50,83 58,82 40,84"),
    dict(id="amazzone_rovesciata", name="L'Amazzone Rovesciata", tag="Uno spettacolo da non perdere", d=2, i=2, x=5,
         desc="B è sdraiato sulla schiena. A si mette a cavalcioni dandogli le spalle, appoggia le mani sulle sue cosce e si muove avanti e indietro.",
         tip="B può sollevare leggermente le ginocchia per offrire ad A un appoggio in più.",
         a=RIDE_R, b=SUP),
    dict(id="incudine", name="L'Incudine", tag="Profonda e travolgente", d=2, i=3, x=5,
         desc="A è sdraiata sulla schiena e solleva le gambe fino ad appoggiarle sulle spalle di B, che è in ginocchio e si piega in avanti sostenendosi sulle mani.",
         tip="Andate per gradi: più B si piega in avanti, più la penetrazione è profonda.",
         a=SUP_UP, b=KNEEL_F),
    dict(id="trono", name="Il Trono", tag="Seduti, vicinissimi", d=1, i=3, x=3,
         desc="B si siede su una sedia robusta. A si siede sul suo grembo dandogli le spalle, con i piedi a terra, e si muove su e giù mentre B la stringe per la vita.",
         tip="Le mani di B sono libere: è il momento ideale per carezze e baci sul collo.",
         a="49,25 50,34 54,58 48,46 46,58 38,60 36,80 40,62 39,82", b=SEATED, props=CHAIR),
    dict(id="abbraccio_sospeso", name="L'Abbraccio Sospeso", tag="Passione senza gravità", d=3, i=4, x=5,
         desc="B è in piedi e solleva A sorreggendola sotto le cosce. A avvolge le gambe intorno ai suoi fianchi e le braccia intorno al suo collo.",
         tip="Appoggiate la schiena di A a una parete: meno fatica, più resistenza.",
         a="61,13 60,22 58,46 51,24 41,27 43,45 34,54 44,49 35,59",
         b="44,16 44,25 46,50 52,40 58,48 44,67 44,84 49,67 50,84"),
    dict(id="ponte", name="Il Ponte", tag="Per coppie acrobatiche", d=3, i=2, x=4,
         desc="B si solleva a ponte, appoggiato su mani e piedi con il ventre verso l'alto. A si mette a cavalcioni in piedi sopra il suo bacino e si abbassa quanto basta.",
         tip="Serve forza nelle braccia: provatela per poco tempo, o con un pouf sotto la schiena di B.",
         a="47,13 48,22 50,46 42,32 36,42 46,65 44,83 54,65 56,83",
         b="22,66 28,62 50,54 26,72 24,83 64,62 68,83 66,64 71,84"),
    dict(id="carriola", name="La Carriola", tag="Divertente quanto impegnativa", d=3, i=2, x=4,
         desc="A appoggia le mani a terra mentre B, in piedi, la solleva per le cosce. A stringe le gambe intorno ai fianchi di B.",
         tip="Più facile se A appoggia gli avambracci sul bordo del letto invece che a terra.",
         a="15,60 22,62 46,55 19,72 18,83 60,50 72,56 61,54 73,60", b=STAND, front="b"),
    dict(id="sessantanove", name="Il 69", tag="Dare e ricevere, insieme", d=2, i=4, x=4,
         desc="B è sdraiato sulla schiena. A si posiziona sopra, in direzione opposta, con le ginocchia ai lati della sua testa: ognuno dedica la bocca al piacere dell'altro.",
         tip="Provatela anche sdraiati sul fianco: più comoda e si resiste molto più a lungo.",
         a="68,66 60,65 36,63 64,74 72,80 22,78 8,80 24,80 10,82",
         b="26,78 34,78 60,79 42,71 44,64 74,68 84,80 76,71 87,82", front="b"),
    dict(id="sfinge", name="La Sfinge", tag="Pelle contro pelle", d=1, i=4, x=3,
         desc="A si sdraia a pancia in giù, sollevando appena il busto sugli avambracci. B si distende sopra la sua schiena, con le gambe all'esterno delle sue.",
         tip="Un cuscino sotto il bacino di A e baci sulla nuca: brividi garantiti.",
         a="16,64 22,68 48,78 22,80 12,82 66,81 84,82 67,83 85,84",
         b="22,48 28,54 50,68 34,66 36,80 66,76 84,79 68,78 86,81", front="b"),
    dict(id="forbici", name="Le Forbici", tag="Intreccio perfetto", d=2, i=3, x=3,
         desc="Sdraiati in direzioni opposte, i due partner intrecciano le gambe come lame di forbice fino a far aderire i bacini. Ci si muove ondeggiando lentamente.",
         tip="Tenetevi per mano o per le caviglie per darvi il ritmo a vicenda.",
         a="10,74 18,75 42,78 26,82 34,83 58,68 72,56 60,80 78,82",
         b="90,74 82,75 58,78 74,82 66,83 42,68 28,56 40,80 22,82"),
    dict(id="farfalla", name="La Farfalla", tag="Sul bordo del letto", d=2, i=3, x=5,
         desc="A si sdraia sulla schiena con il bacino sul bordo del letto e appoggia le gambe sulle spalle di B, che resta in piedi e le sostiene i fianchi.",
         tip="Regolate l'altezza con un cuscino: quando i bacini sono allineati è perfetta.",
         a="14,56 22,57 48,58 30,52 38,50 56,44 62,30 58,47 64,33",
         b="64,20 63,29 60,54 56,42 52,52 59,69 58,84 63,69 64,84", props=BED),
    dict(id="muro", name="Contro il Muro", tag="Quando non si può aspettare", d=2, i=3, x=5,
         desc="A è in piedi, leggermente piegata in avanti con le mani appoggiate alla parete. B si posiziona dietro, in piedi, e la stringe per i fianchi.",
         tip="Se c'è differenza di altezza, A può salire su un gradino o divaricare un po' le gambe.",
         a="28,24 31,32 42,54 24,34 18,28 42,70 40,84 46,70 46,84",
         b="44,16 45,25 52,50 46,40 42,50 52,67 52,84 56,67 58,84", front="b", props=["12,6,4,80"]),
    dict(id="rana", name="La Rana", tag="Accovacciata, padrona del gioco", d=2, i=3, x=5,
         desc="B è sdraiato sulla schiena. A si accovaccia sopra di lui con i piedi ben piantati ai lati dei suoi fianchi e le mani sul suo petto, e molleggia su e giù.",
         tip="È faticosa per le cosce: B può sostenere A sotto i glutei per aiutarla a tenere il ritmo.",
         a="45,34 46,43 50,66 40,54 34,68 38,60 42,80 40,57 45,79", b=SUP),
    dict(id="abbraccio_amazzone", name="L'Amazzone Distesa", tag="Sopra, ma cuore a cuore", d=1, i=5, x=3,
         desc="B è sdraiato sulla schiena. A si distende su di lui, petto contro petto, con le ginocchia ai lati dei suoi fianchi, e si muove scivolando avanti e indietro.",
         tip="Movimenti piccoli e lenti: qui conta lo sfregamento, non la profondità.",
         a="22,64 29,67 52,70 26,73 18,70 62,80 78,77 64,82 80,80", b=SUP),
    dict(id="gatto", name="Il Gatto", tag="Schiena inarcata, fianchi in alto", d=1, i=2, x=5,
         desc="A è in ginocchio con il petto e le braccia distesi sul letto e i fianchi sollevati. B si inginocchia dietro di lei e la tiene per i fianchi.",
         tip="Un cuscino sotto il petto di A rende la posizione comoda anche a lungo.",
         a="18,74 25,75 50,60 18,82 10,82 52,82 70,83 55,82 73,84", b=KNEEL, front="b"),
    dict(id="inchino", name="L'Inchino", tag="In piedi, senza appoggi", d=2, i=2, x=5,
         desc="A è in piedi e si piega in avanti fino a toccare le caviglie o il pavimento. B resta in piedi dietro di lei e la sostiene saldamente per i fianchi.",
         tip="A può appoggiare le mani su uno sgabello basso: più equilibrio, meno tensione sulle gambe.",
         a="18,60 26,56 48,48 24,68 26,80 48,66 47,84 51,66 51,84",
         b="59,14 59,23 59,49 55,38 50,48 58,67 58,84 62,67 63,84", front="b"),
    dict(id="tavolo", name="Il Tavolo", tag="La cucina non è solo per cucinare", d=2, i=4, x=4,
         desc="A si siede sul bordo di un tavolo, appoggiandosi all'indietro su una mano. B resta in piedi di fronte a lei, che gli avvolge le gambe intorno ai fianchi.",
         tip="L'altezza giusta è quella in cui i bacini sono allineati: provate anche con il piano della cucina.",
         a="40,21 42,30 48,53 36,42 32,54 62,48 72,56 63,52 73,60",
         b="64,16 63,25 60,50 56,38 52,46 60,67 60,84 64,67 65,84",
         props=["20,56,34,4", "24,60,3,24", "47,60,3,24"]),
    dict(id="scrivania", name="La Scrivania", tag="Urgente e irresistibile", d=1, i=2, x=5,
         desc="A si piega in avanti e distende il busto su un tavolo o una scrivania, con i piedi a terra. B resta in piedi dietro di lei.",
         tip="Un asciugamano piegato sul bordo evita che lo spigolo dia fastidio.",
         a="21,42 28,47 52,50 20,51 12,51 52,67 50,84 55,67 55,84",
         b="62,16 62,25 62,50 58,38 54,48 61,67 61,84 65,67 66,84", front="b",
         props=["8,54,40,4", "12,58,3,26", "42,58,3,26"]),
    dict(id="dondolo", name="Il Dondolo", tag="Faccia a faccia sulla sedia", d=1, i=5, x=3,
         desc="B si siede su una sedia senza braccioli. A si siede a cavalcioni su di lui, guardandolo, con le braccia intorno al suo collo, e dondola avanti e indietro.",
         tip="Se A arriva con i piedi a terra può darsi la spinta e controllare tutto il movimento.",
         a="53,24 53,33 54,57 59,34 66,33 67,61 72,76 68,63 74,78",
         b="64,27 64,36 64,60 58,48 52,44 46,62 46,82 48,64 48,83", props=CHAIR),
    dict(id="sdraio", name="La Sdraio", tag="Rilassati, ma non troppo", d=2, i=2, x=4,
         desc="B si siede a terra con le gambe distese e il busto inclinato all'indietro, appoggiato sulle mani. A si siede sul suo grembo dandogli le spalle, con i piedi a terra.",
         tip="A può appoggiare le mani sulle ginocchia di B per spingersi e variare l'angolo.",
         a="56,37 56,46 54,70 50,58 42,68 40,62 34,80 42,64 37,82",
         b="74,48 70,56 58,78 76,68 80,81 40,76 22,80 40,79 22,83"),
    dict(id="poltrona", name="La Poltrona", tag="Il divano come alleato", d=2, i=2, x=5,
         desc="A si inginocchia sul divano o su una poltrona, aggrappandosi allo schienale. B resta in piedi dietro di lei.",
         tip="Lo schienale dà ad A un appoggio perfetto per spingere all'indietro e dettare il ritmo.",
         a="21,19 26,26 38,44 22,34 16,37 34,62 50,61 36,63 52,62",
         b="52,10 51,19 48,44 42,32 38,42 50,64 50,84 54,64 55,84", front="b",
         props=["12,36,6,48", "18,64,34,6", "18,70,34,14"]),
    dict(id="candela", name="La Candela", tag="Sottosopra", d=3, i=2, x=4,
         desc="A si sdraia e solleva gambe e bacino verso l'alto, in appoggio sulle spalle, sostenendosi la schiena con le mani. B si inginocchia davanti a lei e la sorregge per le gambe.",
         tip="Tenetela per poco: è una posizione da provare per gioco, non una maratona.",
         a="34,80 40,76 50,52 48,80 50,66 52,36 52,20 55,37 56,22",
         b="66,30 67,39 69,62 62,46 57,32 71,82 89,83 74,82 92,84"),
    dict(id="scivolata", name="La Scivolata", tag="Stretti dalla testa ai piedi", d=1, i=5, x=3,
         desc="A è sdraiata sulla schiena con le gambe distese e unite. B si distende sopra di lei, con le gambe all'esterno delle sue, e si muove scivolando verso l'alto e verso il basso.",
         tip="Tenere le gambe strette aumenta l'attrito e lo sfregamento: sensazioni molto intense per entrambi.",
         a="14,78 22,78 48,79 32,72 40,66 66,79 84,80 66,81 84,82",
         b="20,62 27,65 50,71 30,74 30,83 68,73 86,75 68,75 86,77", front="b"),
    dict(id="ballerino", name="Il Passo di Danza", tag="In piedi, uno contro l'altra", d=2, i=4, x=4,
         desc="In piedi, faccia a faccia. A solleva una gamba e la avvolge intorno al fianco di B, che la sostiene sotto la coscia mentre lei gli si aggrappa al collo.",
         tip="Con la schiena di A contro una parete diventa molto più stabile.",
         a="58,18 57,27 54,51 50,28 42,28 42,52 36,64 56,68 57,84",
         b="42,16 42,25 44,50 50,38 48,52 42,67 42,84 47,67 48,84"),
    dict(id="spaccata", name="La Spaccata", tag="Una gamba su, una giù", d=2, i=3, x=4,
         desc="A è sdraiata sulla schiena: una gamba resta distesa, l'altra si appoggia sulla spalla di B, che è in ginocchio davanti a lei con il busto eretto.",
         tip="Cambiate gamba a metà: l'angolo e le sensazioni cambiano completamente.",
         a="14,78 22,78 46,79 30,72 38,68 54,62 60,46 62,78 80,80",
         b="56,31 56,40 58,64 54,50 56,57 60,82 78,83 63,82 81,84"),
    dict(id="granchio", name="Il Granchio", tag="Guardarsi mentre succede", d=2, i=3, x=3,
         desc="Seduti uno di fronte all'altra, entrambi inclinati all'indietro e appoggiati sulle mani. A avvicina il bacino a quello di B e passa le gambe sopra le sue.",
         tip="Ci si muove poco e lentamente: godetevi la vista e lasciate salire la tensione.",
         a="78,42 74,50 54,72 78,64 82,80 40,62 28,72 40,66 26,78",
         b="22,44 26,52 46,76 22,66 18,80 62,70 78,78 63,74 79,82"),
    dict(id="slitta", name="La Slitta", tag="A pancia in giù, completamente rilassata", d=1, i=3, x=4,
         desc="A è distesa a pancia in giù con le gambe unite. B si inginocchia a cavalcioni delle sue cosce, con il busto eretto e le mani sulla sua schiena.",
         tip="Un massaggio alla schiena prima di cominciare trasforma questa posizione in un rituale.",
         a="13,72 21,75 48,78 20,82 10,82 66,80 84,81 67,82 85,83",
         b="51,33 52,42 56,66 46,54 42,68 50,81 68,83 52,82 70,84", front="b"),
    dict(id="cascata", name="La Cascata", tag="Abbandonarsi all'indietro", d=3, i=3, x=5,
         desc="B è seduto su una sedia. A è seduta a cavalcioni su di lui, di fronte, e si lascia andare all'indietro fino a sfiorare il pavimento con le mani, mentre B la tiene saldamente per i fianchi.",
         tip="Mettete un cuscino a terra e risalite lentamente: il sangue alla testa amplifica le sensazioni.",
         a="27,72 34,66 54,57 30,76 26,83 66,61 72,74 67,63 74,76",
         b="64,27 64,36 64,60 58,48 50,56 46,62 46,82 48,64 48,83", props=CHAIR),
    dict(id="cavalluccio", name="Il Cavalluccio", tag="In ginocchio, incollati", d=2, i=3, x=4,
         desc="B è in ginocchio, seduto sui talloni. A si inginocchia davanti a lui dandogli le spalle e si siede sul suo grembo, con la schiena contro il suo petto.",
         tip="B ha le mani libere per accarezzare A ovunque mentre lei si muove.",
         a="49,33 50,42 52,66 44,54 40,66 36,78 52,83 38,80 54,84",
         b="60,37 60,46 60,70 54,56 48,62 44,78 62,82 45,80 64,84"),
    dict(id="preghiera", name="L'Unione in Ginocchio", tag="Alla pari, vicinissimi", d=2, i=5, x=3,
         desc="Entrambi in ginocchio, uno di fronte all'altra, con il busto eretto e stretti in un abbraccio. A allarga leggermente le ginocchia per accogliere B.",
         tip="Più adatta a baci e carezze che al ritmo: usatela per rallentare e ritrovarvi.",
         a="60,30 59,39 55,62 52,42 44,40 56,82 74,83 59,82 77,84",
         b="40,30 41,39 44,62 50,48 56,52 44,82 26,83 41,82 23,84"),
    dict(id="mezzaluna", name="La Mezzaluna", tag="Il cucchiaio che osa di più", d=2, i=4, x=4,
         desc="Sdraiati sul fianco come nel Cucchiaio, con B dietro. A solleva la gamba superiore verso l'alto e B la sostiene con la mano.",
         tip="La gamba sollevata lascia spazio alle mani di entrambi: approfittatene.",
         a="18,74 25,76 48,78 32,82 40,80 58,62 66,47 64,76 78,84",
         b="14,64 21,67 44,70 38,66 52,62 58,66 70,76 60,69 73,78"),
    dict(id="trono_regina", name="Il Trono della Regina", tag="Lei comodamente seduta, lui ai suoi piedi", d=1, i=4, x=4,
         desc="A si siede sul bordo di una sedia o del letto con le gambe aperte. B si inginocchia davanti a lei e le dedica tutta l'attenzione della sua bocca.",
         tip="A può guidare B con le mani tra i capelli: lento e costante vince sempre.",
         a="64,27 64,36 64,60 58,46 50,52 46,60 44,80 48,63 47,82",
         b="47,51 40,56 28,70 38,66 46,64 30,82 12,83 33,82 15,84", front="b", props=CHAIR),
    dict(id="bacio_ginocchio", name="Il Bacio in Ginocchio", tag="Adorazione", d=1, i=3, x=4,
         desc="A resta in piedi, magari appoggiata a una parete. B si inginocchia davanti a lei e usa bocca e mani per darle piacere.",
         tip="Funziona benissimo anche a ruoli invertiti. Un cuscino sotto le ginocchia aiuta a durare.",
         a="40,16 40,25 42,50 46,38 52,44 41,67 40,84 45,67 45,84",
         b="52,48 57,54 62,68 52,60 46,60 60,82 78,83 63,82 81,84", front="b"),
    dict(id="banchetto", name="Il Banchetto", tag="Solo per il suo piacere", d=1, i=4, x=4,
         desc="A è sdraiata sulla schiena con le ginocchia piegate e aperte. B si distende a pancia in giù tra le sue gambe, con le braccia sotto le sue cosce.",
         tip="Un cuscino sotto il bacino di A migliora l'angolo e risparmia il collo di B.",
         a="8,76 16,77 40,79 24,70 34,66 50,62 58,78 54,64 64,80",
         b="50,72 57,75 80,80 54,81 46,75 93,81 92,66 94,82 96,68", front="b"),
    dict(id="pressa", name="La Pressa", tag="Intensità massima", d=3, i=4, x=5,
         desc="A è sdraiata sulla schiena e porta le ginocchia verso il petto. B si posiziona sopra di lei, sostenendosi sulle braccia tese, con le gambe di A appoggiate contro il suo petto.",
         tip="È la più profonda di tutte: cominciate piano e parlate molto.",
         a="14,78 22,78 46,76 30,70 40,64 36,60 46,46 38,62 49,48",
         b="30,46 34,54 56,66 28,70 26,83 70,76 84,84 72,78 86,84"),
    dict(id="altare", name="L'Altare", tag="Sollevata, offerta", d=2, i=3, x=5,
         desc="B è in ginocchio, seduto sui talloni. A è sdraiata sulla schiena davanti a lui con il bacino sollevato e appoggiato sulle sue cosce, le gambe intorno ai suoi fianchi.",
         tip="B ha entrambe le mani libere e una vista completa: usatele entrambe.",
         a="14,80 22,79 46,66 30,82 40,82 60,58 70,66 61,62 71,70",
         b="57,35 57,44 58,68 52,54 46,62 44,80 62,83 45,82 64,84"),
]

# --- Geometria delle figure (deve restare identica a quella di Kamasutra.kt) -----------------

def sub(a, b): return (a[0] - b[0], a[1] - b[1])
def add(a, b): return (a[0] + b[0], a[1] + b[1])
def mul(a, k): return (a[0] * k, a[1] * k)
def dot(a, b): return a[0] * b[0] + a[1] * b[1]
def mid(a, b): return ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
def norm(a):
    n = math.hypot(*a)
    return (a[0] / n, a[1] / n) if n > 1e-4 else (0.0, 0.0)
def perp(a): return (-a[1], a[0])


def figure(spec, bun):
    """Restituisce tre gruppi di capsule (p0, r0, p1, r1): arti lontani, corpo, braccio."""
    flip = spec.startswith("!")
    pts = [tuple(map(float, p.split(","))) for p in spec.lstrip("!").split()]
    head, neck, hip, elbow, hand, k1, f1, k2, f2 = pts[:9]
    spine = norm(sub(hip, neck))
    side = perp(spine)
    shoulder = add(neck, mul(sub(hip, neck), 0.08))
    waist = add(neck, mul(sub(hip, neck), 0.62))
    # Il davanti del corpo è il lato verso cui sporgono le ginocchia (e, in subordine, la mano).
    score = dot(side, sub(k1, mid(hip, f1))) + dot(side, sub(k2, mid(hip, f2))) + 0.3 * dot(side, sub(hand, shoulder))
    front = mul(side, (1 if score >= 0 else -1) * (-1 if flip else 1))
    up = norm(sub(head, neck))
    face = sub(front, mul(up, dot(front, up)))
    face = norm(face) if math.hypot(*face) > 0.3 else front

    def foot(knee, ankle):
        shin = norm(sub(ankle, knee))
        d = perp(shin)
        bend = dot(d, sub(knee, mid(hip, ankle)))
        if abs(bend) < 0.8:
            bend = dot(d, front)
        d = mul(d, 1 if bend >= 0 else -1)
        return (ankle, 1.9, add(ankle, mul(d, 4.2)), 1.4)

    far = [(hip, 3.9, k2, 2.9), (k2, 2.9, f2, 1.9), foot(k2, f2)]
    body = [
        (head, 2.3, shoulder, 2.5),
        (shoulder, 4.9, waist, 3.9), (waist, 3.9, hip, 4.6),
        (hip, 4.2, k1, 3.0), (k1, 3.0, f1, 2.0), foot(k1, f1),
        (head, 6.0, head, 6.0),
        (add(head, mul(face, 5.8)), 1.4, add(head, mul(face, 5.8)), 1.4),
    ]
    if bun:
        c = add(add(head, mul(face, -5.6)), mul(up, 1.8))
        body.append((c, 3.0, c, 3.0))
    arm = [(shoulder, 2.7, elbow, 2.2), (elbow, 2.2, hand, 1.6), (hand, 1.9, hand, 1.9)]
    return far, body, arm


# --- Anteprima SVG ---------------------------------------------------------------------------

def svg_cap(cap, color, grow=0.0):
    (x0, y0), r0, (x1, y1), r1 = cap
    r0 += grow
    r1 += grow
    out = f'<circle cx="{x0:.2f}" cy="{y0:.2f}" r="{r0:.2f}" fill="{color}"/>'
    if (x0, y0) != (x1, y1):
        n = perp(norm((x1 - x0, y1 - y0)))
        q = [(x0 + n[0] * r0, y0 + n[1] * r0), (x1 + n[0] * r1, y1 + n[1] * r1),
             (x1 - n[0] * r1, y1 - n[1] * r1), (x0 - n[0] * r0, y0 - n[1] * r0)]
        out += f'<polygon points="{" ".join(f"{x:.2f},{y:.2f}" for x, y in q)}" fill="{color}"/>'
        out += f'<circle cx="{x1:.2f}" cy="{y1:.2f}" r="{r1:.2f}" fill="{color}"/>'
    return out


def svg_figure(spec, main, dark, bun):
    out = ""
    for group, color in zip(figure(spec, bun), (dark, main, main)):
        out += "".join(svg_cap(c, "#1a0818", 1.1) for c in group)
        out += "".join(svg_cap(c, color) for c in group)
    return out


def preview(folder, cols=4, per_page=12):
    """Scrive un foglio SVG (e la sua miniatura PNG) ogni [per_page] posizioni."""
    for page in range(0, len(POSITIONS), per_page):
        cells = []
        chunk = POSITIONS[page:page + per_page]
        for n, p in enumerate(chunk):
            props = ""
            for r in p.get("props", []):
                x, y, w, h = r.split(",")
                props += f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="1.5" fill="#ffffff33"/>'
            fa = svg_figure(p["a"], "#ff5c9a", "#b3245a", True)
            fb = svg_figure(p["b"], "#9a6bff", "#4b1fa8", False)
            body = props + (fa + fb if p.get("front") == "b" else fb + fa)
            x, y = (n % cols) * 110, (n // cols) * 118
            cells.append(
                f'<g transform="translate({x + 5},{y + 5})"><rect width="100" height="100" rx="8" fill="#2a0a2b"/>'
                f'<line x1="6" y1="88" x2="94" y2="88" stroke="#ffffff22"/>{body}'
                f'<text x="50" y="109" font-size="6.5" fill="#fff" text-anchor="middle" font-family="Helvetica">'
                f'{page + n + 1}. {p["name"]}</text></g>')
        w = cols * 110
        path = os.path.join(folder, f"poses{page // per_page + 1}.svg")
        with open(path, "w") as f:
            f.write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {w}" width="1600" height="1600">'
                    f'<rect width="{w}" height="{w}" fill="#0d0410"/>{"".join(cells)}</svg>')
        subprocess.run(["qlmanage", "-t", "-s", "1600", "-o", folder, path], capture_output=True)


# --- Generazione Kotlin ----------------------------------------------------------------------

HEADER = '''package com.funcouple.app

// FILE GENERATO da tools/gen_positions.py: modifica lo script, non questo file.

/**
 * Le pose sono disegnate in uno spazio 100x100 (y verso il basso, pavimento a y=84).
 * Ogni figura è una lista di punti "x,y": testa, collo, bacino, gomito, mano, ginocchio1,
 * piede1, ginocchio2, piede2; un "!" iniziale inverte il lato verso cui guarda.
 * I props sono rettangoli "x,y,larghezza,altezza". A è la figura rosa, B quella viola.
 */
data class Position(
    val id: String,
    val name: String,
    val tagline: String,
    val difficulty: Int,
    val intimacy: Int,
    val intensity: Int,
    val description: String,
    val tip: String,
    val a: String,
    val b: String,
    val aFront: Boolean = true,
    val props: List<String> = emptyList(),
)

val difficultyNames = listOf("", "Facile", "Media", "Difficile")

val positions = listOf(
'''


def kotlin():
    out = HEADER
    for p in POSITIONS:
        out += "    Position(\n"
        for key, value in (("id", p["id"]), ("name", p["name"]), ("tagline", p["tag"])):
            out += f'        {key} = "{value}",\n'
        out += f'        difficulty = {p["d"]},\n        intimacy = {p["i"]},\n        intensity = {p["x"]},\n'
        out += f'        description = "{p["desc"]}",\n        tip = "{p["tip"]}",\n'
        out += f'        a = "{p["a"]}",\n        b = "{p["b"]}",\n'
        if p.get("front") == "b":
            out += "        aFront = false,\n"
        if p.get("props"):
            out += "        props = listOf(" + ", ".join(f'"{r}"' for r in p["props"]) + "),\n"
        out += "    ),\n"
    return out + ")\n"


if __name__ == "__main__":
    assert len({p["id"] for p in POSITIONS}) == len(POSITIONS), "id duplicati"
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        preview(sys.argv[2])
    with open(OUT, "w") as f:
        f.write(kotlin())
    print(len(POSITIONS), "posizioni")
