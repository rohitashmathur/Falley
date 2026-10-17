package com.perfgames.ropelab;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/** Small offline English-key catalog. Unknown copy safely falls back to English. */
public final class FalleyStrings {
    public static final int ENGLISH = 0, HINDI = 1, INDONESIAN = 2;
    private static final Map<String, String[]> TRANSLATIONS = new HashMap<>();

    static {
        add("Land safely to unlock the next stage.", "अगला चरण खोलने के लिए सुरक्षित उतरें।", "Mendarat aman untuk membuka tahap berikutnya.");
        add("STAGE %d / LOCKED", "चरण %d / बंद", "TAHAP %d / TERKUNCI");
        add("Stage %d, %s, locked. Finish the previous stage.", "चरण %d, %s, बंद है। पिछला चरण पूरा करें।", "Tahap %d, %s, terkunci. Selesaikan tahap sebelumnya.");
        add("TOUCH", "टच", "SENTUH");
        add("MOTION", "झुकाव", "GERAKAN");
        add("GUIDE %d/%d", "गाइड %d/%d", "PANDUAN %d/%d");
        add("TILT TO STEER", "झुकाकर दिशा बदलें", "MIRINGKAN UNTUK MENGARAH");
        add("SWIPE TO STEER", "स्वाइप करके दिशा बदलें", "GESER UNTUK MENGARAH");
        add("TWO QUICK JUMPS", "एक बार में दो छलाँगें", "DUA LOMPATAN CEPAT");
        add("READ THE DANGER", "खतरे पहचानें", "KENALI BAHAYA");
        add("Hold the phone comfortably. Tilt left or right to steer. Your hold is centered at each start and resume.",
                "फ़ोन आराम से पकड़ें। दिशा बदलने के लिए बाएँ या दाएँ झुकाएँ। हर शुरुआत और दोबारा जारी करने पर आपकी पकड़ को केंद्र माना जाता है।",
                "Pegang ponsel dengan nyaman. Miringkan ke kiri atau kanan untuk mengarah. Peganganmu dipusatkan saat mulai dan melanjutkan.");
        add("Swipe left or right to steer. Release to stop the swipe. A light air current still nudges you.",
                "दिशा बदलने के लिए बाएँ या दाएँ स्वाइप करें। उँगली हटाने पर स्वाइप रुकता है। हल्की हवा फिर भी आपको धकेलती है।",
                "Geser ke kiri atau kanan untuk mengarah. Lepaskan jari untuk berhenti menggeser. Angin ringan tetap mendorongmu.");
        add("Tap the arena or JUMP to jump. You can still swipe to take over steering briefly.",
                "कूदने के लिए मैदान या कूदें बटन पर टैप करें। कुछ देर दिशा अपने हाथ में लेने के लिए स्वाइप भी कर सकते हैं।",
                "Ketuk arena atau LOMPAT untuk melompat. Kamu juga bisa menggeser untuk mengambil alih arah sejenak.");
        add("Tap the arena or JUMP to jump. Watch the burst and total jump counters.",
                "कूदने के लिए मैदान या कूदें बटन पर टैप करें। एक बार की छलाँगों और कुल बची छलाँगों के अंक देखें।",
                "Ketuk arena atau LOMPAT untuk melompat. Perhatikan jumlah lompatan beruntun dan sisa totalnya.");
        add("Tap up to twice in a burst. Descend below where the burst began to recover. Wait briefly after the last jump.",
                "एक बार में अधिकतम दो छलाँगें लगाएँ। फिर शुरू की ऊँचाई से नीचे उतरने पर छलाँगें लौटती हैं। आखिरी छलाँग के बाद थोड़ा रुकें।",
                "Lompat hingga dua kali beruntun. Turun melewati ketinggian awal untuk pulih. Tunggu sejenak setelah lompatan terakhir.");
        add("Campaign has a total jump budget for each stage. Recovery refills the burst, not the total. Arcade has no total limit.",
                "अभियान के हर चरण में कुल छलाँगें सीमित हैं। ठीक होने पर दो छलाँगों का मौका लौटता है, कुल बजट नहीं। आर्केड में कुल सीमा नहीं है।",
                "Kampanye memiliki jatah total lompatan per tahap. Pemulihan mengisi rangkaian, bukan totalnya. Arkade tidak membatasi total.");
        add("The ceiling and side hazards are lethal. Read warnings and avoid live fire, lasers, lightning and saws.",
                "छत और किनारों के खतरे जानलेवा हैं। चेतावनियाँ देखें और आग, लेज़र, बिजली व चलती आरियों से बचें।",
                "Langit-langit dan bahaya samping mematikan. Baca peringatan dan hindari api, laser, petir, serta gergaji aktif.");
        add("Campaign: land on the target to unlock the next stage. Later missed landings are fatal. Arcade: survive.",
                "अभियान: अगला चरण खोलने के लिए लक्ष्य पर उतरें। आगे के चरणों में लक्ष्य चूकना जानलेवा है। आर्केड: बचे रहें।",
                "Kampanye: mendarat di sasaran untuk membuka tahap berikutnya. Di tahap lanjut, meleset itu fatal. Arkade: bertahan hidup.");
        add("RECOVERY", "फिर तैयार", "PEMULIHAN");
        add("BURST %d/%d", "छलाँगें %d/%d", "RANGKAIAN %d/%d");
        add("TOTAL %d/%d", "कुल %d/%d", "TOTAL %d/%d");
        add("NO TOTAL LIMIT", "कुल सीमा नहीं", "TOTAL TAK TERBATAS");
        add("LEVEL BUDGET USED", "चरण की छलाँगें खत्म", "JATAH TAHAP HABIS");
        add("DESCEND TO RECOVER", "फिर तैयार होने को नीचे उतरें", "TURUN UNTUK PULIH");
        add("READY", "तैयार", "SIAP");
        add("WAIT", "रुकें", "TUNGGU");
        add("Jump. Burst %d of %d. No total limit.", "कूदें। एक बार की छलाँगें %d/%d। कुल सीमा नहीं है।", "Lompat. Rangkaian %d dari %d. Total tidak terbatas.");
        add("Jump. Burst %d of %d. Stage budget %d of %d remaining.", "कूदें। एक बार की छलाँगें %d/%d। चरण के बजट में %d/%d बाकी हैं।", "Lompat. Rangkaian %d dari %d. Jatah tahap tersisa %d dari %d.");
        add("by Perf Games", "Perf Games द्वारा", "oleh Perf Games");
        add("CAMPAIGN / ARCADE SURVIVAL", "अभियान / आर्केड में बचे रहें", "KAMPANYE / BERTAHAN DI ARKADE");
        add("CAMPAIGN", "अभियान", "KAMPANYE");
        add("ARCADE", "आर्केड", "ARKADE");
        add("ARCADE / SKIRMISH", "आर्केड / मुक़ाबला", "ARKADE / PERTEMPURAN");
        add("CHAPTERS", "अध्याय", "BAB");
        add("HOW TO PLAY", "कैसे खेलें", "CARA BERMAIN");
        add("SETTINGS", "सेटिंग्स", "PENGATURAN");
        add("LANGUAGE", "भाषा", "BAHASA");
        add("Language", "भाषा", "Bahasa");
        add("Language selection", "भाषा चुनें", "Pilih bahasa");
        add("%s, selected", "%s, चुना गया", "%s, dipilih");
        add("PREVIOUS", "पिछला", "SEBELUMNYA");
        add("NEXT", "अगला", "BERIKUTNYA");
        add("DONE", "हो गया", "SELESAI");
        add("HOME", "मुख्य पृष्ठ", "BERANDA");
        add("PAUSED", "खेल रुका है", "DIJEDA");
        add("RESUME", "जारी रखें", "LANJUTKAN");
        add("RESTART", "फिर शुरू करें", "MULAI ULANG");
        add("REPLAY", "फिर खेलें", "MAIN LAGI");
        add("TRY AGAIN", "फिर कोशिश करें", "COBA LAGI");
        add("NEXT STAGE", "अगला चरण", "TAHAP BERIKUTNYA");
        add("CHANGE SETUP", "विकल्प बदलें", "UBAH PENGATURAN");
        add("GOT IT", "समझ गया", "MENGERTI");
        add("Back", "वापस", "Kembali");
        add("Restart", "फिर शुरू करें", "Mulai ulang");
        add("Pause", "रोकें", "Jeda");
        add("Survive the hazards. Chase a better run.", "खतरों से बचें। अपना रिकॉर्ड सुधारें।", "Hindari bahaya. Pecahkan rekormu.");
        add("DIFFICULTY", "कठिनाई", "KESULITAN");
        add("CHILL", "आराम", "SANTAI");
        add("STANDARD", "सामान्य", "STANDAR");
        add("BRUTAL", "कठिन", "BERAT");
        add("Fireballs", "आग के गोले", "Bola api");
        add("Top Lasers", "ऊपर की लेज़र", "Laser atas");
        add("Side Lasers", "किनारों की लेज़र", "Laser samping");
        add("Lightning", "बिजली", "Petir");
        add("Buzzsaws", "घूमती आरियाँ", "Gergaji bundar");
        add("Keep at least one hazard enabled.", "कम से कम एक खतरा चालू रखें।", "Aktifkan setidaknya satu bahaya.");
        add("No finish line. New attacks arrive in waves.", "कोई अंत नहीं। हर लहर में नए हमले आते हैं।", "Tanpa garis finis. Serangan baru datang bergelombang.");
        add("START SURVIVAL", "खेल शुरू करें", "MULAI BERTAHAN");
        add("BEST %d / LONGEST %.1fs", "सर्वश्रेष्ठ %d / सबसे लंबा %.1fसे", "TERBAIK %d / TERLAMA %.1fd");
        add("Pick a fall. Every stage is ready to play.", "चरण चुनें। सभी चरण खेलने के लिए खुले हैं।", "Pilih tantangan. Semua tahap siap dimainkan.");
        add("Make motion feel right.", "फ़ोन के झुकाव का असर अपने हिसाब से चुनें।", "Sesuaikan respons gerakanmu.");
        add("Touch controls work without sensors.", "टच नियंत्रण बिना सेंसर के भी काम करते हैं।", "Kontrol sentuh bekerja tanpa sensor.");
        add("Motion steering", "झुकाकर दिशा बदलें", "Kendali gerakan");
        add("MOTION FEEL", "झुकाव का असर", "RESPONS GERAKAN");
        add("LOOSE", "ढीला", "LONGGAR");
        add("BALANCED", "संतुलित", "SEIMBANG");
        add("TIGHT", "कसा हुआ", "KETAT");
        add("Quick response. Small tilts move you more.", "तेज़ असर। थोड़ा झुकाने पर ज़्यादा गति।", "Respons cepat. Sedikit miring, lebih jauh bergerak.");
        add("Calmer response. More tilt, less movement.", "हल्का असर। ज़्यादा झुकाने पर भी कम गति।", "Respons tenang. Lebih miring, gerakan lebih kecil.");
        add("A steady balance of response and control.", "तेज़ असर और नियंत्रण का संतुलन।", "Respons dan kendali yang seimbang.");
        add("No sensor available. Swipe left or right to steer.", "सेंसर नहीं है। दिशा बदलने के लिए दाएँ-बाएँ स्वाइप करें।", "Sensor tidak tersedia. Geser ke kiri atau kanan.");
        add("Motion is off. Swipe left or right to steer.", "झुकाव बंद है। दिशा बदलने के लिए दाएँ-बाएँ स्वाइप करें।", "Gerakan nonaktif. Geser ke kiri atau kanan.");
        add("Waiting for a sensor reading...", "सेंसर की रीडिंग का इंतज़ार है…", "Menunggu pembacaan sensor…");
        add("Your hold centers at every start and resume.", "हर शुरुआत पर आपकी पकड़ को सीधा माना जाता है।", "Posisi pegangan dipusatkan tiap mulai atau lanjut.");
        add("FEEDBACK", "ध्वनि और असर", "UMPAN BALIK");
        add("Sound Effects", "ध्वनि प्रभाव", "Efek suara");
        add("Vibration", "कंपन", "Getaran");
        add("Blood Effects", "खून के प्रभाव", "Efek darah");
        add("%s, unavailable", "%s, उपलब्ध नहीं", "%s, tidak tersedia");
        add("unavailable", "उपलब्ध नहीं", "tidak tersedia");
        add("on", "चालू", "aktif");
        add("off", "बंद", "nonaktif");
        add("Longer falls. One bouncy target.", "लंबी गिरावट। एक उछाल भरा लक्ष्य।", "Jatuh lebih jauh. Satu sasaran memantul.");
        add("TAP TO JUMP", "कूदने के लिए टैप करें", "KETUK UNTUK MELOMPAT");
        add("Tap the arena or the Jump button.", "खेल के मैदान या कूदने के बटन पर टैप करें।", "Ketuk arena atau tombol Lompat.");
        add("STEER AGAINST THE DRIFT", "हवा के बहाव से लड़ें", "LAWAN ARAH HEMBUSAN");
        add("Air currents push you. Swipe against the drift.", "हवा आपको धकेलती है। उसके उलट स्वाइप करें।", "Angin mendorongmu. Geser melawan hembusan.");
        add("LET THE ROPES BOUNCE", "रस्सियों से उछलें", "MEMANTUL DI TALI");
        add("Landing bends your knees, then springs you up.", "उतरते समय घुटने मुड़ते हैं, फिर आप उछलते हैं।", "Saat mendarat, lutut menekuk lalu tubuh memantul.");
        add("LAND SOFTLY OR SURVIVE", "सुरक्षित उतरें या बचे रहें", "MENDARAT ATAU BERTAHAN");
        add("Campaign: land on the target. Arcade: stay alive.", "अभियान: लक्ष्य पर उतरें। आर्केड: बचे रहें।", "Kampanye: darat di sasaran. Arkade: bertahan hidup.");
        add("Buzzsaws warn, then sweep sideways. Move clear.", "आरियाँ चेतावनी देकर आड़ी चलती हैं। दूर हटें।", "Gergaji memberi tanda lalu menyapu. Menjauhlah.");
        add("Hard floor outside the target is dangerous.", "लक्ष्य के बाहर का सख्त फ़र्श खतरनाक है।", "Lantai keras di luar sasaran berbahaya.");
        add("Motion ON? Tilt to steer. Adjust feel in Settings.", "झुकाव चालू है? झुकाकर चलें। असर सेटिंग्स में चुनें।", "Gerakan aktif? Miringkan. Atur respons di Pengaturan.");
        add("DOUBLE JUMP", "दो बार कूदें", "LOMPAT GANDA");
        add("Tap twice in the air. Landing restores both jumps.", "हवा में दो बार टैप करें। उतरने पर दोनों छलाँगें फिर मिलती हैं।", "Ketuk dua kali di udara. Mendarat memulihkan kedua lompatan.");
        add("Tap twice in the air. Land to refill jumps.", "हवा में दो बार टैप करें। उतरने पर छलाँगें फिर मिलती हैं।", "Ketuk dua kali di udara. Mendarat memulihkan lompatan.");
        add("KEEP CLEAR OF THE CEILING", "छत से दूर रहें", "JAUHI LANGIT-LANGIT");
        add("The upper edge is lethal. Jump with room above.", "ऊपरी किनारा जानलेवा है। ऊपर जगह हो तभी कूदें।", "Tepi atas mematikan. Sisakan ruang di atas saat melompat.");
        add("The upper edge is lethal. Leave room above.", "ऊपरी किनारा जानलेवा है। ऊपर जगह छोड़ें।", "Tepi atas mematikan. Sisakan ruang di atas.");
        add("Twelve stages. Six chapters.", "बारह चरण। छह अध्याय।", "Dua belas tahap. Enam bab.");
        add("Jump to move. Land to recharge.", "कूदकर आगे बढ़ें। उतरकर फिर छलाँगें पाएँ।", "Melompat untuk bergerak. Mendarat untuk mengisi ulang.");
        add("FALLEY / ARCADE", "FALLEY / आर्केड", "FALLEY / ARKADE");
        add("FALLEY / STAGE %d", "FALLEY / चरण %d", "FALLEY / TAHAP %d");
        add("SCORE %d", "अंक %d", "SKOR %d");
        add("WEIGHT %.1fx", "वज़न %.1fx", "BOBOT %.1fx");
        add("CUTS %d/%d", "कट %d/%d", "PUTUS %d/%d");
        add("WAVE %d", "लहर %d", "GELOMBANG %d");
        add("STAGE %d", "चरण %d", "TAHAP %d");
        add("STAGE %d / %s", "चरण %d / %s", "TAHAP %d / %s");
        add("STAGE %d / BEST %d / %d LANDINGS", "चरण %d / सर्वश्रेष्ठ %d / %d बार उतरे", "TAHAP %d / TERBAIK %d / %d PENDARATAN");
        add("BEST %d / %d LANDINGS", "सर्वश्रेष्ठ %d / %d बार उतरे", "TERBAIK %d / %d PENDARATAN");
        add("STAGE %d BEST %d", "चरण %d सर्वश्रेष्ठ %d", "TAHAP %d TERBAIK %d");
        add("CHAPTERS %d/%d", "अध्याय %d/%d", "BAB %d/%d");
        add("%dm / LAND %s", "%dमी / %s उतरें", "%dm / DARAT %s");
        add("LEFT", "बाएँ", "KIRI");
        add("RIGHT", "दाएँ", "KANAN");
        add("CENTER", "बीच में", "TENGAH");
        add("%.1fs SURVIVED", "%.1fसे बचे", "BERTAHAN %.1fd");
        add("HOLD COMFORTABLY", "आराम से पकड़ें", "PEGANG DENGAN NYAMAN");
        add("MOTION ON", "झुकाव चालू", "GERAKAN AKTIF");
        add("SWIPE + DRIFT", "स्वाइप + बहाव", "GESER + HEMBUSAN");
        add("JUMP", "कूदें", "LOMPAT");
        add("JUMP ↑", "कूदें ↑", "LOMPAT ↑");
        add("JUMP ...", "कूदें …", "LOMPAT …");
        add("JUMPS %d/%d", "छलाँगें %d/%d", "LOMPATAN %d/%d");
        add("JUMP %d/%d", "कूदें %d/%d", "LOMPAT %d/%d");
        add("JUMP %d/%d ...", "कूदें %d/%d …", "LOMPAT %d/%d …");
        add("Jump %d of %d", "छलाँग %d / %d", "Lompat %d dari %d");
        add("No jumps left. Land to recharge.", "छलाँगें खत्म। फिर पाने के लिए उतरें।", "Lompatan habis. Mendarat untuk mengisi ulang.");
        add("HARD FLOOR", "सख्त फ़र्श", "LANTAI KERAS");
        add("LAND HERE", "यहाँ उतरें", "DARAT DI SINI");
        add("LANDED!", "उतर गए!", "MENDARAT!");
        add("RUN ENDED", "दौर समाप्त", "SESI BERAKHIR");
        add("FALL ENDED", "गिरावट समाप्त", "JATUH BERAKHIR");
        add("A soft landing. A good bounce.", "नरम उतरान। बढ़िया उछाल।", "Mendarat lembut. Pantulan mantap.");
        add("HIT BY %s", "%s से टकराए", "TERKENA %s");
        add("%.1fs / WAVE %d", "%.1fसे / लहर %d", "%.1fd / GELOMBANG %d");
        add("%.1fs / %d ROPES CUT", "%.1fसे / %d रस्सियाँ कटीं", "%.1fd / %d TALI PUTUS");
        add("ARCADE BEST %d", "आर्केड सर्वश्रेष्ठ %d", "TERBAIK ARKADE %d");
        add("LASER", "लेज़र", "LASER");
        add("SPIKES", "काँटे", "DURI");
        add("FIRE", "आग", "API");
        add("TOP LASER", "ऊपरी लेज़र", "LASER ATAS");
        add("SIDE LASER", "किनारे की लेज़र", "LASER SAMPING");
        add("LIGHTNING", "बिजली", "PETIR");
        add("THUNDER", "बिजली", "PETIR");
        add("BUZZSAW", "घूमती आरी", "GERGAJI BUNDAR");
        add("HARD LANDING", "ज़ोर की टक्कर", "PENDARATAN KERAS");
        add("CEILING", "छत", "LANGIT-LANGIT");
        add("First light", "पहली किरण", "Cahaya pertama");
        add("Tight beam", "तंग किरण", "Sinar sempit");
        add("Sharp turn", "तीखा मोड़", "Tikungan tajam");
        add("Needle run", "सुइयों के बीच", "Lintasan jarum");
        add("Warm welcome", "गरम स्वागत", "Sambutan hangat");
        add("Sky on fire", "जलता आसमान", "Langit membara");
        add("Crosswind", "आड़ी हवा", "Angin samping");
        add("Storm window", "तूफ़ान में रास्ता", "Celah badai");
        add("Slow blades", "धीमे ब्लेड", "Bilah lambat");
        add("Factory line", "कारखाने की कतार", "Jalur pabrik");
        add("Ember crossing", "अंगारों का रास्ता", "Lintasan bara");
        add("Final descent", "आखिरी गिरावट", "Turunan terakhir");
        add("Laser descent", "लेज़र की गिरावट", "Turunan laser");
        add("Spike canyon", "काँटों की घाटी", "Ngarai berduri");
        add("Fire fall", "आग की गिरावट", "Terjunan api");
        add("Storm passage", "तूफ़ानी रास्ता", "Lorong badai");
        add("Blade foundry", "ब्लेड की भट्ठी", "Pabrik bilah");
        add("Last descent", "अंतिम गिरावट", "Turunan akhir");
        add("Campaign. Stage %d, %s", "अभियान। चरण %d, %s", "Kampanye. Tahap %d, %s");
        add("Stage %d, %s. Best %d. %d landings", "चरण %d, %s। सर्वश्रेष्ठ %d। %d बार उतरे", "Tahap %d, %s. Terbaik %d. %d pendaratan");
        add("Falley home", "Falley मुख्य पृष्ठ", "Beranda Falley");
        add("Settings", "सेटिंग्स", "Pengaturan");
        add("How to play", "कैसे खेलें", "Cara bermain");
        add("Paused", "खेल रुका है", "Dijeda");
        add("Result", "नतीजा", "Hasil");
        add("Chapters", "अध्याय", "Bab");
        add("Arcade setup", "आर्केड के विकल्प", "Pengaturan Arkade");
        add("Falley by Perf Games", "Perf Games द्वारा Falley", "Falley oleh Perf Games");
        add("Falley gameplay", "Falley का खेल", "Permainan Falley");
        add("Run ended. Hit by %s. Score %d. %.1f seconds.", "दौर समाप्त। %s से टकराए। अंक %d। %.1f सेकंड।", "Sesi berakhir. Terkena %s. Skor %d. %.1f detik.");
        add("Landed. Score %d. %.1f seconds.", "उतर गए। अंक %d। %.1f सेकंड।", "Mendarat. Skor %d. %.1f detik.");
        add("Tap the arena or Jump button to jump. Swipe left or right to steer against air currents. Ropes bend, then bounce you. In Campaign, land on the bouncy target. In Arcade, survive as long as you can. Hazards warn before they activate. Buzzsaws sweep sideways. Move clear. Hard floor outside the target is dangerous. Enable motion steering in Settings to tilt instead of swipe. Loose, Balanced and Tight adjust motion response.",
                "कूदने के लिए मैदान या कूदने के बटन पर टैप करें। हवा के बहाव के खिलाफ दिशा बदलने के लिए दाएँ-बाएँ स्वाइप करें। रस्सियाँ झुककर आपको उछालती हैं। अभियान में उछाल भरे लक्ष्य पर उतरें। आर्केड में जितनी देर हो सके बचे रहें। खतरे सक्रिय होने से पहले चेतावनी देते हैं। घूमती आरियाँ आड़ी चलती हैं। दूर हटें। लक्ष्य के बाहर सख्त फ़र्श खतरनाक है। स्वाइप के बदले फ़ोन झुकाकर दिशा बदलने के लिए सेटिंग्स में झुकाव नियंत्रण चालू करें। ढीला, संतुलित और कसा हुआ विकल्प झुकाव का असर बदलते हैं।",
                "Ketuk arena atau tombol Lompat untuk melompat. Geser ke kiri atau kanan melawan arus angin. Tali melengkung lalu memantulkanmu. Dalam Kampanye, mendaratlah di sasaran yang memantul. Dalam Arkade, bertahanlah selama mungkin. Bahaya memberi peringatan sebelum aktif. Gergaji menyapu ke samping. Menjauhlah. Lantai keras di luar sasaran berbahaya. Aktifkan kendali gerakan di Pengaturan untuk mengarahkan dengan memiringkan perangkat. Longgar, Seimbang, dan Ketat mengatur respons gerakan.");
        add("PRIVACY POLICY", "गोपनीयता नीति", "KEBIJAKAN PRIVASI");
        add("END OF POLICY", "नीति समाप्त", "AKHIR KEBIJAKAN");
        add("SWIPE UP TO READ MORE", "और पढ़ने के लिए ऊपर स्वाइप करें", "GESER KE ATAS UNTUK MEMBACA");
        add("BACK TO SETTINGS", "सेटिंग्स पर वापस जाएँ", "KEMBALI KE PENGATURAN");
        add("Falley Privacy Policy", "Falley की गोपनीयता नीति", "Kebijakan Privasi Falley");
        add("Publisher: Perf Games", "प्रकाशक: Perf Games", "Penerbit: Perf Games");
        add("Effective: 16 October 2026", "लागू: 16 अक्टूबर 2026", "Berlaku: 16 Oktober 2026");
        add("An offline game", "एक ऑफ़लाइन खेल", "Game luring");
        add("Falley works offline and has no Internet permission. It has no accounts, advertising, analytics or tracking SDKs. Perf Games does not collect, receive, sell or share personal information through this version of Falley.",
                "Falley ऑफ़लाइन चलता है और इसके पास इंटरनेट इस्तेमाल करने की अनुमति नहीं है। इसमें खाते, विज्ञापन, विश्लेषण या ट्रैकिंग SDK नहीं हैं। Falley के इस संस्करण के ज़रिए Perf Games निजी जानकारी इकट्ठा नहीं करता, प्राप्त नहीं करता, बेचता नहीं और साझा नहीं करता।",
                "Falley bekerja secara luring dan tidak memiliki izin Internet. Game ini tidak memiliki akun, iklan, analitik, atau SDK pelacakan. Perf Games tidak mengumpulkan, menerima, menjual, atau membagikan informasi pribadi melalui versi Falley ini.");
        add("Motion controls", "झुकाव से नियंत्रण", "Kontrol gerakan");
        add("If you enable motion steering, accelerometer and gyroscope readings are processed on your device to control the game. These readings are temporary: Falley does not save them or send them anywhere. Touch controls work with motion steering off.",
                "झुकाव से दिशा बदलने का विकल्प चालू करने पर, खेल को नियंत्रित करने के लिए एक्सेलेरोमीटर और जायरोस्कोप की रीडिंग आपके डिवाइस पर ही संसाधित होती हैं। ये रीडिंग अस्थायी हैं: Falley इन्हें सहेजता नहीं और कहीं भेजता नहीं। झुकाव नियंत्रण बंद होने पर भी टच नियंत्रण काम करते हैं।",
                "Jika kendali gerakan diaktifkan, pembacaan akselerometer dan giroskop diproses di perangkatmu untuk mengendalikan game. Pembacaan ini bersifat sementara: Falley tidak menyimpan atau mengirimnya ke mana pun. Kontrol sentuh tetap bekerja saat kendali gerakan dinonaktifkan.");
        add("Data on your device", "आपके डिवाइस पर डेटा", "Data di perangkatmu");
        add("Falley saves your settings, selected stage, scores, run counts and best times in the app's private storage on your device. Sound effects and vibration are produced locally. No gameplay data is sent to Perf Games or another service.",
                "Falley आपकी सेटिंग्स, चुना हुआ चरण, अंक, खेले गए दौरों की संख्या और सबसे अच्छे समय आपके डिवाइस पर ऐप के निजी स्टोरेज में सहेजता है। ध्वनि प्रभाव और कंपन डिवाइस पर ही उत्पन्न होते हैं। खेल का कोई डेटा Perf Games या किसी अन्य सेवा को नहीं भेजा जाता।",
                "Falley menyimpan pengaturan, tahap yang dipilih, skor, jumlah sesi, dan waktu terbaik di penyimpanan pribadi aplikasi pada perangkatmu. Efek suara dan getaran dihasilkan secara lokal. Tidak ada data permainan yang dikirim ke Perf Games atau layanan lain.");
        add("Deleting your data", "अपना डेटा मिटाना", "Menghapus datamu");
        add("To erase your saved settings and records, clear Falley's app storage in Android Settings or uninstall the app. This cannot be undone. Android app backup is disabled for Falley.",
                "अपनी सहेजी गई सेटिंग्स और रिकॉर्ड मिटाने के लिए Android की सेटिंग्स में Falley का ऐप स्टोरेज साफ़ करें या ऐप अनइंस्टॉल करें। इसे वापस नहीं किया जा सकता। Falley के लिए Android ऐप बैकअप बंद है।",
                "Untuk menghapus pengaturan dan rekor tersimpan, hapus penyimpanan aplikasi Falley di Pengaturan Android atau uninstal aplikasi. Tindakan ini tidak dapat dibatalkan. Pencadangan aplikasi Android dinonaktifkan untuk Falley.");
        add("Your choices", "आपके विकल्प", "Pilihanmu");
        add("You can turn motion steering, sound effects, vibration and blood effects on or off in Settings. Falley does not ask for camera, microphone, location or contacts access.",
                "सेटिंग्स में आप झुकाव नियंत्रण, ध्वनि प्रभाव, कंपन और खून के प्रभाव चालू या बंद कर सकते हैं। Falley कैमरा, माइक्रोफ़ोन, लोकेशन या संपर्कों तक पहुँचने की अनुमति नहीं माँगता।",
                "Kamu dapat mengaktifkan atau menonaktifkan kendali gerakan, efek suara, getaran, dan efek darah di Pengaturan. Falley tidak meminta akses kamera, mikrofon, lokasi, atau kontak.");
        add("About this policy", "इस नीति के बारे में", "Tentang kebijakan ini");
        add("This policy describes the current offline version of Falley, published by Perf Games. If the app's data practices change, its policy will be updated before those changes are introduced.",
                "यह नीति Perf Games द्वारा प्रकाशित Falley के मौजूदा ऑफ़लाइन संस्करण का वर्णन करती है। यदि ऐप के डेटा इस्तेमाल के तरीके बदलते हैं, तो उन बदलावों को लागू करने से पहले इसकी नीति अपडेट की जाएगी।",
                "Kebijakan ini menjelaskan versi luring Falley saat ini yang diterbitkan oleh Perf Games. Jika praktik data aplikasi berubah, kebijakan ini akan diperbarui sebelum perubahan tersebut diterapkan.");
    }

    private FalleyStrings() { }

    public static int sanitize(int language) {
        return language >= ENGLISH && language <= INDONESIAN ? language : ENGLISH;
    }

    public static String languageName(int index) {
        switch (sanitize(index)) {
            case HINDI: return "हिन्दी";
            case INDONESIAN: return "Bahasa Indonesia";
            default: return "English";
        }
    }

    public static String text(int language, String english, Object... args) {
        if (english == null) return "";
        String value = english;
        int selected = sanitize(language);
        String[] entry = selected == ENGLISH ? null : TRANSLATIONS.get(english);
        if (entry != null) value = entry[selected - 1];
        if (args == null || args.length == 0) return value;
        try {
            return String.format(Locale.US, value, args);
        } catch (IllegalFormatException invalidTranslation) {
            // A malformed translation must never break gameplay or navigation.
            try { return String.format(Locale.US, english, args); }
            catch (IllegalFormatException invalidEnglish) { return english; }
        }
    }

    private static void add(String english, String hindi, String indonesian) {
        TRANSLATIONS.put(english, new String[] {hindi, indonesian});
    }
}
