package com.perfgames.ropelab;

import java.util.Map;

/** Current 4.4 policy: offline play plus separately optional Google Firebase reporting. */
public final class PrivacyPolicy {
    public static final String TITLE = "Falley Privacy Policy";
    public static final String PUBLISHER = "Publisher: Perf Games";
    public static final String EFFECTIVE_DATE = "Effective: 16 October 2026";
    public static final String UPDATED_DATE = "Updated: 29 October 2026";
    public static final String CONTACT_EMAIL = "mathurrohitash16@gmail.com";
    public static final String PUBLIC_URL = "https://falley-privacy-policy.vercel.app/";
    // Each row contains heading/body pairs in English, Hindi and Bahasa Indonesia.
    private static final String[][] COPY = {
        {"Offline play and optional reporting",
            "Falley 4.4 can be played without an Internet connection. Perf Games offers optional crash reports through Firebase Crashlytics and optional gameplay analytics through Google Analytics for Firebase, provided by Google. Each option is off by default until you enable it separately in Settings. Neither option is required to play. With both options off when the app starts, Falley does not initialize Firebase. Falley has no accounts, ads or purchases.",
            "ऑफ़लाइन खेल और वैकल्पिक रिपोर्टिंग",
            "Falley 4.4 बिना इंटरनेट कनेक्शन के खेला जा सकता है। Perf Games, Google की Firebase Crashlytics सेवा से वैकल्पिक क्रैश रिपोर्ट और Google Analytics for Firebase से वैकल्पिक गेमप्ले विश्लेषण देता है। जब तक आप सेटिंग्स में हर विकल्प अलग से चालू न करें, दोनों बंद रहते हैं। खेलने के लिए इनमें से कोई भी ज़रूरी नहीं है। ऐप शुरू होते समय दोनों विकल्प बंद हों तो Falley, Firebase शुरू नहीं करता। Falley में खाते, विज्ञापन या खरीदारी नहीं है।",
            "Bermain luring dan pelaporan opsional",
            "Falley 4.4 dapat dimainkan tanpa koneksi Internet. Perf Games menawarkan laporan error opsional melalui Firebase Crashlytics dan analitik permainan opsional melalui Google Analytics for Firebase, yang disediakan Google. Keduanya mati secara bawaan hingga Anda mengaktifkan setiap pilihan secara terpisah di Pengaturan. Keduanya tidak wajib untuk bermain. Jika keduanya mati saat aplikasi dimulai, Falley tidak menginisialisasi Firebase. Falley tidak memiliki akun, iklan, atau pembelian."},
        {"Motion controls",
            "If you enable motion steering, accelerometer and gyroscope readings are processed temporarily on your device to control the game. Falley does not save sensor history or include raw sensor readings, touch coordinates or motion-calibration samples in reporting. Touch controls work with motion steering off.",
            "मोशन नियंत्रण",
            "गति से दिशा बदलना चालू करने पर, एक्सेलेरोमीटर और जायरोस्कोप की रीडिंग खेल को नियंत्रित करने के लिए आपके डिवाइस पर अस्थायी रूप से संसाधित होती हैं। Falley सेंसर का इतिहास सहेजता नहीं है और रिपोर्ट में कच्ची सेंसर रीडिंग, स्पर्श के निर्देशांक या गति कैलिब्रेशन के नमूने शामिल नहीं करता। गति नियंत्रण बंद होने पर स्पर्श नियंत्रण काम करता है।",
            "Kontrol gerak",
            "Jika kendali gerak diaktifkan, bacaan akselerometer dan giroskop diproses sementara di perangkat untuk mengendalikan permainan. Falley tidak menyimpan riwayat sensor atau menyertakan bacaan sensor mentah, koordinat sentuhan, atau sampel kalibrasi gerak dalam pelaporan. Kendali sentuh tetap berfungsi saat kendali gerak dimatikan."},
        {"Country and language",
            "Falley may read a coarse country hint from your mobile network, SIM provider or device region to assist language selection. This is not precise location. That hint is temporary, is not saved, and is not included in Falley reporting events. English is selected by default; you choose a language on first launch and can change it in Settings. If you enable Analytics, Google may separately derive approximate geography from network information such as your IP address.",
            "देश और भाषा",
            "भाषा चुनने में मदद के लिए Falley आपके मोबाइल नेटवर्क, सिम प्रदाता या डिवाइस के क्षेत्र से देश का सामान्य संकेत पढ़ सकता है। यह सटीक स्थान नहीं है। यह संकेत अस्थायी है, सहेजा नहीं जाता और Falley के रिपोर्टिंग इवेंट में शामिल नहीं होता। शुरुआत में अंग्रेज़ी चुनी रहती है; पहली बार ऐप खोलने पर आप भाषा चुनते हैं और इसे सेटिंग्स में बदल सकते हैं। Analytics चालू करने पर Google आपके आईपी पते जैसी नेटवर्क जानकारी से अलग से अनुमानित भौगोलिक क्षेत्र निकाल सकता है।",
            "Negara dan bahasa",
            "Falley dapat membaca petunjuk negara secara umum dari jaringan seluler, penyedia SIM, atau wilayah perangkat untuk membantu pemilihan bahasa. Ini bukan lokasi yang tepat. Petunjuk ini bersifat sementara, tidak disimpan, dan tidak disertakan dalam peristiwa pelaporan Falley. Bahasa Inggris dipilih secara bawaan; Anda memilih bahasa saat pertama membuka aplikasi dan dapat mengubahnya di Pengaturan. Jika Analytics diaktifkan, Google dapat memperkirakan wilayah secara terpisah dari informasi jaringan seperti alamat IP."},
        {"Data on your device",
            "Falley saves preferences, reporting choices, selected stages, campaign unlocks, stars, avatar and rope choices, scores, run counts, best times, Daily Drop results and streaks in app-private storage. Music, sound effects and vibration are produced locally. Your full save file is not uploaded. If you enable reporting, the limited diagnostic or gameplay information described below can be sent to Google for Perf Games.",
            "आपके डिवाइस पर डेटा",
            "Falley आपकी पसंद, रिपोर्टिंग के विकल्प, चुने हुए चरण, खुले अभियान चरण, सितारे, अवतार और रस्सियों की पसंद, स्कोर, कोशिशों की संख्या, सबसे अच्छे समय, Daily Drop के परिणाम और लगातार जीत का रिकॉर्ड ऐप के निजी स्टोरेज में सहेजता है। संगीत, ध्वनि प्रभाव और कंपन स्थानीय रूप से बनते हैं। आपकी पूरी सेव फ़ाइल अपलोड नहीं होती। रिपोर्टिंग चालू करने पर, नीचे बताई गई सीमित तकनीकी या गेमप्ले जानकारी Perf Games के लिए Google को भेजी जा सकती है।",
            "Data di perangkatmu",
            "Falley menyimpan preferensi, pilihan pelaporan, tahap yang dipilih, kemajuan kampanye, bintang, pilihan avatar dan tali, skor, jumlah permainan, waktu terbaik, hasil Daily Drop, dan rentetan penyelesaian di penyimpanan pribadi aplikasi. Musik, efek suara, dan getaran dihasilkan secara lokal. Berkas simpanan lengkap tidak diunggah. Jika pelaporan diaktifkan, informasi diagnostik atau permainan terbatas yang dijelaskan di bawah dapat dikirim ke Google untuk Perf Games."},
        {"Optional crash reports",
            "When enabled, Firebase Crashlytics sends crash stack traces and related diagnostics, app version, device and operating-system information, installation identifiers, and limited game state such as mode, stage, wave, avatar, controls and outcome. Perf Games uses these reports to find and fix faults. We do not assign your name, email address or an account ID to reports. Changes to crash reporting fully apply on the next app launch. Enabling it can upload crash information stored locally while Firebase was active earlier, including when only Analytics was enabled. Reports already queued or in transit may still finish sending after you turn it off. Turning it off does not delete reports already received.",
            "वैकल्पिक क्रैश रिपोर्ट",
            "चालू होने पर Firebase Crashlytics क्रैश स्टैक ट्रेस और संबंधित तकनीकी जानकारी, ऐप संस्करण, डिवाइस और ऑपरेटिंग सिस्टम की जानकारी, इंस्टॉलेशन पहचानकर्ता और मोड, चरण, वेव, अवतार, नियंत्रण तथा परिणाम जैसी सीमित गेम स्थिति भेजता है। Perf Games इन रिपोर्ट से समस्याएँ ढूँढ़कर ठीक करता है। हम रिपोर्ट में आपका नाम, ईमेल पता या खाता आईडी नहीं जोड़ते। क्रैश रिपोर्टिंग के बदलाव अगली बार ऐप खोलने पर पूरी तरह लागू होते हैं। इसे चालू करने पर वह क्रैश जानकारी भी अपलोड हो सकती है जो Firebase पहले सक्रिय होने पर डिवाइस में सहेजी गई थी, जैसे केवल Analytics चालू होने के समय। बंद करने के बाद भी पहले से कतार में या भेजी जा रही रिपोर्ट का भेजना पूरा हो सकता है। इसे बंद करने से पहले प्राप्त रिपोर्ट नहीं मिटतीं।",
            "Laporan error opsional",
            "Saat diaktifkan, Firebase Crashlytics mengirim jejak tumpukan error dan diagnostik terkait, versi aplikasi, informasi perangkat dan sistem operasi, pengenal instalasi, serta keadaan permainan terbatas seperti mode, tahap, gelombang, avatar, kendali, dan hasil. Perf Games memakai laporan ini untuk menemukan dan memperbaiki masalah. Kami tidak menambahkan nama, alamat email, atau ID akun Anda ke laporan. Perubahan pelaporan error berlaku sepenuhnya pada pembukaan aplikasi berikutnya. Mengaktifkannya dapat mengunggah informasi error yang tersimpan secara lokal saat Firebase sebelumnya aktif, termasuk saat hanya Analytics yang aktif. Laporan yang sudah mengantre atau sedang dikirim masih dapat selesai terkirim setelah Anda mematikannya. Mematikannya tidak menghapus laporan yang sudah diterima."},
        {"Optional gameplay analytics",
            "When enabled, Google Analytics for Firebase records limited gameplay interactions such as starting and ending a run, with mode, stage, wave, avatar, control mode and outcome. The SDK can also collect automatic app/session events, app-install/source attribution information, timestamps, app version, device and operating-system details, language, an app-instance identifier and approximate geography. Perf Games uses this to understand play and improve balance. Falley disables advertising-ID collection and advertising personalization. We do not send your full save file, raw sensor samples or touch coordinates. Analytics can be turned off independently of crash reports.",
            "वैकल्पिक गेमप्ले विश्लेषण",
            "चालू होने पर Google Analytics for Firebase खेल शुरू और समाप्त करने जैसी सीमित गतिविधियाँ दर्ज करता है, जिनमें मोड, चरण, वेव, अवतार, नियंत्रण मोड और परिणाम शामिल होते हैं। SDK स्वचालित ऐप और सत्र इवेंट, ऐप इंस्टॉल होने और उसके स्रोत की जानकारी, समय, ऐप संस्करण, डिवाइस और ऑपरेटिंग सिस्टम का विवरण, भाषा, ऐप इंस्टेंस का पहचानकर्ता और अनुमानित भौगोलिक क्षेत्र भी इकट्ठा कर सकता है। Perf Games इससे खेल और संतुलन को बेहतर बनाता है। Falley विज्ञापन आईडी का संग्रह और व्यक्तिगत विज्ञापन बंद रखता है। हम पूरी सेव फ़ाइल, कच्चे सेंसर नमूने या स्पर्श के निर्देशांक नहीं भेजते। Analytics को क्रैश रिपोर्ट से अलग बंद किया जा सकता है।",
            "Analitik permainan opsional",
            "Saat diaktifkan, Google Analytics for Firebase mencatat interaksi permainan terbatas seperti memulai dan mengakhiri permainan, beserta mode, tahap, gelombang, avatar, mode kendali, dan hasil. SDK juga dapat mengumpulkan peristiwa aplikasi/sesi otomatis, informasi atribusi pemasangan/sumber aplikasi, waktu, versi aplikasi, detail perangkat dan sistem operasi, bahasa, pengenal instance aplikasi, dan perkiraan wilayah. Perf Games memakainya untuk memahami permainan dan memperbaiki keseimbangan. Falley menonaktifkan pengumpulan ID iklan dan personalisasi iklan. Kami tidak mengirim berkas simpanan lengkap, sampel sensor mentah, atau koordinat sentuhan. Analytics dapat dimatikan secara terpisah dari laporan error."},
        {"Your choices",
            "Both reporting options start off and remain optional. Change crash reports and gameplay analytics separately in Settings. Analytics changes apply immediately and turning it off asks the SDK to reset local Analytics data; crash-report changes fully apply on the next app launch. You can also control motion steering, music, sound effects, vibration and blood effects. Reporting choices are remembered on this device. Turning an option off does not remove information already received by Google. Gameplay remains available with both reporting options off.",
            "आपके विकल्प",
            "शुरुआत में रिपोर्टिंग के दोनों विकल्प बंद हैं और वैकल्पिक रहते हैं। सेटिंग्स में क्रैश रिपोर्ट और गेमप्ले विश्लेषण अलग-अलग बदलें। Analytics के बदलाव तुरंत लागू होते हैं और बंद करने पर SDK से स्थानीय Analytics डेटा रीसेट करने को कहा जाता है; क्रैश रिपोर्टिंग के बदलाव अगली बार ऐप खोलने पर पूरी तरह लागू होते हैं। गति नियंत्रण, संगीत, ध्वनि प्रभाव, कंपन और खून के प्रभाव भी चुन सकते हैं। रिपोर्टिंग की पसंद इसी डिवाइस पर याद रखी जाती है। विकल्प बंद करने से Google को पहले मिली जानकारी नहीं मिटती। दोनों रिपोर्टिंग विकल्प बंद होने पर भी खेल उपलब्ध रहता है।",
            "Pilihanmu",
            "Kedua pilihan pelaporan awalnya mati dan tetap opsional. Ubah laporan error dan analitik permainan secara terpisah di Pengaturan. Perubahan Analytics berlaku segera dan mematikannya meminta SDK mereset data Analytics lokal; perubahan laporan error berlaku sepenuhnya pada pembukaan aplikasi berikutnya. Anda juga dapat mengatur kendali gerak, musik, efek suara, getaran, dan efek darah. Pilihan pelaporan diingat pada perangkat ini. Mematikan pilihan tidak menghapus informasi yang sudah diterima Google. Permainan tetap tersedia saat kedua pilihan pelaporan dimatikan."},
        {"Deleting your data",
            "Clear Falley app storage in Android Settings or uninstall the app to erase local preferences and progress. This cannot be undone; Android app backup and device-transfer backup are disabled. Turning reporting off or clearing local storage does not erase data already received by Google. Analytics retention follows the Google Analytics property settings and applicable deletion procedures. Google currently documents Crashlytics crash traces and associated identifiers as retained for 90 days before removal from live and backup systems begins. Contact us for a privacy request; without an account or identifying details, we may be unable to link a request to a particular pseudonymous report.",
            "अपना डेटा मिटाना",
            "स्थानीय पसंद और प्रगति मिटाने के लिए Android सेटिंग्स में Falley का ऐप स्टोरेज साफ करें या ऐप अनइंस्टॉल करें। इसे वापस नहीं लाया जा सकता; Android ऐप बैकअप और डिवाइस ट्रांसफ़र बैकअप बंद हैं। रिपोर्टिंग बंद करने या स्थानीय स्टोरेज साफ करने से Google को पहले मिला डेटा नहीं मिटता। Analytics का संग्रहण समय Google Analytics प्रॉपर्टी की सेटिंग्स और लागू मिटाने की प्रक्रियाओं के अनुसार होता है। Google के वर्तमान दस्तावेज़ के अनुसार Crashlytics क्रैश ट्रेस और संबंधित पहचानकर्ता 90 दिन तक रखे जाते हैं, फिर सक्रिय और बैकअप सिस्टम से हटाने की प्रक्रिया शुरू होती है। गोपनीयता अनुरोध के लिए संपर्क करें; खाता या पहचान की जानकारी न होने पर हम किसी खास छद्मनाम वाली रिपोर्ट को अनुरोध से जोड़ने में असमर्थ हो सकते हैं।",
            "Menghapus datamu",
            "Hapus penyimpanan aplikasi Falley di Pengaturan Android atau copot aplikasi untuk menghapus preferensi dan kemajuan lokal. Ini tidak dapat dibatalkan; pencadangan aplikasi Android dan transfer perangkat dinonaktifkan. Mematikan pelaporan atau menghapus penyimpanan lokal tidak menghapus data yang sudah diterima Google. Retensi Analytics mengikuti setelan properti Google Analytics dan prosedur penghapusan yang berlaku. Dokumentasi Google saat ini menyatakan jejak error Crashlytics dan pengenal terkait disimpan selama 90 hari sebelum proses penghapusan dari sistem aktif dan cadangan dimulai. Hubungi kami untuk permintaan privasi; tanpa akun atau rincian pengenal, kami mungkin tidak dapat menghubungkan permintaan dengan laporan berpengenal samaran tertentu."},
        {"Google services and network access",
            "Optional reporting uses Google Firebase and Google Analytics. Google processes information under the applicable Firebase/Analytics terms and privacy information; processing may take place outside your country. Perf Games can view the resulting reporting dashboards. Falley uses Internet, network-state, background wake and Play install-referrer access permissions for these services. It does not request camera, microphone, location or contacts access. Music and effects are bundled locally and Falley does not record audio. Read Google's privacy information at https://policies.google.com/privacy and Firebase details at https://firebase.google.com/support/privacy/ .",
            "Google सेवाएँ और नेटवर्क पहुँच",
            "वैकल्पिक रिपोर्टिंग Google Firebase और Google Analytics का उपयोग करती है। Google जानकारी को लागू Firebase/Analytics शर्तों और गोपनीयता जानकारी के अनुसार संसाधित करता है; यह आपके देश के बाहर भी हो सकता है। Perf Games संबंधित रिपोर्टिंग डैशबोर्ड देख सकता है। Falley इन सेवाओं के लिए इंटरनेट, नेटवर्क स्थिति, बैकग्राउंड कार्य के लिए जागृत रखने और Play से ऐप इंस्टॉल होने के स्रोत की जानकारी पढ़ने की अनुमतियाँ उपयोग करता है। यह कैमरा, माइक्रोफ़ोन, स्थान या संपर्कों तक पहुँच नहीं माँगता। संगीत और प्रभाव ऐप में स्थानीय रूप से शामिल हैं और Falley ऑडियो रिकॉर्ड नहीं करता। Google की गोपनीयता जानकारी https://policies.google.com/privacy और Firebase का विवरण https://firebase.google.com/support/privacy/ पर पढ़ें।",
            "Layanan Google dan akses jaringan",
            "Pelaporan opsional memakai Google Firebase dan Google Analytics. Google memproses informasi berdasarkan ketentuan dan informasi privasi Firebase/Analytics yang berlaku; pemrosesan dapat berlangsung di luar negara Anda. Perf Games dapat melihat dasbor pelaporan yang dihasilkan. Falley memakai izin Internet, status jaringan, tetap aktif untuk pekerjaan latar belakang, dan akses sumber pemasangan dari Play untuk layanan ini. Aplikasi tidak meminta akses kamera, mikrofon, lokasi, atau kontak. Musik dan efek disertakan secara lokal dan Falley tidak merekam audio. Baca informasi privasi Google di https://policies.google.com/privacy dan rincian Firebase di https://firebase.google.com/support/privacy/ ."},
        {"Sharing a result",
            "Daily Drop records, stars, streaks and appearance choices are saved on your device. If you choose Share Result, Falley creates an image with the course date, generator version, score, time, jumps, avatar and control mode. Android lets you choose where to share it. No result image is shared automatically. The receiving app handles the image under its own policy. A temporary copy remains in app-private cache; copies saved or shared elsewhere must be deleted there.",
            "नतीजा साझा करना",
            "Daily Drop के रिकॉर्ड, सितारे, लगातार जीत और रूप की पसंद आपके डिवाइस पर सहेजी जाती हैं। Share Result चुनने पर Falley कोर्स की तारीख, जनरेटर संस्करण, स्कोर, समय, छलाँगों, अवतार और नियंत्रण मोड की छवि बनाता है। Android आपको साझा करने का ऐप चुनने देता है। परिणाम की कोई छवि अपने आप साझा नहीं होती। प्राप्त करने वाला ऐप अपनी नीति के अनुसार छवि संभालता है। अस्थायी प्रति ऐप के निजी कैश में रहती है; कहीं और सहेजी या साझा की गई प्रतियाँ वहीं मिटानी होंगी।",
            "Membagikan hasil",
            "Rekor Daily Drop, bintang, rentetan penyelesaian, dan pilihan tampilan disimpan pada perangkat. Jika Anda memilih Bagikan Hasil, Falley membuat gambar berisi tanggal rute, versi generator, skor, waktu, jumlah lompatan, avatar, dan mode kendali. Android memungkinkan Anda memilih tempat berbagi. Tidak ada gambar hasil yang dibagikan otomatis. Aplikasi penerima menangani gambar sesuai kebijakannya sendiri. Salinan sementara tetap berada di cache pribadi aplikasi; salinan yang disimpan atau dibagikan di tempat lain harus dihapus di sana."},
        {"Support messages",
            "If you email us, we receive your address, message and any attachments you choose to send. We use them to answer your inquiry or privacy request. Gmail, provided by Google, handles correspondence under Google's Privacy Policy. We keep messages only as long as needed to resolve your inquiry, unless law requires longer. You can request deletion by email. We do not sell messages or use them for advertising. Contacting us does not automatically send game records or sensor readings. Do not include sensitive information.",
            "सहायता के संदेश",
            "ईमेल करने पर हमें आपका पता, संदेश और आपकी भेजी हुई अटैचमेंट मिलती हैं। हम उनका उपयोग आपके सवाल या गोपनीयता अनुरोध का जवाब देने के लिए करते हैं। Google का Gmail पत्राचार को Google की गोपनीयता नीति के अनुसार संभालता है। हम संदेश केवल समस्या हल करने के लिए ज़रूरी समय तक रखते हैं, जब तक कानून अधिक समय न माँगे। आप ईमेल से मिटाने का अनुरोध कर सकते हैं। हम संदेश बेचते या विज्ञापन के लिए उपयोग नहीं करते। संपर्क करने से खेल के रिकॉर्ड या सेंसर रीडिंग अपने आप नहीं भेजी जातीं। संवेदनशील जानकारी न भेजें।",
            "Pesan dukungan",
            "Jika Anda mengirim email, kami menerima alamat, pesan, dan lampiran yang Anda pilih untuk dikirim. Kami memakainya untuk menjawab pertanyaan atau permintaan privasi. Gmail dari Google menangani korespondensi berdasarkan Kebijakan Privasi Google. Kami menyimpan pesan hanya selama diperlukan untuk menyelesaikan pertanyaan, kecuali hukum mengharuskan lebih lama. Anda dapat meminta penghapusan melalui email. Kami tidak menjual pesan atau memakainya untuk iklan. Menghubungi kami tidak otomatis mengirim rekor permainan atau bacaan sensor. Jangan sertakan informasi sensitif."},
        {"Online privacy policy",
            "Read the public policy at https://falley-privacy-policy.vercel.app/ . Opening it uses your browser and Internet connection. The policy page has no analytics scripts, forms, advertising or external assets. Vercel hosts it and may process technical request information such as an IP address to deliver and protect the page. This website visit is separate from your in-game reporting choices.",
            "ऑनलाइन गोपनीयता नीति",
            "सार्वजनिक नीति https://falley-privacy-policy.vercel.app/ पर पढ़ें। इसे खोलने में आपका ब्राउज़र और इंटरनेट कनेक्शन उपयोग होता है। नीति पेज में Analytics स्क्रिप्ट, फ़ॉर्म, विज्ञापन या बाहरी संसाधन नहीं हैं। Vercel इसे होस्ट करता है और पेज पहुँचाने व सुरक्षित रखने के लिए आईपी पते जैसी तकनीकी अनुरोध जानकारी संसाधित कर सकता है। वेबसाइट देखना खेल के रिपोर्टिंग विकल्पों से अलग है।",
            "Kebijakan privasi daring",
            "Baca kebijakan publik di https://falley-privacy-policy.vercel.app/ . Membukanya memakai browser dan koneksi Internet Anda. Halaman kebijakan tidak memiliki skrip analitik, formulir, iklan, atau aset eksternal. Vercel menghostingnya dan dapat memproses informasi permintaan teknis seperti alamat IP untuk menyediakan dan melindungi halaman. Kunjungan situs ini terpisah dari pilihan pelaporan di dalam permainan."},
        {"About this policy",
            "This policy describes Falley 4.4 by Perf Games. If information practices change, this policy will be updated before a version with those changes is distributed. Older versions may have different behavior; version 4.3 and earlier did not include Firebase reporting.",
            "इस नीति के बारे में",
            "यह नीति Perf Games के Falley 4.4 का वर्णन करती है। जानकारी सँभालने के तरीके बदलने पर, उन बदलावों वाला संस्करण वितरित करने से पहले नीति बदली जाएगी। पुराने संस्करणों का व्यवहार अलग हो सकता है; संस्करण 4.3 और उससे पहले Firebase रिपोर्टिंग शामिल नहीं थी।",
            "Tentang kebijakan ini",
            "Kebijakan ini menjelaskan Falley 4.4 dari Perf Games. Jika praktik penanganan informasi berubah, kebijakan diperbarui sebelum versi dengan perubahan tersebut didistribusikan. Versi lama dapat berperilaku berbeda; versi 4.3 dan sebelumnya tidak menyertakan pelaporan Firebase."},
        {"Privacy contact",
            "For privacy questions about Falley, email Perf Games at mathurrohitash16@gmail.com. The buttons below open the policy website or your email app. No message is sent automatically.",
            "गोपनीयता के लिए संपर्क",
            "Falley की गोपनीयता से जुड़े सवालों के लिए Perf Games को mathurrohitash16@gmail.com पर ईमेल करें। नीचे के बटन नीति की वेबसाइट या आपका ईमेल ऐप खोलते हैं। कोई संदेश अपने आप नहीं भेजा जाता।",
            "Kontak privasi",
            "Untuk pertanyaan privasi tentang Falley, kirim email kepada Perf Games di mathurrohitash16@gmail.com. Tombol di bawah membuka situs kebijakan atau aplikasi email Anda. Tidak ada pesan yang dikirim otomatis."},
    };
    public static final String[][] SECTIONS = englishSections();

    private static String[][] englishSections() {
        String[][] rows = new String[COPY.length][2];
        for (int i=0;i<COPY.length;i++) rows[i]=new String[]{COPY[i][0],COPY[i][1]};
        return rows;
    }

    /** Called last by the shared release catalog so current disclosures override older copy. */
    public static void addTranslations(Map<String,String[]> catalog) {
        catalog.put(TITLE,new String[]{"Falley की गोपनीयता नीति","Kebijakan Privasi Falley"});
        catalog.put(PUBLISHER,new String[]{"प्रकाशक: Perf Games","Penerbit: Perf Games"});
        catalog.put(EFFECTIVE_DATE,new String[]{"लागू: 16 अक्टूबर 2026","Berlaku: 16 Oktober 2026"});
        catalog.put(UPDATED_DATE,new String[]{"अद्यतन: 29 अक्टूबर 2026","Diperbarui: 29 Oktober 2026"});
        for (String[] row:COPY) {
            catalog.put(row[0],new String[]{row[2],row[4]});
            catalog.put(row[1],new String[]{row[3],row[5]});
        }
    }

    private PrivacyPolicy() { }

    public static String fullText() {
        StringBuilder text = new StringBuilder(TITLE).append("\n").append(PUBLISHER)
                .append("\n").append(EFFECTIVE_DATE).append("\n").append(UPDATED_DATE);
        for (String[] section : SECTIONS) text.append("\n\n").append(section[0]).append("\n").append(section[1]);
        return text.toString();
    }
}
