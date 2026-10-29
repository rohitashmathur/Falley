package com.perfgames.ropelab;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/** Offline release interface copy. Older screens retain the existing translation catalog chain. */
public final class ReleaseStrings {
    private static final Map<String,String[]> STRINGS=new HashMap<>();
    static {
        add("CHAPTER %d / %d","अध्याय %d / %d","BAB %d / %d");
        add("Finish the previous stage","पिछला चरण पूरा करें","Selesaikan tahap sebelumnya");
        add("Recommended","सुझाया गया","Disarankan");
        add("Choose a stage","चरण चुनें","Pilih tahap");
        add("Start stage %d","चरण %d शुरू करें","Mulai tahap %d");
        add("Continue stage %d","चरण %d जारी रखें","Lanjut tahap %d");
        add("Chapter locked","अध्याय बंद है","Bab terkunci");
        add("Standard Daily physics","रोज़ की चुनौती में सामान्य भौतिकी","Fisika standar Daily");
        add("Jump %s · Weight %s","छलाँग %s · भार %s","Lompatan %s · Beban %s");
        add("Attack protection only. Walls and spikes still kill.","केवल हमलों से सुरक्षा। दीवारें और काँटे अब भी घातक हैं।","Hanya melindungi dari serangan. Dinding dan duri tetap mematikan.");
        add("Every avatar uses the same hitbox.","हर अवतार के टकराने का आकार समान है।","Semua avatar memakai area benturan yang sama.");
        add("Same hitbox. Daily uses Human abilities.","टकराने का आकार समान। रोज़ की चुनौती में मानव की क्षमताएँ।","Area benturan sama. Daily memakai kemampuan Manusia.");
        add("Standard jumps","सामान्य छलाँगें","Lompatan standar");
        add("Jump height +20%","छलाँग की ऊँचाई +20%","Tinggi lompatan +20%");
        add("Jump height −15%","छलाँग की ऊँचाई −15%","Tinggi lompatan −15%");
        add("Jump height +35%","छलाँग की ऊँचाई +35%","Tinggi lompatan +35%");
        add("Fireball immunity","आग के गोलों से सुरक्षा","Kebal bola api");
        add("Laser and lightning immunity","लेज़र और बिजली से सुरक्षा","Kebal laser dan petir");
        add("Standard rope load. All hazards can still kill.","रस्सी पर सामान्य भार। हर खतरा अब भी घातक है।","Beban tali standar. Semua bahaya tetap mematikan.");
        add("Standard rope load. Wings do not block hazards.","रस्सी पर सामान्य भार। पंख खतरों से नहीं बचाते।","Beban tali standar. Sayap tidak menahan bahaya.");
        add("Rope load +25%. No hazard immunity.","रस्सी का भार +25%। खतरों से सुरक्षा नहीं।","Beban tali +25%. Tidak kebal bahaya.");
        add("Rope load −25%. No hazard immunity.","रस्सी का भार −25%। खतरों से सुरक्षा नहीं।","Beban tali −25%. Tidak kebal bahaya.");
        add("Rope load +15%. Fire walls and other hazards still kill.","रस्सी का भार +15%। जलती दीवारें और बाकी खतरे अब भी घातक हैं।","Beban tali +15%. Dinding api dan bahaya lain tetap mematikan.");
        add("Jump height +20%; rope load −15%. Fireballs still kill.","छलाँग की ऊँचाई +20%; रस्सी का भार −15%। आग के गोले अब भी घातक हैं।","Tinggi lompatan +20%; beban tali −15%. Bola api tetap mematikan.");
        add("A city runner who turns every fall into a way forward.","शहर का धावक, जो हर गिरावट में आगे बढ़ने की राह खोजता है।","Pelari kota yang mengubah setiap jatuh menjadi jalan ke depan.");
        add("Escaped the furnace with wings made for one more leap.","भट्टी से निकला, एक और छलाँग के लिए बने पंखों के साथ।","Lolos dari tungku dengan sayap untuk satu lompatan lagi.");
        add("A sky watcher following the ropes into the dark.","आसमान का प्रहरी, जो रस्सियों के सहारे अँधेरे में उतरता है।","Penjaga langit yang mengikuti tali menuju kegelapan.");
        add("A rescue veteran carrying the weight of those left below.","अनुभवी बचावकर्मी, नीचे छूटे लोगों की ज़िम्मेदारी उठाए।","Veteran penyelamat yang memikul beban mereka yang tertinggal di bawah.");
        add("A stranded explorer, light on the ropes and hungry for open sky.","भटका खोजी, रस्सियों पर हल्का और खुले आसमान का तलबगार।","Penjelajah tersesat, ringan di tali dan merindukan langit terbuka.");
        add("A foundry worker built to cross sparks and falling fire.","ढलाईघर का कर्मी, चिंगारियों और बरसती आग से गुज़रने के लिए बना।","Pekerja pengecoran yang dibuat untuk melewati percikan dan hujan api.");
        add("Born between broken lights, at home in beams and storms.","टूटी रोशनी के बीच जन्मा, किरणों और तूफ़ानों का साथी।","Lahir di antara cahaya yang padam, akrab dengan sinar dan badai.");
        add("The city hangs in light. Read the prism rig's charge and find a way down.","शहर रोशनी में अटका है। प्रिज़्म मशीन का चार्ज पहचानें और नीचे की राह खोजें।","Kota terjebak dalam cahaya. Baca isian mesin prisma dan cari jalan turun.");
        add("The canyon keeps what falls. A spinning rig guards the last bounce.","घाटी गिरी चीज़ों को रोक लेती है। घूमती मशीन आखिरी उछाल की रखवाली करती है।","Ngarai menahan yang jatuh. Mesin berputar menjaga pantulan terakhir.");
        add("Below the canyon, a furnace demon wakes. Read its hands before the fire flies.","घाटी के नीचे भट्टी का दानव जागता है। आग निकलने से पहले उसके हाथ देखें।","Di bawah ngarai, iblis tungku bangun. Perhatikan tangannya sebelum api melesat.");
        add("A storm robot drinks the sky. Its coils glow before every bolt.","तूफ़ानी रोबोट आसमान की शक्ति खींचता है। हर बिजली से पहले उसकी कुंडलियाँ चमकती हैं।","Robot badai menyerap daya langit. Kumparannya berpijar sebelum setiap petir.");
        add("The foundry's iron guardian is still running. Its cannon spits saws and light.","ढलाईघर का लौह रक्षक अब भी चल रहा है। उसकी तोप आरे और रोशनी उगलती है।","Penjaga besi pengecoran masih bekerja. Meriamnya melontarkan gergaji dan cahaya.");
        add("The core demon waits at the end. Read each charge; take the gap it leaves.","अंत में कोर दानव इंतज़ार करता है। हर चार्ज पहचानें और बची जगह से निकलें।","Iblis inti menunggu di ujung. Baca setiap isian dan ambil celah yang tersisa.");
        add("The prism rig sleeps. A winged stranger joins your descent.","प्रिज़्म मशीन सो गई। पंखों वाला अजनबी आपके सफ़र में जुड़ता है।","Mesin prisma tertidur. Sosok bersayap ikut turun bersamamu.");
        add("The saw rig falls silent. A sky watcher offers another way down.","आरे की मशीन शांत हुई। आकाश प्रहरी नीचे जाने का नया रास्ता दिखाता है।","Mesin gergaji terdiam. Penjaga langit menawarkan jalan turun lain.");
        add("The furnace demon fades. A rescue veteran answers the falling sparks.","भट्टी का दानव खो गया। अनुभवी बचावकर्मी बरसती चिंगारियों के बीच पहुँचता है।","Iblis tungku memudar. Veteran penyelamat datang menembus percikan.");
        add("The storm robot powers down. An explorer arrives from the quiet sky.","तूफ़ानी रोबोट रुक गया। शांत आसमान से एक खोजी उतरता है।","Robot badai padam. Seorang penjelajah datang dari langit yang tenang.");
        add("The iron guardian stops. A foundry worker chooses the way down.","लौह रक्षक थम गया। ढलाईघर का कर्मी नीचे का रास्ता चुनता है।","Penjaga besi berhenti. Pekerja pengecoran memilih jalan turun.");
        add("The core demon dissolves. A shadow steps out of the broken light.","कोर दानव बिखर गया। टूटी रोशनी से एक छाया निकलती है।","Iblis inti lenyap. Bayangan melangkah dari cahaya yang pecah.");
        add("Finale: watch the caster charge, then move clear of its shot.","अंतिम चुनौती: हमलावर का चार्ज देखें, फिर निशाने से हटें।","Finale: perhatikan pengisi serangan, lalu hindari tembakannya.");
        add("GEAR RIG","गियर मशीन","MESIN GIGI");
        add("FURNACE DEMON","भट्टी का दानव","IBLIS TUNGKU");
        add("STORM ROBOT","तूफ़ानी रोबोट","ROBOT BADAI");
        add("FORGE ROBOT","ढलाईघर का रोबोट","ROBOT TEMPA");
        add("CORE DEMON","कोर दानव","IBLIS INTI");
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
        add("Build your survival run.","अपनी सर्वाइवल चुनौती चुनें।","Rancang tantangan bertahanmu.");
        add("Longer warnings","ज़्यादा चेतावनी समय","Peringatan lebih lama");
        add("A steady challenge","संतुलित चुनौती","Tantangan seimbang");
        add("Faster attacks","तेज़ हमले","Serangan lebih cepat");
        add("CHOOSE HAZARDS","खतरे चुनें","PILIH BAHAYA");
        add("ROPE MIX","मिश्रित रस्सियाँ","CAMPURAN TALI");
        add("Special ropes from Wave 2","वेव 2 से खास रस्सियाँ","Tali khusus mulai Gelombang 2");
        add("Tap cards to add or remove hazards.","खतरे चुनने या हटाने के लिए कार्ड टैप करें।","Ketuk kartu untuk memilih atau menghapus bahaya.");
        add("Mint chevrons: Spring rebound 1.5×","हरे तीर: स्प्रिंग उछाल 1.5×","Panah hijau: pantulan Pegas 1.5×");
        add("Mint chevrons: Spring / Violet bolt: Static","हरे तीर: स्प्रिंग / बैंगनी बिजली: स्थैतिक","Panah hijau: Pegas / Petir ungu: Statis");
        add("Every course","हर कोर्स में","Di setiap rute");
        add("Campaign Stage 1 onward / Arcade Wave 2","अभियान चरण 1 से / आर्केड वेव 2","Kampanye Tahap 1 / Arkade Gelombang 2");
        add("Campaign Stage 3 onward / Arcade Wave 2","अभियान चरण 3 से / आर्केड वेव 2","Kampanye Tahap 3 / Arkade Gelombang 2");
        add("Ropes change within each course. Read their colour and symbol. Daily uses Classic.","हर कोर्स में रस्सियाँ बदलती हैं। रंग और चिन्ह पहचानें। रोज़ की चुनौती में क्लासिक रस्सी रहती है।","Jenis tali berganti dalam rute. Perhatikan warna dan simbolnya. Daily memakai Klasik.");
        add("BREEZE","हल्की हवा","ANGIN");
        add("New avatar unlocked","नया अवतार अनलॉक हुआ","Avatar baru terbuka");
        add("A new way to fall.","गिरने का नया अंदाज़।","Gaya baru saat jatuh.");
        add("Current avatar: %s","अभी चुना अवतार: %s","Avatar saat ini: %s");
        add("Same physics. A different jumping style.","भौतिकी समान। छलाँग का अंदाज़ अलग।","Fisika sama. Gaya lompatan berbeda.");
        add("Use %s","%s चुनें","Gunakan %s");
        add("Keep %s","%s को रखें","Tetap pakai %s");
        add("Could not save. Please try again.","सहेज नहीं पाए। फिर कोशिश करें।","Gagal menyimpan. Silakan coba lagi.");
        add("MEET YOUR AVATAR","नया अवतार देखें","LIHAT AVATAR BARU");
        add("Complete chapters for avatars. Campaign and Arcade mix rope types; Daily uses Classic.","अध्याय पूरे करके अवतार पाएँ। अभियान और आर्केड में मिली-जुली रस्सियाँ हैं; रोज़ की चुनौती में क्लासिक।","Selesaikan bab untuk avatar. Kampanye dan Arkade mencampur jenis tali; Daily memakai Klasik.");
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
        add("DIAGNOSTICS & ANALYTICS","निदान और विश्लेषण","DIAGNOSTIK & ANALITIK");
        add("Diagnostics & analytics","निदान और विश्लेषण","Diagnostik & analitik");
        add("Help improve Falley","Falley को बेहतर बनाने में मदद करें","Bantu tingkatkan Falley");
        add("Both choices are optional and off by default.","दोनों विकल्प वैकल्पिक हैं और शुरुआत में बंद हैं।","Kedua pilihan opsional dan awalnya nonaktif.");
        add("Crash reports","क्रैश रिपोर्ट","Laporan crash");
        add("Gameplay analytics","गेमप्ले विश्लेषण","Analitik permainan");
        add("Send crash, error and device diagnostics to Firebase.","क्रैश, त्रुटि और डिवाइस के निदान Firebase को भेजें।","Kirim laporan crash, kesalahan dan diagnostik perangkat ke Firebase.");
        add("Share runs, stages and control choices to improve balance.","संतुलन सुधारने के लिए दौर, चरण और नियंत्रण के विकल्प साझा करें।","Bagikan permainan, tahap dan pilihan kontrol untuk meningkatkan keseimbangan.");
        add("Crash report changes fully apply next app launch. Reports already sending may finish.","क्रैश रिपोर्ट के बदलाव अगली बार ऐप खोलने पर पूरी तरह लागू होंगे। भेजी जा रही रिपोर्ट पूरी हो सकती हैं।","Perubahan laporan crash berlaku penuh saat aplikasi dibuka berikutnya. Laporan yang sedang dikirim mungkin tetap selesai.");
        add("Enabling crash reports can include reports saved on this device.","क्रैश रिपोर्ट चालू करने पर इस डिवाइस में सहेजी रिपोर्ट भी भेजी जा सकती हैं।","Mengaktifkan laporan crash dapat menyertakan laporan yang tersimpan di perangkat ini.");
        add("No ads, advertising ID, raw motion or personal profile data.","कोई विज्ञापन, विज्ञापन आईडी, गति के मूल नमूने या निजी प्रोफ़ाइल डेटा नहीं।","Tanpa iklan, ID iklan, data gerakan mentah atau data profil pribadi.");
        add("Diagnostics are unavailable. Your choices are saved.","निदान अभी उपलब्ध नहीं है। आपके विकल्प सहेजे गए हैं।","Diagnostik tidak tersedia. Pilihan Anda telah disimpan.");
        PrivacyPolicy.addTranslations(STRINGS);
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
