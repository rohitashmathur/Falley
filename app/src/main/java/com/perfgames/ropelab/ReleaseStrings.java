package com.perfgames.ropelab;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/** Offline 4.2 interface copy. Older screens retain the existing translation catalog chain. */
public final class ReleaseStrings {
    private static final Map<String,String[]> STRINGS=new HashMap<>();
    static {
        add("Before the drop","गिरने से पहले","Sebelum turun");
        add("Three star goals","तीन सितारों के लक्ष्य","Tiga target bintang");
        add("Par ≤ %d","लक्ष्य ≤ %d","Target ≤ %d");
        add("Finish to progress. Other stars are optional.","आगे बढ़ने के लिए चरण पूरा करें। बाकी सितारे वैकल्पिक हैं।","Selesaikan tahap untuk maju. Bintang lainnya opsional.");
        add("Practice options","अभ्यास के विकल्प","Opsi latihan");
        add("Hide practice options","अभ्यास के विकल्प छिपाएँ","Sembunyikan opsi latihan");
        add("Avatars and ropes. Make the fall yours.","अवतार और रस्सियाँ। गिरावट को अपना रंग दें।","Avatar dan tali. Turun dengan gayamu.");
        add("PREVIEW JUMP","छलाँग की झलक","PRATINJAU LOMPATAN");
        add("Classic jump","सामान्य छलाँग","Lompatan klasik");
        add("Wings spread on takeoff","छलाँग लगाते ही पंख खुलते हैं","Sayap terbuka saat melompat");
        add("Graceful wing flare","सुंदरता से फैलते पंख","Kepakan sayap anggun");
        add("Tucked knees","मुड़े हुए घुटने","Lutut ditekuk");
        add("Floating jump pose","तैरती छलाँग की मुद्रा","Pose lompatan melayang");
        add("Mechanical spring pose","यांत्रिक स्प्रिंग की मुद्रा","Pose pegas mekanis");
        add("Shadow stretch","छाया का खिंचाव","Regangan bayangan");
        add("Jump style only. Same physics and hitbox.","केवल छलाँग का अंदाज़ बदलेगा। भौतिकी और टक्कर का आकार वही।","Hanya gaya lompatan. Fisika dan area benturan tetap sama.");
        add("Classic","क्लासिक","Klasik");
        add("Spring","स्प्रिंग","Pegas");
        add("Static","स्थैतिक","Statis");
        add("Original rope rebound. No extra effects.","रस्सी का मूल उछाल। कोई अतिरिक्त असर नहीं।","Pantulan tali asli. Tanpa efek tambahan.");
        add("1.5× supporting-rope rebound. Manual jumps stay normal.","सहारा देने वाली रस्सी का उछाल 1.5×। टैप से छलाँगें सामान्य रहती हैं।","Pantulan dari tali penopang 1.5×. Lompatan manual tetap normal.");
        add("After rope support: next manual jump 0.8×, brief steering damping.","रस्सी का सहारा मिलने पर: अगली टैप वाली छलाँग 0.8×, मोड़ने की प्रतिक्रिया थोड़ी देर धीमी।","Setelah ditopang tali: lompatan manual berikutnya 0.8×, kendali arah melambat sesaat.");
        add("Campaign abilities. Daily and Arcade use Classic.","अभियान की क्षमताएँ। रोज़ की चुनौती और आर्केड में क्लासिक रस्सी रहती है।","Kemampuan kampanye. Daily dan Arkade memakai Klasik.");
        add("Retro music","रेट्रो संगीत","Musik retro");
        add("Choose your language","अपनी भाषा चुनें","Pilih bahasamu");
        add("English is selected by default.","शुरुआत में अंग्रेज़ी चुनी हुई है।","Bahasa Inggris dipilih secara bawaan.");
        add("You can change this anytime in Settings.","इसे कभी भी सेटिंग्स में बदल सकते हैं।","Kamu bisa mengubahnya kapan saja di Pengaturan.");
        add("Region hint: %s","क्षेत्र का संकेत: %s","Petunjuk wilayah: %s");
        add("Available for your region","आपके क्षेत्र के लिए उपलब्ध","Tersedia untuk wilayahmu");
        add("Your choice stays on this device.","आपकी पसंद इसी डिवाइस पर रहती है।","Pilihanmu tetap di perangkat ini.");
        add("1 NEW STAR","1 नया सितारा","1 BINTANG BARU");
        add("%d NEW STARS","%d नए सितारे","%d BINTANG BARU");
        add("Try this","यह आज़माएँ","Coba ini");
        add("%d%% OF THE DROP","गिरावट का %d%%","%d%% RUTE TURUN");
        add("Static charged: next jump 0.8×","स्थैतिक चार्ज: अगली छलाँग 0.8×","Statis terisi: lompatan berikutnya 0.8×");
        add("Static current","स्थैतिक प्रवाह","Arus statis");
        add("Spring rebound 1.5×","स्प्रिंग उछाल 1.5×","Pantulan Pegas 1.5×");
        add("Classic rope","क्लासिक रस्सी","Tali Klasik");
        add("Jump recovery","छलाँग फिर तैयार","Pemulihan lompatan");
        add("Classic rope in Daily and Arcade.","रोज़ की चुनौती और आर्केड में क्लासिक रस्सी।","Tali Klasik di Daily dan Arkade.");
        add("Daily Drop refreshes at 00:00 UTC. Complete chapters for avatars; collect stars for rope abilities.","रोज़ की चुनौती 00:00 UTC पर बदलती है। अध्यायों से अवतार पाएँ; रस्सियों की क्षमताओं के लिए सितारे जुटाएँ।","Daily Drop diperbarui pukul 00.00 UTC. Selesaikan bab untuk avatar; kumpulkan bintang untuk kemampuan tali.");
        add("Updated: 25 October 2026","अद्यतन: 25 अक्टूबर 2026","Diperbarui: 25 Oktober 2026");
        add("Country and language","देश और भाषा","Negara dan bahasa");
        add("Falley may read a coarse country hint from your mobile network, SIM provider or device region to assist language selection. This is not precise location. The hint stays temporarily on your device and is not saved or sent anywhere. English is selected by default. You choose a language on first launch and can change it in Settings.",
            "भाषा चुनने में मदद के लिए Falley आपके मोबाइल नेटवर्क, सिम प्रदाता या डिवाइस के क्षेत्र से देश का सामान्य संकेत पढ़ सकता है। यह सटीक स्थान नहीं है। यह संकेत अस्थायी रूप से आपके डिवाइस पर रहता है; इसे सहेजा या कहीं भेजा नहीं जाता। शुरुआत में अंग्रेज़ी चुनी हुई है। पहली बार ऐप खोलने पर आप भाषा चुनते हैं और इसे सेटिंग्स में बदल सकते हैं।",
            "Falley dapat membaca petunjuk negara secara umum dari jaringan seluler, penyedia SIM, atau wilayah perangkat untuk membantu pemilihan bahasa. Ini bukan lokasi yang tepat. Petunjuk hanya digunakan sementara di perangkat Anda, tidak disimpan atau dikirim ke mana pun. Bahasa Inggris dipilih secara bawaan. Anda memilih bahasa saat pertama membuka aplikasi dan dapat mengubahnya di Pengaturan.");
        add("Falley saves your settings, selected stage, scores, run counts and best times in the app's private storage on your device. Music, sound effects and vibration are produced locally. No gameplay data is sent to Perf Games or another service.",
            "Falley आपकी सेटिंग्स, चुना हुआ चरण, स्कोर, कोशिशों की संख्या और सबसे अच्छे समय आपके डिवाइस पर ऐप के निजी स्टोरेज में सहेजता है। संगीत, ध्वनि प्रभाव और कंपन स्थानीय रूप से बनते हैं। खेल का कोई डेटा Perf Games या किसी दूसरी सेवा को नहीं भेजा जाता।",
            "Falley menyimpan pengaturan, tahap yang dipilih, skor, jumlah permainan, dan waktu terbaik di penyimpanan pribadi aplikasi pada perangkat Anda. Musik, efek suara, dan getaran dihasilkan secara lokal. Tidak ada data permainan yang dikirim ke Perf Games atau layanan lain.");
        add("You can turn motion steering, music, sound effects, vibration and blood effects on or off in Settings. Falley does not ask for camera, microphone, location or contacts access.",
            "आप सेटिंग्स में झुकाकर दिशा बदलने, संगीत, ध्वनि प्रभाव, कंपन और खून के प्रभाव चालू या बंद कर सकते हैं। Falley कैमरे, माइक्रोफ़ोन, स्थान या संपर्कों तक पहुँच नहीं माँगता।",
            "Anda dapat menyalakan atau mematikan kendali gerak, musik, efek suara, getaran, dan efek darah di Pengaturan. Falley tidak meminta akses kamera, mikrofon, lokasi, atau kontak.");
    }
    public static String text(int language,String english,Object...args){
        if(english==null)return "";
        String[] row=STRINGS.get(english);
        if(row==null||language!=FalleyStrings.HINDI&&language!=FalleyStrings.INDONESIAN)
            return UxStrings.text(language,english,args);
        String value=row[language==FalleyStrings.HINDI?0:1];
        if(args==null||args.length==0)return value;
        try{return String.format(Locale.US,value,args);}
        catch(IllegalFormatException invalid){return UxStrings.text(language,english,args);}
    }
    private static void add(String english,String hindi,String indonesian){
        if(STRINGS.put(english,new String[]{hindi,indonesian})!=null)throw new IllegalStateException("Duplicate release translation: "+english);
    }
    private ReleaseStrings(){}
}
