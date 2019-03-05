package com.gt.utils;

import com.gt.data.RankData;
import com.gt.towers.constants.ResourceType;
import com.smartfoxserver.v2.entities.data.ISFSArray;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 7/22/2017.
 */
public class RankingUtils extends UtilBase
{
    static public RankingUtils getInstance()
    {
        return (RankingUtils)UtilBase.get(RankingUtils.class);
    }
    static final String[] names = new String[]{"بازی مصهب", "Axfourd", "رضا متری", "WilliamSctt", "یاسین مهیمنی", "SHOKIZIKA", "sajad1400", "علی با ادب", "SHAHNAM", "۱۳۳", "عليرضا قيم", "بنده حقیرخدا", "عشق بی احساس", "kamale", "mohad", "جواتی", "s@j@d", "گرگ اصبانی", "mohamad diyy", "¥uness¥", "پریا حرفه ای", "ali va kosar", "درامون", "MOHAMDREZA", "esmail خطر ", "محمدکوچولو", "MKADI0917", "میثم گرگی", "H,سردار", "mr.m", "امام ", "naz", "داش مشتیی", "ملکه تنهایی", "افلاطون", "سلامتی", "M**A", "ilia7689", "روژین جون", "ℳ¤ɦﾑℳℳﾑの ﾑℓɨ", "حسین شاه", "امیرملکی", "ثم ال یاسمسک", "دختر❤شهریوری", "پوریای قدرتم", "MF14", "MAFIA", "Saeid Mallah", "مهرگان", "امیریسن", "کوردم", "armin.mk", "نناجا", "نفیسه خانم", "افرا", "forozan", "eli ;)", "ruwshank", "حمیدر۱ا", "amir.kh", "نرگس ❤", "hananeh", "woto", "شهاب خطر", "MAHDI KHAN", "ali❤❤❤❤kosar", "شاه قله", "AMIR;Barca", "رونالدو عشق", "آرتا", "adib...m.r", "جنگاور ایران", "ariai", "a. khafan", "Տհɑժօա", "امیر سلطان", "m.r.akbari", "Hoomah", "کرخ مخوف", "mohmmd reza", "بوراک دنیز", "ali eblis", "sorooshking", "جنگجوی باهوش", "هبحی", "خنده", "میثم جوون", "لابباایدخهفف", "بولول", "توفان برزخ", "..محمد رضا..", "NEMAN", "جان نثار", "پارسا کابوی ", "✅mr. sorena✔", "نیما قوی", "지민 jimin", "hahka", "آرتینا رحمتی", "fariba", "masoudking", "آنیا", "G.g", "F16", "عادل تایسیز", "کریم خطر", "پارس باز", "⇡⇡⇡⇡⇡", "ahmad sadeqi", "گرگ‌خسته", "bahare", "SHAHRYAR", "Abdullah", "یاسیین", "amine84th", "آرای", "ققنوس وحشت", "آرین قرمان", "Soltan Hsein", "حميد", "اتش سرخ سفید", "Just me", "ali141414", "qwert", "King A", "❤parimah❤", "پارسا افشار", "pani", "جنگ ملکه", "ترمیناتور", "❤arsham ❤", "Queen.raha", "❤ سامان❤ ", "MGFIGHTER", "mehrsa", "محمدحسام", "killerdemon", "تک تاز", "امین ابرکوه", "javadaskari", "حیاط", "scorp", "امیر آریا", "yasy", "m......l", "اشکان خ", "Niliy", "امیراحمد", "باهوبابی", "HOSSIEN.M", "فرشاد10@", "حاجی ممد", "M∞N", "nafasssss", "MAMADO1385", "جوادخطر!!!", "سوگل جون", "هنزخ", "321", "aryan_si", "پورییا", "دوقلوها", "امین خطر", "تورک اوغلو", "هادی ۳۴", "Moein1382", "reرضاza", "ملکه نور", "Death horror", "مجتبى", "اژدهای‌قهرما", "فراموشم کن", "اشکان جان ", "ضحا جون", "حسین مغول زا", "پیشی پارساو", "Amir.K.H.", "hasan2", "میتا", "دنیای غم 48", "Amin gk 69", "măĥśá - af", "m.shayan", "saBaa", "استیو مک کار", "♥امیرعلی♥", "نینجا سیاه ", "ماهان قوی", "تند پا", "...", "fatem 02828", "sis", "Shawn Mendes", "جنگجو موفق", "m99", "←ساختار شکن→", "امیر محمدی ", "jack.sparrow", "amoo", "رعد اسا", "elham.jj", "ermi.badboy", "DSD", "vlad", "mis mahsa", "صاف دل", "رعد آتشین", "﷼ سجاد ﷼", "sefid.barfi❤", "★GHOGHNOOS★", "محمد بازیگر ", "بسیجی ۲", "محمد شیرمرد", "سلطان پویا ", "[/]/®$", "juanestundo", "Afra", "ahahin", "روکاه", "مستر تاریکی", "دختر آمازونی", "hossein009", "حریم سلطان", "helma eftkha", "کنعان", "sina110011", "Hoosin", "راکی", "ALI..83", "@rash", "pekka killer", "khoh", "داوود مغول ز", "H@$Ti", "سینا رمضانی", "arman .z", "حسین۲", "تتلتلل", "ρ£dﾑr £Liყﾑ", "تاج همه", "bita_and_lak", "nakhoda", "king HASSAN.", "TERESA", "عشق بی پایان", "taregh ", "امیر حسین رف", "رضا حسینی ", "SH.Victor", "مطالعاتی", "میثم پلنگ", "jrobfk", "amir hoossin", "@.s.n.h@.", "سربازان جنگج", "Fire_Cat", "سالار هیچکس", "مــــیم چ", "قاتل ابدی", "masuod", "رضا صبحی", "ihnbvbh", "تالاب", "MEHDI♠", "fireboy2000", "$$H.R.salh", "m.monster ir", "جن کوچولو", "amin.com", "❤نیلا❤", "شبح سياه", "حسین83", "ماهان لوتی", "ایمان پلنگ", "خااانوم", "عرفان لودر", "الکلاسیکو", "haripater", "سید حسام", "H. ", "مهمدمحدی ", "simin", "رئیس امیر", "YOUNG KING", ".-.-", "Moɦseŋ", "سینا خفن", "SR7", "ساسان بارسای", "**nazdar**", "علیرضا کشاور", "Nadi", "*shiva*", "باقری", "آنوبیس", "aylar9 ", "امیریگانه فر", "فاروق", "mahdikhan", "داش سام", "B KING", "کاخ ستم", "f.of.love", "مرداد", "jabaran", "Rick.Grz", "John snow", "IRIN BAD BOY", "(درسا)", "جان سینا", "عباس خان پهل", "ناشناش", "علی روشن", "mohamadkrim", "بیل", "دنیای غم48", "فرشته ی دانا", "دخترک شیطون ", "عشقم مهدی", "@امیر@", "ابوالفضل رمض", "aliraza12", "مرسانا", "یو سف", "[[[AROSHA]]]", "amir.7", "dokhtar", "sardar_a.m", "qing", "ARSHIA. ", "خشم تاریکی", "صنمی ریکا", "farzin.a ", "ALI SHAR SHA", "♤Lucifer♤", "مبینا ۱۳کرج", "lost in you", "...ℳﾑɦⓢﾑ....", "مهدی جووووون", "مبینا ۷۸", "ABOOLFZL ", "(king omid)", "TURNADO", "سینا۸۷", "Mohammad3728", "تنهاوعاشق", "Benyamin212", "نفس گیر", "امیر محمد یو", "❤Roya...", "* _ *", "Mahkoom", "علی و مهرداد", "LEYan.ALI", "zahra king", "هخامنش83", "ali_bit", "قصر جادو", "اسکلت آتشین", "شاه آرمان", "شاه یاشار", "عباس شیر", "مسی جون", "امیرسام خسته", "Meysam CR7", "دخی زلزله", "محمدرضا موسو", "omer jenetje", "افشین.پودرکن", "davoodd", "آتیلا", "۶اریتاب", "گردبادمرگ", "پرنسس (ماریا", "شب زده", "ادیس", "قاتل توی پر ", "*nazanin*", "من خیلی خوبم", "alirezaجی ام", "JAFAR1383", "لیوال", "arezoo.s", "A.M.E.8228", "JOKKER", "کوثر ملایی", "yeghane", "death angel", "Aarsh", "ستاره ی سیاه", "m.a.tiractur", "ali.ni", "محمد اصغری", "1383317", "فانوس", "MrMetti", "sam tiam", "آنا جوون", "rahman62", "❤Łove❤", "علی اصغر صبا", "King Diako", "kamibil", "مهیار خطر", "۱۲۴۳", "dada", "MoRiS√H", "M.dodo", "VENOM", "شخله پخله", "more16za", "god of koche", "shahab khan", "یلدا خطری", "xX maj Xx", "سلطان سلیمان", "mo.shoja", "درندگان", "فا نو س سبز", "omid 250", "Anyderx", "گرگان GG", "King abol", "Javad_Sry", "ژاکلین", "(amir.kimia)", "یوهانا", "amirali144", "Saber1385", "رل چت", "Caspian", "AMINP", "ɦﾑsｲɨ★", "Abrhim", "FSMAHDi", "F.C.M", "queen negin", "KIMIA BOROOS", "ویکتور۱۱۱", "kalara", "Carazy!", "ɦ£ŁŁ❤giЯŁ", "شمیرساز", "❤ارمیتا❤", "اسکلت سیاه", "فقط شاهزاده", "mobina joon", "عاشق دلشکسته", "❤Melika❤", "HÂĶĔŔ", "taregh.k", "جک۱۸ساله", "جمالو ", "آیهان پور اس", "amir zd", "کنیتا جون", "دختر آریایی", "$فرنوش$", "پرسپولیس ام", "moha", "yuyuile", "ehsan._.11", "جهنم", "Mahak", "S.A", "شاهین سیاه", "احسان راوی", "saeghe", "دیدی", "zeynab14", "گادفادر", "اژدربلا", "amir hosen:)", "میتراعشقه", "سفید برفی ", "❤رومینا❤", "خطر خطر خطر ", "○●●ZEUS●●○", "عرفان ...خان", "اژدهای قرمز", "مهدیه زیبا", "شهرزاد خانم❤", "سجاد خوشگله", "bolbol", "乙ﾑみrﾑ", "برنده شاهین", "فوجی", "ممد باحال", "شادی ابلیش", "ROKSANA", "داخافه", "فرمانده ی قا", "sleader32", "ارباب بازی", "ali1910", "nemikham", "little moon", "KING W KOEEN", "کشتی گیران", "زخمی.", "ghazal❤", "امیرشهریار", "amkes", "رادوین..", "nora", "♤AMIN♤", "mohtbs", "ATA TURK", "{فهیم}©", "سرزمین خوبا", "الیار میرزای", "matinmahla", "shokolati3", "سوشا", "MH.ESTEGHLAL", "مهراب داغون", "Sofea", "باران خطر", "BMA", "محکوم", "احسان007", "علیرضا پلنگ", "طوفان عظیم", "SAKO", "courtois", "BORHAN.J", "₩yasin₩", "ktce", "فرمانده جاک", "❤ماریا❤", "محمدامین ناب", "گرگ دل", "AMIR BAX", "Omid.me", "گلنار ۱۴", "m.khademi83", "M.J.P", "meshki", "بازمانده#", "ŃÁÚĞĤŤŶ", "پارسا خسروی", "tokchin", "S$T$A$R", "J.O.K.E.R", "IR#MAD.MAX", "فرهین", "KevinSpacy1", "کاشفی", "Zari_goli", "♥♡ραяოiԃα♡♥~", "عشقم OMID", "eliiika", "فلسطین", "اقای ایکس", "spider_man", "خسرو", "چیتای گمشده", "×♡×ƏŁÏÝÆß×♡×", "J.A", "آرین واهورا", "khosh_andam", "goli", "♠bad_boy♠", "DicTatoR", "Pouria_m12", "حاکم جنگ ", "amirabbas86", "michael", "رعد آسمانی", "ali.gh.78", "Mahammad", "پارسا بغدبا", "داش سینا", "persian kill", "black down", "MHA1381", "مبارزه ابدی", "♥Giяℓعاشق", "theat angel", "mjsh", "Ali1386", "amir.r.t", "رضا یادگاری", "mohammad ebz", "A.KH.W", "شکییا", "خدابخش ", "اژدهای جهنمی", "@arezoo.s", "مریم گل", "ملکه ی عشق", "melodi", "fazi cyrus", "Aryats", "V•R", "×{MR}×CRAZY×", "حرفه ای منم", "سحر زمانیان", "❤TaRa❤", "ARIA.021"};
    public ConcurrentHashMap<Integer, RankData> getUsers() {
        return (ConcurrentHashMap<Integer, RankData>) ext.getParentZone().getProperty("ranking");
    }

    public void fillActives()
    {
        if( ext.getParentZone().containsProperty("ranking") )
            return;

        ConcurrentHashMap<Integer, RankData> users = new ConcurrentHashMap();

        // fill top players
        String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type = " + ResourceType.R2_POINT + " AND resources.count > 0";
        ISFSArray dbResult = null;
        try {
            dbResult = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        for( int p=0; p < dbResult.size(); p++ )
            users.put(dbResult.getSFSObject(p).getInt("id"), new RankData(dbResult.getSFSObject(p).getUtfString("name"), dbResult.getSFSObject(p).getInt("count")));
        trace("filled in-memory tops in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds. -> uers:", users.size());

        // fill fake tops
        /*int[] points = new int[]{ 6, 32, 0, 8, 14, 9, 21, 18, 4, 14, 6, 11, 24, 30, 12, 29, 22, 11, 9, 17, 32, 7, 31, 22, 11, 0, 29, 34, 17, 6, 19, 30, 8, 27, 3, 3, 2, 24, 33, 9, 21, 15, 3, 11, 4, 26, 20, 31, 21, 0, 33, 34, 31, 32, 24, 3, 23, 34, 30, 23, 20, 30, 9, 28, 23, 9, 15, 23, 1, 22, 31, 20, 24, 31, 67, 73, 73, 53, 62, 56, 65, 66, 74, 83, 70, 82, 68, 64, 53, 51, 78, 82, 81, 63, 62, 70, 52, 67, 65, 72, 78, 54, 75, 64, 65, 70, 76, 53, 61, 71, 71, 73, 56, 60, 74, 69, 83, 65, 80, 75, 72, 57, 70, 56, 62, 61, 79, 67, 61, 65, 60, 77, 80, 81, 74, 70, 75, 83, 84, 83, 62, 70, 83, 80, 62, 81, 55, 57, 148, 111, 141, 105, 154, 145, 146, 102, 126, 144, 137, 133, 115, 112, 107, 102, 139, 154, 102, 107, 126, 131, 154, 126, 110, 126, 132, 107, 120, 133, 128, 151, 115, 120, 145, 138, 104, 106, 123, 113, 117, 135, 132, 152, 153, 139, 143, 152, 109, 139, 144, 117, 121, 106, 152, 114, 113, 144, 132, 102, 143, 125, 139, 144, 128, 120, 148, 120, 147, 118, 143, 150, 123, 145, 212, 213, 184, 215, 217, 201, 207, 201, 187, 182, 172, 220, 178, 177, 187, 189, 172, 202, 174, 208, 181, 176, 208, 202, 217, 207, 180, 201, 177, 201, 179, 220, 172, 187, 209, 220, 202, 186, 185, 186, 192, 222, 182, 195, 176, 174, 181, 193, 180, 185, 196, 184, 223, 189, 179, 176, 178, 186, 195, 175, 194, 215, 197, 216, 188, 190, 183, 216, 173, 223, 207, 178, 196, 171, 294, 277, 259, 262, 273, 246, 244, 284, 287, 258, 264, 260, 251, 275, 281, 243, 277, 282, 279, 266, 265, 271, 280, 244, 269, 254, 250, 243, 293, 280, 268, 250, 264, 294, 242, 280, 251, 259, 275, 275, 254, 283, 244, 280, 291, 241, 262, 283, 255, 280, 242, 271, 269, 278, 245, 265, 242, 251, 267, 276, 289, 292, 257, 289, 283, 252, 242, 257, 281, 260, 269, 284, 264, 257, 390, 379, 368, 385, 453, 326, 397, 464, 379, 325, 405, 411, 449, 339, 378, 344, 435, 422, 450, 446, 429, 411, 407, 367, 361, 433, 392, 356, 448, 344, 397, 463, 408, 444, 474, 327, 461, 454, 325, 356, 427, 477, 355, 355, 459, 345, 362, 373, 473, 326, 343, 371, 431, 443, 407, 390, 356, 466, 342, 378, 367, 456, 455, 378, 461, 400, 423, 346, 452, 379, 320, 467, 331, 326, 530, 547, 521, 542, 665, 504, 620, 645, 556, 526, 556, 672, 576, 593, 542, 547, 580, 525, 564, 656, 611, 679, 563, 559, 678, 654, 535, 638, 522, 670, 665, 607, 660, 528, 516, 678, 664, 588, 529, 619, 567, 590, 606, 550, 645, 673, 651, 527, 515, 511, 637, 593, 555, 627, 674, 563, 506, 612, 505, 589, 647, 614, 562, 565, 518, 651, 679, 574, 582, 680, 624, 675, 628, 679, 903, 920, 954, 765, 878, 783, 801, 883, 733, 746, 754, 743, 793, 794, 899, 964, 787, 919, 845, 919, 893, 806, 758, 705, 709, 861, 701, 764, 929, 802, 957, 708, 802, 829, 907, 727, 880, 941, 898, 718, 961, 923, 780, 859, 946, 932, 739, 826, 806, 967, 820, 815, 763, 824, 870, 922, 828, 797, 932, 718, 951, 743, 897, 828, 737, 708, 867, 739, 843, 736, 740, 898, 856, 745, 1342, 1224, 1087, 1172, 1061, 1129, 1176, 1173, 1230, 1073, 1143, 1267, 1157, 1066, 1162, 1081, 1248, 1170, 1198, 1028, 1116, 1164, 1118, 1025, 1283, 1154, 1148, 1205, 1292, 1200, 1311, 1318, 1176, 1013, 1161, 1006, 1212, 1011, 1283, 1257, 1120, 1037, 1303, 1196, 1330, 1264, 1154, 1005, 1160, 1154, 1284, 1345, 1185, 1151, 1093, 1184, 1310, 1115, 1350, 1357, 1202, 1170, 1009, 1321, 1090, 1018, 1278, 1296, 1011, 1338, 1004, 1011, 1296, 1092, 6852, 1508, 3365, 7385, 7360, 1941, 5467, 3250, 4403, 8179, 5583, 3179, 6493, 6295, 7653, 7033, 1503, 4406, 8382, 4520, 8609, 2755, 4627, 1669, 7200, 7387, 4555, 7091, 7219, 2669, 6287, 8805, 2973, 6248, 4252, 9121, 3922, 3412, 7317, 2409, 5076, 9073, 4192, 4766, 1528, 6680, 2252, 6062, 2114, 8529, 5734, 6235, 8606, 6830, 4468, 1958, 2621, 1537, 8882, 3882, 4944, 4809, 4240, 5976, 1971, 7619, 5001, 8512, 7371, 7676, 5719, 4616, 5186, 6636 };

        int start = 12;
        for ( int n = start, i = points.length-1;  n < RankRequestHandler.PAGE_SIZE + start;  n++, i-=2 )
            users.put(n - start , new RankData(names[n], points[i], -1, -1));
        trace("filled in-memory bots in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");*/

        ext.getParentZone().setProperty("ranking", users);
    }

    public String getRandomName()
    {
        return names[(int) (names.length * Math.random())];
    }

/*    public void fillStatistics()
    {
        String time = "'2018-09-09 00:00:00' AND '2018-09-16 00:00:00'";
        Map<Integer, Map<Integer, Object>> statistics = new HashMap();
        getResourceStats(statistics, ResourceType.R1_XP, time);
        getResourceStats(statistics, ResourceType.R2_POINT, time);
        getResourceStats(statistics, ResourceType.R3_CURRENCY_SOFT, time);
        getResourceStats(statistics, ResourceType.R4_CURRENCY_HARD, time);
        getResourceStats(statistics, ResourceType.R12_BATTLES, time);
        getResourceStats(statistics, ResourceType.R13_BATTLES_WINS, time);
        getResourceStats(statistics, ResourceType.R21_BOOK_OPENED_BATTLE, time);
        getResourceStats(statistics, ResourceType.R22_BOOK_OPENED_FREE, time);
        getCardStats(statistics, CardTypes.C101, time);
        getCardStats(statistics, CardTypes.C102, time);
        getCardStats(statistics, CardTypes.C103, time);
        getCardStats(statistics, CardTypes.C104, time);
        getCardStats(statistics, CardTypes.C105, time);
        getCardStats(statistics, CardTypes.C106, time);
        getCardStats(statistics, CardTypes.C107, time);
        getCardStats(statistics, CardTypes.C108, time);
        getCardStats(statistics, CardTypes.C109, time);
        getCardStats(statistics, CardTypes.C110, time);
        getCardStats(statistics, CardTypes.C111, time);

        getCardStats(statistics, CardTypes.C151, time);
        getCardStats(statistics, CardTypes.C152, time);
        getOperationStats(statistics, time);

        String stats = "";
        for (Map.Entry<Integer, Map<Integer, Object>> entry : statistics.entrySet() )
        {
            stats += entry.getKey() + ",";
            Map<Integer, Object> d = entry.getValue();

            stats += (   d.containsKey(ResourceType.R1_XP)         ? d.get(ResourceType.R1_XP) : 0 )+","+
            (   d.containsKey(ResourceType.R2_POINT)               ? d.get(ResourceType.R2_POINT) : 0 )+","+
            (   d.containsKey(ResourceType.R3_CURRENCY_SOFT)       ? d.get(ResourceType.R3_CURRENCY_SOFT) : 0 )+","+
            (   d.containsKey(ResourceType.R4_CURRENCY_HARD)       ? d.get(ResourceType.R4_CURRENCY_HARD) : 0 )+","+
            (   d.containsKey(ResourceType.R12_BATTLES)             ? d.get(ResourceType.R12_BATTLES) : 0 )+","+
            (   d.containsKey(ResourceType.R13_BATTLES_WINS)        ? d.get(ResourceType.R13_BATTLES_WINS) : 0 )+","+
            (   d.containsKey(ResourceType.R21_BOOK_OPENED_BATTLE)  ? d.get(ResourceType.R21_BOOK_OPENED_BATTLE) : 0 )+","+
            (   d.containsKey(ResourceType.R22_BOOK_OPENED_FREE)    ? d.get(ResourceType.R22_BOOK_OPENED_FREE) : 0 )+","+
            (   d.containsKey(201)                              ? d.get(201) : 0 )+","+
            (   d.containsKey(202)                              ? d.get(202) : 0 )+",,,";

            stats += getCardValues(entry, d,  CardTypes.C101, ",,");
            stats += getCardValues(entry, d,  CardTypes.C102, ",,");
            stats += getCardValues(entry, d,  CardTypes.C103, ",,");
            stats += getCardValues(entry, d,  CardTypes.C104, ",,");
            stats += getCardValues(entry, d,  CardTypes.C105, ",,");
            stats += getCardValues(entry, d,  CardTypes.C106, ",,");
            stats += getCardValues(entry, d,  CardTypes.C107, ",,");
            stats += getCardValues(entry, d,  CardTypes.C108, ",,");
            stats += getCardValues(entry, d,  CardTypes.C109, ",,");
            stats += getCardValues(entry, d,  CardTypes.C110, ",,");
            stats += getCardValues(entry, d,  CardTypes.C111, ",,");

            stats += getCardValues(entry, d,  CardTypes.C151, ",,");
            stats += getCardValues(entry, d,  CardTypes.C152, ",\n");
        }

        trace(stats);
    }

    private String getCardValues(Map.Entry<Integer,Map<Integer,Object>> entry, Map<Integer, Object> d, int type, String post)
    {
        if( d.containsKey(type) )
            return ((RankData)d.get(type)).point + "," + ((RankData)d.get(type)).xp + post;
        return "0,0" + post;
    }


    private void getCardStats(Map<Integer,Map<Integer,Object>> statistics, int key, String time)
    {
        String query = "SELECT players.id, resources.count, resources.level FROM players INNER JOIN resources ON players.id = resources.player_id WHERE (players.create_at BETWEEN " + time + ") AND resources.type = " + key;
        ISFSArray players = null;
        try {
            players = ext.getParentZone().getDBManager().executeQuery(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

        for (int i = 0; i < players.size(); i++ )
        {
            if( !statistics.containsKey( players.getSFSObject(i).getInt("id") ) )
                statistics.put(players.getSFSObject(i).getInt("id"), new HashMap());
            statistics.get(players.getSFSObject(i).getInt("id")).put(key, new RankData(0,"", players.getSFSObject(i).getInt("count"), players.getSFSObject(i).getInt("level")));
        }
        trace(key + " get in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    private void getResourceStats(Map<Integer, Map<Integer, Object>> statistics, int key, String time)
    {
        String query = "SELECT players.id, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE (players.create_at BETWEEN " + time + ") AND resources.type = " + key;
        ISFSArray players = null;
        try {
            players = ext.getParentZone().getDBManager().executeQuery(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

        for (int i = 0; i < players.size(); i++ )
        {
            if( !statistics.containsKey( players.getSFSObject(i).getInt("id") ) )
                statistics.put(players.getSFSObject(i).getInt("id"), new HashMap());
            statistics.get(players.getSFSObject(i).getInt("id")).put(key, players.getSFSObject(i).getInt("count"));
        }
        trace(key + " get in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    private void getOperationStats(Map<Integer,Map<Integer,Object>> statistics, String time)
    {
        String query = "SELECT players.id, MAX(operations.`index` ) as i, AVG(operations.score) as s FROM players INNER JOIN operations ON players.id = operations.player_id WHERE (players.create_at BETWEEN " + time + ") group by players.id;";
        ISFSArray players = null;
        try {
            players = ext.getParentZone().getDBManager().executeQuery(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

        for (int i = 0; i < players.size(); i++ )
        {
            if( !statistics.containsKey( players.getSFSObject(i).getInt("id") ) )
                statistics.put(players.getSFSObject(i).getInt("id"), new HashMap());
            statistics.get(players.getSFSObject(i).getInt("id")).put(201, players.getSFSObject(i).getInt("i"));
            statistics.get(players.getSFSObject(i).getInt("id")).put(202, (int) (players.getSFSObject(i).getDouble("s")*10));
        }
        trace("operations get in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }*/
}

