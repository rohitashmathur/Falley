package com.perfgames.ropelab;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/** Offline expansion copy; existing UI copy remains in FalleyStrings. */
public final class ExpansionStrings {
    private static final Map<String,String[]> STRINGS=new HashMap<>();
    static {
        add("CHECK DEVICE DATE", "डिवाइस की तारीख जाँचें", "PERIKSA TANGGAL PERANGKAT");
        add("CAMPAIGN / STAGE %d", "अभियान / चरण %d", "KAMPANYE / TAHAP %d");
        add("DAILY DROP", "रोज़ की गिरावट", "DAILY DROP");
        add("Daily Drop", "रोज़ की गिरावट", "Daily Drop");
        add("WARDROBE", "रूप-सज्जा", "PENAMPILAN");
        add("%d / %d STARS", "%d / %d सितारे", "%d / %d BINTANG");
        add("CHAPTER %d / %d", "अध्याय %d / %d", "BAB %d / %d");
        add("CHAPTER %d", "अध्याय %d", "BAB %d");
        add("STAGE %d", "चरण %d", "TAHAP %d");
        add("Finish stages. Collect stars. Beat the finale.", "चरण पूरे करें। सितारे पाएँ। अंतिम मशीन को पार करें।", "Selesaikan tahap. Kumpulkan bintang. Taklukkan final.");
        add("CHAPTER FINALE", "अध्याय का अंतिम पड़ाव", "FINAL BAB");
        add("PAR %d JUMPS", "लक्ष्य: %d छलाँगें", "PAR %d LOMPATAN");
        add("REWARD: %s", "इनाम: %s", "HADIAH: %s");
        add("NEW COURSE AT 00:00 UTC", "नया रास्ता: 00:00 UTC", "RUTE BARU PUKUL 00.00 UTC");
        add("SAME COURSE", "सबके लिए एक रास्ता", "RUTE YANG SAMA");
        add("FRESH EACH DAY", "हर दिन नया", "BARU SETIAP HARI");
        add("UNLIMITED TRIES", "असीमित कोशिशें", "COBA TANPA BATAS");
        add("NO REVIVES", "दोबारा जीवन नहीं", "TANPA HIDUP KEMBALI");
        add("One seeded course for everyone. Tilt or swipe. Land safely to complete today's drop.", "सबके लिए एक ही रोज़ का रास्ता। झुकाएँ या स्वाइप करें। आज की चुनौती पूरी करने के लिए सुरक्षित उतरें।", "Satu rute harian untuk semua. Miringkan atau geser. Mendarat aman untuk menuntaskan tantangan hari ini.");
        add("STREAK %d / BEST %d", "लगातार %d / सर्वश्रेष्ठ %d", "RENTETAN %d / TERBAIK %d");
        add("TODAY: %d WINS / %d TRIES", "आज: %d जीत / %d कोशिशें", "HARI INI: %d MENANG / %d COBA");
        add("BEST SCORE %d", "सर्वश्रेष्ठ स्कोर %d", "SKOR TERBAIK %d");
        add("CLOCK CHANGED: PRACTICE ONLY", "घड़ी बदली: केवल अभ्यास", "JAM BERUBAH: LATIHAN SAJA");
        add("START DAILY DROP", "रोज़ की चुनौती शुरू करें", "MULAI DAILY DROP");
        add("NEXT DESCENT", "अगली गिरावट", "TURUN BERIKUTNYA");
        add("PRACTICE / NO STARS", "अभ्यास / सितारे नहीं", "LATIHAN / TANPA BINTANG");
        add("MIRROR PRACTICE", "उलटी दिशा अभ्यास", "LATIHAN CERMIN");
        add("START DROP", "गिरावट शुरू करें", "MULAI TURUN");
        add("Finale: read the machine's warnings, take the safe gap, then land on the target.", "अंतिम पड़ाव: मशीन की चेतावनी पढ़ें, सुरक्षित जगह से निकलें और निशाने पर उतरें।", "Final: baca peringatan mesin, lewati celah aman, lalu mendarat di sasaran.");
        add("The landing target moves. Follow its centre.", "उतरने का निशाना चलता है। उसके केंद्र की ओर जाएँ।", "Sasaran pendaratan bergerak. Ikuti pusatnya.");
        add("The target pulses smaller. Time your landing.", "निशाना बीच-बीच में छोटा होता है। सही समय पर उतरें।", "Sasaran menyusut berkala. Atur waktu pendaratan.");
        add("Marked wind zones push sideways. Countersteer gently.", "चिह्नित हवा वाले हिस्से धकेलते हैं। हल्के से विपरीत दिशा में मोड़ें।", "Zona angin bertanda mendorong ke samping. Lawan arah dengan lembut.");
        add("Cyan pads bounce you. Leave room above.", "नीले पैड आपको उछालते हैं। ऊपर जगह रखें।", "Bantalan sian memantulkanmu. Sisakan ruang di atas.");
        add("Amber anchors crack under load. Move before they break.", "पीले सहारे भार से चटकते हैं। टूटने से पहले हट जाएँ।", "Jangkar kuning retak saat terbebani. Bergerak sebelum patah.");
        add("Collect the hourglass to slow danger briefly.", "खतरे को कुछ देर धीमा करने के लिए रेतघड़ी लें।", "Ambil jam pasir untuk memperlambat bahaya sejenak.");
        add("LOW GRAVITY", "कम गुरुत्वाकर्षण", "GRAVITASI RENDAH");
        add("WIND GUSTS", "हवा के झोंके", "HEMBUSAN ANGIN");
        add("HEAVY ROPE LOAD", "रस्सी पर अधिक भार", "BEBAN TALI BERAT");
        add("ONE JUMP PER BURST", "एक बार में एक छलाँग", "SATU LOMPATAN PER RANGKAIAN");
        add("READ THE WARNINGS", "चेतावनियाँ देखें", "BACA PERINGATAN");
        add("Read the warnings. Let ropes bend and carry you down.", "चेतावनियाँ देखें। रस्सियों को झुकने दें और नीचे बढ़ें।", "Baca peringatan. Biarkan tali melengkung dan membawamu turun.");
        add("AVATARS", "अवतार", "AVATAR");
        add("ROPES", "रस्सियाँ", "TALI");
        add("TRAILS", "पदचिह्न", "JEJAK");
        add("LANDING", "उतरना", "PENDARATAN");
        add("SELECTED", "चुना गया", "DIPILIH");
        add("UNLOCKED", "खुला", "TERBUKA");
        add("LOCKED", "बंद", "TERKUNCI");
        add("A new avatar for every chapter finale.", "हर अध्याय पूरा करने पर नया अवतार।", "Avatar baru setelah setiap final bab.");
        add("%d STARS", "%d सितारे", "%d BINTANG");
        add("Human", "इंसान", "Manusia");
        add("Demon", "दानव", "Iblis");
        add("Angel", "देवदूत", "Malaikat");
        add("Military", "सैनिक", "Tentara");
        add("Astronaut", "अंतरिक्ष यात्री", "Astronaut");
        add("Robot", "रोबोट", "Robot");
        add("Shadow", "परछाईं", "Bayangan");
        add("Original", "मूल", "Asli");
        add("Mint", "मिंट", "Mint");
        add("Violet", "बैंगनी", "Ungu");
        add("Gold", "सुनहरा", "Emas");
        add("None", "कोई नहीं", "Tanpa");
        add("Spark", "चिंगारी", "Percikan");
        add("Comet", "धूमकेतु", "Komet");
        add("Prism", "प्रिज़्म", "Prisma");
        add("Ripple", "लहर", "Riak");
        add("Bloom", "खिलना", "Mekar");
        add("Starburst", "तारों की चमक", "Ledakan Bintang");
        add("Nova", "नोवा", "Nova");
        add("WIND", "हवा", "ANGIN");
        add("BOUNCE", "उछाल", "PANTULAN");
        add("SLOW TIME", "धीमा समय", "WAKTU LAMBAT");
        add("ANCHOR BREAKING", "सहारा टूट रहा है", "JANGKAR RETAK");
        add("BOSS WARNING", "मशीन की चेतावनी", "PERINGATAN BOS");
        add("SAFE GAP", "सुरक्षित जगह", "CELAH AMAN");
        add("BOSS ACTIVE", "मशीन चालू", "BOS AKTIF");
        add("ROPE TRICKS", "रस्सी के करतब", "TRIK TALI");
        add("READ THE AIR", "हवा पहचानें", "BACA ANGIN");
        add("MASTER THE DROP", "गिरावट में महारत", "KUASAI TURUN");
        add("Cyan pads bounce you without spending a jump. Amber anchors crack under load and then break.", "नीले पैड बिना छलाँग खर्च किए उछालते हैं। पीले सहारे भार से चटककर टूट जाते हैं।", "Bantalan sian memantulkanmu tanpa memakai lompatan. Jangkar kuning retak karena beban lalu patah.");
        add("Wind arrows show the push. Moving targets need timing. The hourglass briefly slows the world.", "तीर हवा की दिशा दिखाते हैं। चलते निशानों के लिए सही समय चाहिए। रेतघड़ी कुछ देर दुनिया को धीमा करती है।", "Panah menunjukkan dorongan angin. Sasaran bergerak perlu waktu tepat. Jam pasir memperlambat dunia sejenak.");
        add("Earn a star for finishing, meeting the jump par and landing in the golden centre. Stars combine across wins.", "पूरा करने, तय छलाँग सीमा के भीतर रहने और सुनहरे केंद्र में उतरने पर सितारे पाएँ। अलग-अलग जीत के सितारे जुड़ते हैं।", "Raih bintang untuk selesai, memenuhi par lompatan, dan mendarat di pusat emas. Bintang digabung dari beberapa kemenangan.");
        add("Updated: 24 October 2026", "अपडेट: 24 अक्टूबर 2026", "Diperbarui: 24 Oktober 2026");
        add("Sharing a result", "नतीजा साझा करना", "Membagikan hasil");
        add("Daily Drop records, stars, streaks and appearance choices stay on your device. If you choose Share Result, Falley creates an image with the course date, generator version, score, time, jumps, avatar and control mode. Android lets you choose where to share it. Nothing is shared automatically. The app you choose handles that shared image under its own policy.",
            "Daily Drop के रिकॉर्ड, सितारे, लगातार खेले दिनों और रूप के विकल्प आपके डिवाइस पर रहते हैं। नतीजा साझा करने पर Falley तारीख, संस्करण, अंक, समय, छलाँगें, अवतार और नियंत्रण तरीके वाली तस्वीर बनाता है। Android में आप चुनते हैं कि इसे कहाँ भेजना है। कुछ भी अपने आप साझा नहीं होता। चुना हुआ ऐप अपनी नीति के अनुसार उस तस्वीर को संभालता है।",
            "Rekor Daily Drop, bintang, rentetan hari dan pilihan tampilan tetap di perangkatmu. Jika memilih Bagikan Hasil, Falley membuat gambar berisi tanggal, versi generator, skor, waktu, lompatan, avatar dan mode kontrol. Android membiarkanmu memilih tempat berbagi. Tidak ada yang dibagikan otomatis. Aplikasi pilihanmu menangani gambar tersebut berdasarkan kebijakannya sendiri.");
        add("Leave room below the lethal ceiling. One-jump stages allow one jump per burst, then descent restores it.", "जानलेवा छत से दूरी रखें। एक-छलाँग चरण में एक बार में एक छलाँग मिलती है, नीचे आने पर लौटती है।", "Jauhi langit-langit mematikan. Tahap satu lompatan memberi satu lompatan per rangkaian; turun untuk memulihkannya.");
        add("Each chapter ends with a machine finale. Follow its warnings and safe gap. Mirror Practice is optional and awards no stars.", "हर अध्याय का अंत मशीन से होता है। चेतावनी और सुरक्षित जगह देखें। उलटी दिशा का अभ्यास वैकल्पिक है और सितारे नहीं देता।", "Setiap bab berakhir dengan mesin. Ikuti peringatan dan celah aman. Latihan Cermin opsional dan tidak memberi bintang.");
        add("Daily Drop refreshes at 00:00 UTC. Complete chapters for avatars; collect stars for ropes, trails and landing effects.", "रोज़ की चुनौती 00:00 UTC पर बदलती है। अध्यायों से अवतार और सितारों से रस्सियाँ, पदचिह्न व उतरने के प्रभाव पाएँ।", "Daily Drop diperbarui pukul 00.00 UTC. Selesaikan bab untuk avatar; kumpulkan bintang untuk tali, jejak, dan efek pendaratan.");
        add("MIXED CONTROLS", "मिले-जुले नियंत्रण", "KONTROL CAMPURAN");
        add("SHARE RESULT", "नतीजा साझा करें", "BAGIKAN HASIL");
        add("Unable to share this result.", "यह नतीजा साझा नहीं हो सका।", "Hasil ini tidak dapat dibagikan.");
        add("One course. A fresh drop every day.", "एक रास्ता। हर दिन नई गिरावट।", "Satu rute. Tantangan baru setiap hari.");
        add("JUMPS", "छलाँगें", "LOMPATAN");
        add("UNLOCKED: %s", "खुला: %s", "TERBUKA: %s");
        add("Practice does not award stars.", "अभ्यास में सितारे नहीं मिलते।", "Latihan tidak memberi bintang.");
        add("Stars combine across your best runs.", "अच्छी कोशिशों के सितारे जुड़ते हैं।", "Bintang digabung dari percobaan terbaikmu.");
        add("Jump. Burst %d of %d. No total limit.", "छलाँग। एक बार में %d में से %d। कुल सीमा नहीं।", "Lompat. Rangkaian %d dari %d. Tanpa batas total.");
        add("Jump. Burst %d of %d. Stage budget %d of %d remaining.", "छलाँग। एक बार में %d में से %d। चरण में %d में से %d बाकी।", "Lompat. Rangkaian %d dari %d. Sisa batas tahap %d dari %d.");
        add("BURST %d/%d", "एक बार में %d/%d", "RANGKAIAN %d/%d");
        add("%.1fs / %d JUMPS", "%.1f सेकंड / %d छलाँगें", "%.1f dtk / %d LOMPATAN");
        add("First light", "पहली रोशनी", "Cahaya Pertama");
        add("Catch the light", "रोशनी का पीछा", "Tangkap Cahaya");
        add("Moon drift", "चाँद सी गिरावट", "Hanyut Bulan");
        add("Prism keeper", "प्रिज़्म का रखवाला", "Penjaga Prisma");
        add("Spring canyon", "उछाल की घाटी", "Ngarai Pegas");
        add("Heavy footsteps", "भारी कदम", "Langkah Berat");
        add("Spring pursuit", "उछाल का पीछा", "Kejar Pantulan");
        add("Sawtooth sentinel", "आरी का प्रहरी", "Penjaga Gergaji");
        add("Fraying point", "टूटने का मोड़", "Titik Rapuh");
        add("Ember anchors", "अंगारों के सहारे", "Jangkar Bara");
        add("One spark", "एक चिंगारी", "Satu Percikan");
        add("Furnace heart", "भट्ठी का दिल", "Jantung Tungku");
        add("Read the wind", "हवा को पढ़ें", "Baca Angin");
        add("Gust windows", "झोंकों के बीच", "Celah Hembusan");
        add("Storm springs", "तूफ़ानी उछाल", "Pegas Badai");
        add("Thunder engine", "गरज की मशीन", "Mesin Petir");
        add("Borrowed seconds", "उधार के पल", "Detik Pinjaman");
        add("Soft metal", "नरम धातु", "Logam Lunak");
        add("Clockwork bounce", "घड़ी सा उछाल", "Pantulan Mesin");
        add("Foundry guardian", "ढलाईघर का रक्षक", "Penjaga Pabrik");
        add("Closing circle", "सिमटता घेरा", "Lingkaran Menyempit");
        add("Living current", "जीवंत प्रवाह", "Arus Hidup");
        add("Last rehearsal", "अंतिम अभ्यास", "Latihan Terakhir");
        add("The falling core", "गिरता केंद्र", "Inti yang Jatuh");
        add("The lights have trapped the city. Find a path through their rhythm.", "रोशनियों ने शहर को जकड़ लिया है। उनकी लय के बीच रास्ता खोजें।", "Cahaya menjebak kota. Temukan jalan di sela iramanya.");
        add("The canyon keeps what falls. Trust the bounce, then let go.", "घाटी गिरने वालों को रोक लेती है। उछाल पर भरोसा करें, फिर छोड़ दें।", "Ngarai menahan yang jatuh. Percayai pantulan, lalu lepaskan.");
        add("The furnace is awake. Borrow a breath of slow time.", "भट्ठी जाग चुकी है। कुछ पल धीमे समय के लें।", "Tungku telah bangun. Pinjam sejenak waktu yang melambat.");
        add("The storm has no master. Read the wind before it turns.", "तूफ़ान किसी का नहीं। हवा बदलने से पहले उसे पहचानें।", "Badai tak bertuan. Baca angin sebelum berubah.");
        add("The foundry is still running. Break its rhythm, not your nerve.", "ढलाईघर अब भी चल रहा है। उसकी लय तोड़ें, अपना हौसला नहीं।", "Pabrik masih berjalan. Patahkan iramanya, pertahankan nyalimu.");
        add("Every fall led here. Bring the last machine to silence.", "हर गिरावट यहाँ लाई है। आखिरी मशीन को शांत करें।", "Setiap jatuh membawamu ke sini. Bungkam mesin terakhir.");
        add("A line of light goes dark. A new way down opens.", "रोशनी की एक लकीर बुझी। नीचे जाने का नया रास्ता खुला।", "Satu garis cahaya padam. Jalan baru ke bawah terbuka.");
        add("The canyon lets you pass. Something burns below.", "घाटी ने रास्ता दे दिया। नीचे कुछ जल रहा है।", "Ngarai membiarkanmu lewat. Sesuatu terbakar di bawah.");
        add("The furnace quiets. The sky answers with thunder.", "भट्ठी शांत हुई। आसमान ने गरजकर जवाब दिया।", "Tungku mereda. Langit menjawab dengan petir.");
        add("The storm clears. The foundry waits beneath it.", "तूफ़ान छँट गया। नीचे ढलाईघर इंतज़ार कर रहा है।", "Badai reda. Pabrik menunggu di bawah.");
        add("The blades rest. One final descent remains.", "धारें थम गईं। बस एक आखिरी गिरावट बाकी है।", "Bilah beristirahat. Satu penurunan terakhir tersisa.");
        add("The machines are quiet. Tomorrow brings a new drop.", "मशीनें शांत हैं। कल नई गिरावट लाएगा।", "Mesin-mesin telah diam. Besok hadir tantangan baru.");
    }
    public static String text(int language,String english,Object...args){
        if(english==null)return "";
        String[] row=STRINGS.get(english);
        if(row==null || language!=FalleyStrings.HINDI && language!=FalleyStrings.INDONESIAN)
            return FalleyStrings.text(language,english,args);
        String value=row[language==FalleyStrings.HINDI?0:1];
        if(args==null || args.length==0)return value;
        try{return String.format(Locale.US,value,args);}
        catch(IllegalFormatException invalid){return FalleyStrings.text(language,english,args);}
    }
    private static void add(String english,String hindi,String indonesian){STRINGS.put(english,new String[]{hindi,indonesian});}
    private ExpansionStrings(){}
}
