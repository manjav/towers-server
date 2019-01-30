package com.gerantech.towers.sfs.utils;

import com.gerantech.towers.sfs.handlers.RankRequestHandler;
import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.others.Arena;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.util.RandomPicker;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ManJav on 7/22/2017.
 */
public class RankingUtils
{
    private static RankingUtils _instance;
    private final SFSExtension ext;
    private static final String[] names = new String[]{"بازی مصهب", "Axfourd", "رضا متری", "WilliamSctt", "یاسین مهیمنی", "SHOKIZIKA", "sajad1400", "علی با ادب", "SHAHNAM", "۱۳۳", "عليرضا قيم", "بنده حقیرخدا", "عشق بی احساس", "kamale", "mohad", "جواتی", "s@j@d", "گرگ اصبانی", "mohamad diyy", "¥uness¥", "پریا حرفه ای", "ali va kosar", "درامون", "MOHAMDREZA", "esmail خطر ", "محمدکوچولو", "MKADI0917", "میثم گرگی", "H,سردار", "mr.m", "امام ", "naz", "داش مشتیی", "ملکه تنهایی", "افلاطون", "سلامتی", "M**A", "ilia7689", "روژین جون", "ℳ¤ɦﾑℳℳﾑの ﾑℓɨ", "حسین شاه", "امیرملکی", "ثم ال یاسمسک", "دختر❤شهریوری", "پوریای قدرتم", "MF14", "MAFIA", "Saeid Mallah", "مهرگان", "امیریسن", "کوردم", "armin.mk", "نناجا", "نفیسه خانم", "افرا", "forozan", "eli ;)", "ruwshank", "حمیدر۱ا", "amir.kh", "نرگس ❤", "hananeh", "woto", "شهاب خطر", "MAHDI KHAN", "ali❤❤❤❤kosar", "شاه قله", "AMIR;Barca", "رونالدو عشق", "آرتا", "adib...m.r", "جنگاور ایران", "ariai", "a. khafan", "Տհɑժօա", "امیر سلطان", "m.r.akbari", "Hoomah", "کرخ مخوف", "mohmmd reza", "بوراک دنیز", "ali eblis", "sorooshking", "جنگجوی باهوش", "هبحی", "خنده", "میثم جوون", "لابباایدخهفف", "بولول", "توفان برزخ", "..محمد رضا..", "NEMAN", "جان نثار", "پارسا کابوی ", "✅mr. sorena✔", "نیما قوی", "지민 jimin", "hahka", "آرتینا رحمتی", "fariba", "masoudking", "آنیا", "G.g", "F16", "عادل تایسیز", "کریم خطر", "پارس باز", "⇡⇡⇡⇡⇡", "ahmad sadeqi", "گرگ‌خسته", "bahare", "SHAHRYAR", "Abdullah", "یاسیین", "amine84th", "آرای", "ققنوس وحشت", "آرین قرمان", "Soltan Hsein", "حميد", "اتش سرخ سفید", "Just me", "ali141414", "qwert", "King A", "❤parimah❤", "پارسا افشار", "pani", "جنگ ملکه", "ترمیناتور", "❤arsham ❤", "Queen.raha", "❤ سامان❤ ", "MGFIGHTER", "mehrsa", "محمدحسام", "killerdemon", "تک تاز", "امین ابرکوه", "javadaskari", "حیاط", "scorp", "امیر آریا", "yasy", "m......l", "اشکان خ", "Niliy", "امیراحمد", "باهوبابی", "HOSSIEN.M", "فرشاد10@", "حاجی ممد", "M∞N", "nafasssss", "MAMADO1385", "جوادخطر!!!", "سوگل جون", "هنزخ", "321", "aryan_si", "پورییا", "دوقلوها", "امین خطر", "تورک اوغلو", "هادی ۳۴", "Moein1382", "reرضاza", "ملکه نور", "Death horror", "مجتبى", "اژدهای‌قهرما", "فراموشم کن", "اشکان جان ", "ضحا جون", "حسین مغول زا", "پیشی پارساو", "Amir.K.H.", "hasan2", "میتا", "دنیای غم 48", "Amin gk 69", "măĥśá - af", "m.shayan", "saBaa", "استیو مک کار", "♥امیرعلی♥", "نینجا سیاه ", "ماهان قوی", "تند پا", "...", "fatem 02828", "sis", "Shawn Mendes", "جنگجو موفق", "m99", "←ساختار شکن→", "امیر محمدی ", "jack.sparrow", "amoo", "رعد اسا", "elham.jj", "ermi.badboy", "DSD", "vlad", "mis mahsa", "صاف دل", "رعد آتشین", "﷼ سجاد ﷼", "sefid.barfi❤", "★GHOGHNOOS★", "محمد بازیگر ", "بسیجی ۲", "محمد شیرمرد", "سلطان پویا ", "[/]/®$", "juanestundo", "Afra", "ahahin", "روکاه", "مستر تاریکی", "دختر آمازونی", "hossein009", "حریم سلطان", "helma eftkha", "کنعان", "sina110011", "Hoosin", "راکی", "ALI..83", "@rash", "pekka killer", "khoh", "داوود مغول ز", "H@$Ti", "سینا رمضانی", "arman .z", "حسین۲", "تتلتلل", "ρ£dﾑr £Liყﾑ", "تاج همه", "bita_and_lak", "nakhoda", "king HASSAN.", "TERESA", "عشق بی پایان", "taregh ", "امیر حسین رف", "رضا حسینی ", "SH.Victor", "مطالعاتی", "میثم پلنگ", "jrobfk", "amir hoossin", "@.s.n.h@.", "سربازان جنگج", "Fire_Cat", "سالار هیچکس", "مــــیم چ", "قاتل ابدی", "masuod", "رضا صبحی", "ihnbvbh", "تالاب", "MEHDI♠", "fireboy2000", "$$H.R.salh", "m.monster ir", "جن کوچولو", "amin.com", "❤نیلا❤", "شبح سياه", "حسین83", "ماهان لوتی", "ایمان پلنگ", "خااانوم", "عرفان لودر", "الکلاسیکو", "haripater", "سید حسام", "H. ", "مهمدمحدی ", "simin", "رئیس امیر", "YOUNG KING", ".-.-", "Moɦseŋ", "سینا خفن", "SR7", "ساسان بارسای", "**nazdar**", "علیرضا کشاور", "Nadi", "*shiva*", "باقری", "آنوبیس", "aylar9 ", "امیریگانه فر", "فاروق", "mahdikhan", "داش سام", "B KING", "کاخ ستم", "f.of.love", "مرداد", "jabaran", "Rick.Grz", "John snow", "IRIN BAD BOY", "(درسا)", "جان سینا", "عباس خان پهل", "ناشناش", "علی روشن", "mohamadkrim", "بیل", "دنیای غم48", "فرشته ی دانا", "دخترک شیطون ", "عشقم مهدی", "@امیر@", "ابوالفضل رمض", "aliraza12", "مرسانا", "یو سف", "[[[AROSHA]]]", "amir.7", "dokhtar", "sardar_a.m", "qing", "ARSHIA. ", "خشم تاریکی", "صنمی ریکا", "farzin.a ", "ALI SHAR SHA", "♤Lucifer♤", "مبینا ۱۳کرج", "lost in you", "...ℳﾑɦⓢﾑ....", "مهدی جووووون", "مبینا ۷۸", "ABOOLFZL ", "(king omid)", "TURNADO", "سینا۸۷", "Mohammad3728", "تنهاوعاشق", "Benyamin212", "نفس گیر", "امیر محمد یو", "❤Roya...", "* _ *", "Mahkoom", "علی و مهرداد", "LEYan.ALI", "zahra king", "هخامنش83", "ali_bit", "قصر جادو", "اسکلت آتشین", "شاه آرمان", "شاه یاشار", "عباس شیر", "مسی جون", "امیرسام خسته", "Meysam CR7", "دخی زلزله", "محمدرضا موسو", "omer jenetje", "افشین.پودرکن", "davoodd", "آتیلا", "۶اریتاب", "گردبادمرگ", "پرنسس (ماریا", "شب زده", "ادیس", "قاتل توی پر ", "*nazanin*", "من خیلی خوبم", "alirezaجی ام", "JAFAR1383", "لیوال", "arezoo.s", "A.M.E.8228", "JOKKER", "کوثر ملایی", "yeghane", "death angel", "Aarsh", "ستاره ی سیاه", "m.a.tiractur", "ali.ni", "محمد اصغری", "1383317", "فانوس", "MrMetti", "sam tiam", "آنا جوون", "rahman62", "❤Łove❤", "علی اصغر صبا", "King Diako", "kamibil", "مهیار خطر", "۱۲۴۳", "dada", "MoRiS√H", "M.dodo", "VENOM", "شخله پخله", "more16za", "god of koche", "shahab khan", "یلدا خطری", "xX maj Xx", "سلطان سلیمان", "mo.shoja", "درندگان", "فا نو س سبز", "omid 250", "Anyderx", "گرگان GG", "King abol", "Javad_Sry", "ژاکلین", "(amir.kimia)", "یوهانا", "amirali144", "Saber1385", "رل چت", "Caspian", "AMINP", "ɦﾑsｲɨ★", "Abrhim", "FSMAHDi", "F.C.M", "queen negin", "KIMIA BOROOS", "ویکتور۱۱۱", "kalara", "Carazy!", "ɦ£ŁŁ❤giЯŁ", "شمیرساز", "❤ارمیتا❤", "اسکلت سیاه", "فقط شاهزاده", "mobina joon", "عاشق دلشکسته", "❤Melika❤", "HÂĶĔŔ", "taregh.k", "جک۱۸ساله", "جمالو ", "آیهان پور اس", "amir zd", "کنیتا جون", "دختر آریایی", "$فرنوش$", "پرسپولیس ام", "moha", "yuyuile", "ehsan._.11", "جهنم", "Mahak", "S.A", "شاهین سیاه", "احسان راوی", "saeghe", "دیدی", "zeynab14", "گادفادر", "اژدربلا", "amir hosen:)", "میتراعشقه", "سفید برفی ", "❤رومینا❤", "خطر خطر خطر ", "○●●ZEUS●●○", "عرفان ...خان", "اژدهای قرمز", "مهدیه زیبا", "شهرزاد خانم❤", "سجاد خوشگله", "bolbol", "乙ﾑみrﾑ", "برنده شاهین", "فوجی", "ممد باحال", "شادی ابلیش", "ROKSANA", "داخافه", "فرمانده ی قا", "sleader32", "ارباب بازی", "ali1910", "nemikham", "little moon", "KING W KOEEN", "کشتی گیران", "زخمی.", "ghazal❤", "امیرشهریار", "amkes", "رادوین..", "nora", "♤AMIN♤", "mohtbs", "ATA TURK", "{فهیم}©", "سرزمین خوبا", "الیار میرزای", "matinmahla", "shokolati3", "سوشا", "MH.ESTEGHLAL", "مهراب داغون", "Sofea", "باران خطر", "BMA", "محکوم", "احسان007", "علیرضا پلنگ", "طوفان عظیم", "SAKO", "courtois", "BORHAN.J", "₩yasin₩", "ktce", "فرمانده جاک", "❤ماریا❤", "محمدامین ناب", "گرگ دل", "AMIR BAX", "Omid.me", "گلنار ۱۴", "m.khademi83", "M.J.P", "meshki", "بازمانده#", "ŃÁÚĞĤŤŶ", "پارسا خسروی", "tokchin", "S$T$A$R", "J.O.K.E.R", "IR#MAD.MAX", "فرهین", "KevinSpacy1", "کاشفی", "Zari_goli", "♥♡ραяოiԃα♡♥~", "عشقم OMID", "eliiika", "فلسطین", "اقای ایکس", "spider_man", "خسرو", "چیتای گمشده", "×♡×ƏŁÏÝÆß×♡×", "J.A", "آرین واهورا", "khosh_andam", "goli", "♠bad_boy♠", "DicTatoR", "Pouria_m12", "حاکم جنگ ", "amirabbas86", "michael", "رعد آسمانی", "ali.gh.78", "Mahammad", "پارسا بغدبا", "داش سینا", "persian kill", "black down", "MHA1381", "مبارزه ابدی", "♥Giяℓعاشق", "theat angel", "mjsh", "Ali1386", "amir.r.t", "رضا یادگاری", "mohammad ebz", "A.KH.W", "شکییا", "خدابخش ", "اژدهای جهنمی", "@arezoo.s", "مریم گل", "ملکه ی عشق", "melodi", "fazi cyrus", "Aryats", "V•R", "×{MR}×CRAZY×", "حرفه ای منم", "سحر زمانیان", "❤TaRa❤", "ARIA.021"};


    public RankingUtils()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static RankingUtils getInstance()
    {
        if( _instance == null )
            _instance = new RankingUtils();
        return _instance;
    }

    public void fillActives()
    {
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        if( users.size() > 0 )
            return;
        ext.trace("start in-memory filling in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
        // insert real champions
        try
        {
            IDBManager dbManager = ext.getParentZone().getDBManager();

            // fill active players
            String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type = " + ResourceType.R14_BATTLES_WEEKLY + " AND resources.count > 0";
            ISFSArray dbResult = dbManager.executeQuery(query, new Object[] {});
            for( int p=0; p<dbResult.size(); p++ )
            {
                ISFSObject pp = dbResult.getSFSObject(p);
                users.put(pp.getInt("id"), new RankData(pp.getUtfString("name"), 0, pp.getInt("count"), 0));
            }
            ext.trace("filled in-memory actives in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");

            // fill top players
            query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type = " + ResourceType.R2_POINT + " AND resources.count > 0";
            dbResult = dbManager.executeQuery(query, new Object[]{});
            for( int p=0; p < dbResult.size(); p++ )
            {
                ISFSObject pp = dbResult.getSFSObject(p);
                if( users.containsKey(pp.getInt("id")) )
                {
                    RankData rd = users.get(pp.getInt("id"));
                    rd.point = pp.getInt("count");
                    users.replace(pp.getInt("id"), rd);
                }
                else
                {
                    users.put(pp.getInt("id"), new RankData(pp.getUtfString("name"), pp.getInt("count"), 0, 0));
                }
            }
            ext.trace("filled in-memory tops in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.", users.size());

            // fill stars of players
            query = "SELECT resources.player_id, resources.count FROM resources WHERE resources.type = " + ResourceType.R18_STARS_WEEKLY + " AND resources.count > 0";
            dbResult = dbManager.executeQuery(query, new Object[]{});
            for( int p=0; p < dbResult.size(); p++ )
            {
                ISFSObject pp = dbResult.getSFSObject(p);
                if( users.containsKey(pp.getInt("player_id")) )
                {
                    RankData rd = users.get(pp.getInt("player_id"));
                    rd.weeklyStars = pp.getInt("count");
                    users.replace(pp.getInt("player_id"), rd);
                }
            }
            ext.trace("filled in-memory stars in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.", users.size());
        } catch (SQLException e) { e.printStackTrace(); }

        // insert npcs
        //logRandomBots(game);
        int[] points = new int[]{ 6, 32, 0, 8, 14, 9, 21, 18, 4, 14, 6, 11, 24, 30, 12, 29, 22, 11, 9, 17, 32, 7, 31, 22, 11, 0, 29, 34, 17, 6, 19, 30, 8, 27, 3, 3, 2, 24, 33, 9, 21, 15, 3, 11, 4, 26, 20, 31, 21, 0, 33, 34, 31, 32, 24, 3, 23, 34, 30, 23, 20, 30, 9, 28, 23, 9, 15, 23, 1, 22, 31, 20, 24, 31, 67, 73, 73, 53, 62, 56, 65, 66, 74, 83, 70, 82, 68, 64, 53, 51, 78, 82, 81, 63, 62, 70, 52, 67, 65, 72, 78, 54, 75, 64, 65, 70, 76, 53, 61, 71, 71, 73, 56, 60, 74, 69, 83, 65, 80, 75, 72, 57, 70, 56, 62, 61, 79, 67, 61, 65, 60, 77, 80, 81, 74, 70, 75, 83, 84, 83, 62, 70, 83, 80, 62, 81, 55, 57, 148, 111, 141, 105, 154, 145, 146, 102, 126, 144, 137, 133, 115, 112, 107, 102, 139, 154, 102, 107, 126, 131, 154, 126, 110, 126, 132, 107, 120, 133, 128, 151, 115, 120, 145, 138, 104, 106, 123, 113, 117, 135, 132, 152, 153, 139, 143, 152, 109, 139, 144, 117, 121, 106, 152, 114, 113, 144, 132, 102, 143, 125, 139, 144, 128, 120, 148, 120, 147, 118, 143, 150, 123, 145, 212, 213, 184, 215, 217, 201, 207, 201, 187, 182, 172, 220, 178, 177, 187, 189, 172, 202, 174, 208, 181, 176, 208, 202, 217, 207, 180, 201, 177, 201, 179, 220, 172, 187, 209, 220, 202, 186, 185, 186, 192, 222, 182, 195, 176, 174, 181, 193, 180, 185, 196, 184, 223, 189, 179, 176, 178, 186, 195, 175, 194, 215, 197, 216, 188, 190, 183, 216, 173, 223, 207, 178, 196, 171, 294, 277, 259, 262, 273, 246, 244, 284, 287, 258, 264, 260, 251, 275, 281, 243, 277, 282, 279, 266, 265, 271, 280, 244, 269, 254, 250, 243, 293, 280, 268, 250, 264, 294, 242, 280, 251, 259, 275, 275, 254, 283, 244, 280, 291, 241, 262, 283, 255, 280, 242, 271, 269, 278, 245, 265, 242, 251, 267, 276, 289, 292, 257, 289, 283, 252, 242, 257, 281, 260, 269, 284, 264, 257, 390, 379, 368, 385, 453, 326, 397, 464, 379, 325, 405, 411, 449, 339, 378, 344, 435, 422, 450, 446, 429, 411, 407, 367, 361, 433, 392, 356, 448, 344, 397, 463, 408, 444, 474, 327, 461, 454, 325, 356, 427, 477, 355, 355, 459, 345, 362, 373, 473, 326, 343, 371, 431, 443, 407, 390, 356, 466, 342, 378, 367, 456, 455, 378, 461, 400, 423, 346, 452, 379, 320, 467, 331, 326, 530, 547, 521, 542, 665, 504, 620, 645, 556, 526, 556, 672, 576, 593, 542, 547, 580, 525, 564, 656, 611, 679, 563, 559, 678, 654, 535, 638, 522, 670, 665, 607, 660, 528, 516, 678, 664, 588, 529, 619, 567, 590, 606, 550, 645, 673, 651, 527, 515, 511, 637, 593, 555, 627, 674, 563, 506, 612, 505, 589, 647, 614, 562, 565, 518, 651, 679, 574, 582, 680, 624, 675, 628, 679, 903, 920, 954, 765, 878, 783, 801, 883, 733, 746, 754, 743, 793, 794, 899, 964, 787, 919, 845, 919, 893, 806, 758, 705, 709, 861, 701, 764, 929, 802, 957, 708, 802, 829, 907, 727, 880, 941, 898, 718, 961, 923, 780, 859, 946, 932, 739, 826, 806, 967, 820, 815, 763, 824, 870, 922, 828, 797, 932, 718, 951, 743, 897, 828, 737, 708, 867, 739, 843, 736, 740, 898, 856, 745, 1342, 1224, 1087, 1172, 1061, 1129, 1176, 1173, 1230, 1073, 1143, 1267, 1157, 1066, 1162, 1081, 1248, 1170, 1198, 1028, 1116, 1164, 1118, 1025, 1283, 1154, 1148, 1205, 1292, 1200, 1311, 1318, 1176, 1013, 1161, 1006, 1212, 1011, 1283, 1257, 1120, 1037, 1303, 1196, 1330, 1264, 1154, 1005, 1160, 1154, 1284, 1345, 1185, 1151, 1093, 1184, 1310, 1115, 1350, 1357, 1202, 1170, 1009, 1321, 1090, 1018, 1278, 1296, 1011, 1338, 1004, 1011, 1296, 1092, 6852, 1508, 3365, 7385, 7360, 1941, 5467, 3250, 4403, 8179, 5583, 3179, 6493, 6295, 7653, 7033, 1503, 4406, 8382, 4520, 8609, 2755, 4627, 1669, 7200, 7387, 4555, 7091, 7219, 2669, 6287, 8805, 2973, 6248, 4252, 9121, 3922, 3412, 7317, 2409, 5076, 9073, 4192, 4766, 1528, 6680, 2252, 6062, 2114, 8529, 5734, 6235, 8606, 6830, 4468, 1958, 2621, 1537, 8882, 3882, 4944, 4809, 4240, 5976, 1971, 7619, 5001, 8512, 7371, 7676, 5719, 4616, 5186, 6636 };

        int start = 12;
        for ( int n = start, i = points.length-1;  n < RankRequestHandler.PAGE_SIZE + start;  n++, i-=2 )
            users.put(n - start , new RankData(names[n], points[i], -1, -1));
        ext.trace("filled in-memory bots in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    public RankData getNearOpponent(IMap<Integer, RankData> users, int point, int range)
    {
        Collection<RankData> result = getResult(users, point, range);
        int num = (int) (Math.random() * result.size());
        for(RankData t: result)
            if (--num < 0) {
                t.name = names[RandomPicker.getInt(names.length)];
                return t;
            }
        throw new AssertionError();
    }
    private Collection<RankData> getResult(IMap<Integer, RankData> users, int point, int range)
    {
        EntryObject eo = new PredicateBuilder().getEntryObject();
        Predicate sqlQuery = eo.get("point").between(point-range, point+range).and(eo.get("point").notEqual(point)).and(eo.get("xp").equal(-1));

        // a comparator which helps to sort in descending order of point field
        Comparator<Map.Entry> descendingComparator = new Comparator<Map.Entry>() {
            public int compare(Map.Entry e1, Map.Entry e2) {
                RankData s1 = (RankData) e1.getValue();
                RankData s2 = (RankData) e2.getValue();
                return s2.point - s1.point;
            }
        };
        PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, descendingComparator, 20);

        Collection<RankData> result = users.values(pagingPredicate);

        if(result.size() == 0 && range < 500)
            result = getResult(users, point, range * 2);

        return result;
    }

    public void setWeeklyBattles(int id, int battles)
    {
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        RankData opponent = users.get(id);
        opponent.weeklyBattles = battles;
        users.replace(id, opponent);
    }


    public List<RankData> getFakedNearMeRanking(IMap<Integer, RankData> users, int myId, int from, int to)
    {
        List<RankData> ret = new ArrayList();
        getFakeRankResult(users, myId, from, to, ret);
        while ( ret.size() > 4 )
            ret.remove(ret.size() - 1);
        return ret;
     }

    private void getFakeRankResult(IMap<Integer, RankData> users, int myId, int from, int to, List<RankData> returnList)
    {
        EntryObject eo = new PredicateBuilder().getEntryObject();
        int point = users.get(myId).point;
        Predicate sqlQuery = eo.key().notEqual(myId).and(eo.get("point").between(point-to, point-from).or(eo.get("point").between(point+from, point+to)));

        //PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, 20);
        returnList.addAll(users.values(sqlQuery));

        if ( returnList.size() < 4 && to < 100 )
            getFakeRankResult(users, myId, to, to==0?1:to*2, returnList);
    }


    private void logRandomBots(Game game)
    {
        String[] names = {"Ford Mostang", "Mad max", "Wonder woman", "Max pain", "Delta force", "Batman vs super man", "Jon snow", "Walter white", "Ahmad mahmood", "No one", "Ramsy snow", "Dr.gordon freeman", "The last of us", "Captain price", "The egle", "Sepehr to", "game start", "winner man", "Shadow wariror", "Doom metal", "Dark souls", "Marcos fenix", "Naked snake", "Dark lord", "Storm rage", "Devil may cry", "lord of light", "علی رضا", "منصور پیروز", "امیر رضا", "کامران محمدی", "نیمار جونیور", "۶۶-علی حرفه ای", "شاه دیوانه", "$بیل گیتز$", "٧١- سهراب سپهری", "هستی ^-^", "عقاب آسیا", "نیما بوفون", "هارد کور !~!", "دیجی الکس", "مرد خشمگین", "پسر جهنمی", "آقا علی کریمی", "رضا گلدمن", "43-حامد کرزای", "حسن مرادی", "علی حاتمی", "کریس رونالدو", "الناز خانوم", "تام کروز", "امیر حسین", "حامد همایگون", "جوانمرد قاتل", "فقط تراکتور", "عشق فراری", "مسی ١٠", "گرگ تنها", "ریک و مورتی", "پرنسس فاطمه", "میر حسین ١٣٨٨", "جعفر ", "برنده نهایی", "لارا کرفت", "سلطان ", "حسین استارک", "رحیم نجفی", "دیوید کاپرفیلد", "پرنسس سوفیا", "گیسو کمند", "مجیدی فرهادی", "حمید صفت", "یوز ایرانی#", "نهنگ آبی", "کارآگاه پوآرو", "غرب وحشی", "ممد دیکابریو", "دونالد ترامپ", "گاندولف سفید", "فرودو بگینز", "ریییس بزرگ", "کریم هستم", "شرلوک ", "جمشید هاشم پور", "آریا استارک", "کریستوفر نولان", "فاتح قلعه ها", "هری پاتر", "جیمز موریارتی", "استاد کوجیما ", "ارباب قلعه ها", "ارتش برتر", "پیتر بیلیش", "علی دراکولا", "سالید اسنیک", "غارت گر", "اژدها کش", "جک غول کش", "استیون هاوکینگز", "ون هل سینگ", "شکارچی قلب ها", "تام کروز", "برنده ی بازی", "آدم خوار", "دارک سایدرز", "اژدها سوار", "گاو چران خسته", "سوار بی سر", "تک شاخ جادویی", "استاد قلعه ها", "مزدک میرزایی", "زیبای خفته", "زن گربه ای", "بازم تویی؟", "باز تو؟", "فرار کن! منم", "کاپیتان آمریکا", "خدای جنگ", "پادشاه نامیرا", "شاه شب", "سلام بازنده", "آخرش میبازی", "فقط خدا", "جان ویک", "پاپ اسپنجی", "کریس ردفیلد", "Roxana ben10", "Roxana ", "Roxana@lone", "romina Eli", "black cat", "hamed Joker", "eli-Joel", "Roxana ^~^", "mohammad TNT", "XFACTOR Gama", "XFACTOR ^~^", "ben10Roxana", "ben10 Killer", "ben10Gameek", "ben10 Batman", "ben10 Ilidan", "ben10 *king*", "ben10 alpha", "LeonRoxana", "Leon XFACTOR", "Leon@lone", "Leon Killer", "cris Ronaldo", "Leon Batman", "Leon Joker", "Leon Pip-boy", "Leon Joel", "RICKI", "Leon Ilidan", "Leon DMC", "Leon *king*", "Leon ^~^", "Leon 111111", "Leon alpha", "@lone girl", "@lone", "@lone Killer", "@lone Batman", "mad Joker", "@lone *king*", "Killer ben10", "Killer Leon", "Killer Eli", "Killer Joker", "Killer Joel", "Killer ^~^", "Denerys jon", "visirion", "drogon", "Denerys ^~^", "zeos", "Kreitos ***", "EliRoxana", "Elimohammad", "Eli wonder", "Eli single", "Eli @lone", "Eli Killer", "Eli Kreitos", "Eli Assassin", "Eli Joker", "Eli RICKI", "Eli ashari", "negin italian", "reyhan TNT", "Eli *queen*", "narges 111111", "team alpha", "Gama girl", "Gama XFACTOR", "Gama ben10", "raymanGama @lone", "GamaKiller", "Gama Pip-boy", "Gama alpha", "Gameek amir", "Gameekalone", "Gameek Joker", "Gameek alpha", "Finarmin Eli", "rock star", "good fireman", "Ronaldo10", "Assassin man", "Batman ben10", "Batmancatgirl", "Batman @lone", "Batman Eli", "Batman Joker", "Batman ^~^", "rehana", "selena gomes", "Joker ben10", "Joker jester", "Joker Killer", "just ps4", "Joker *king*", "Joker 1378", "Joker pink", "fallout", "Pip-boy Eli", "princes cat", "magician", "Pip-boy TNT", "Joel hana", "hahahahaha", "Joel ben10", "mad max2", "Joel Killer", "Joel Eli", "Joel Gama", "Joel Gameek", "Joel Batman", "Joel Pip-boy", "Joel *king*", "Joel ***", "Joel 77", "Joel alpha", "last hope", "RICKI legolas", "RICKI ben10", "alone ninja", "RICKI @lone", "RICKI Killer", "RICKI Eli", "RICKI Batman", "RICKI Joker", "RICKI Joel", "RICKI Ilidan", "RICKI ^~^", "RICKI 34738947", "RICKI alpha", "Sargeras lord", "blizzard", "Sargeras 000", "Ilidan nakon", "Ilidan @lone", "Ilidan DMC", "Ilidan ^~^", "TNT Roxana", "TNT mohammad", "TNT XFACTOR", "TNT ben10", "TNT Leon", "TNT @lone", "TNT Killer", "TNT Denerys", "TNT Kreitos", "TNT Eli", "TNT Gama", "TNT Gameek", "TNT Finarmin", "TNT Ronaldo", "TNT Assassin", "TNT Batman", "TNT Joker", "TNT mojibazi", "TNT Pip-boy", "TNT Joel", "TNT RICKI", "TNT Sargeras", "TNT Ilidan", "TNT DMC", "TNT *king*", "TNT ^~^", "TNT 111111", "TNT alpha", "DMC Roxana", "DMC mohammad", "DMC XFACTOR", "DMC ben10", "DMC Leon", "DMC @lone", "DMC Killer", "DMC Denerys", "DMC Kreitos", "DMC Eli", "DMC Gama", "DMC Gameek", "DMC Finarmin", "DMC Ronaldo", "DMC Assassin", "DMC Batman", "DMC Joker", "DMC mojibazi", "DMC Pip-boy", "DMC Joel", "DMC RICKI", "DMC Sargeras", "DMC Ilidan", "DMC TNT", "DMC *king*", "DMC ^~^", "DMC 111111", "DMC alpha", "*king* ben10", "*king* Leon", "*king* @lone", "*king* Eli", "*king* Gama", "*king* Joker", "*king* Joel", "*king* RICKI", "*king* TNT", "*king* DMC", "*king* ^~^", "*king* alpha", "alpha Roxana", "alpha ben10", "alpha Leon", "alpha @lone", "alpha Killer", "alpha Eli", "alpha Gama", "alpha Gameek", "alpha Batman", "alpha Joker", "alpha Joel", "alpha RICKI", "alpha Ilidan", "alpha TNT", "alpha DMC", "alpha *king*", "alpha ^~^", "alpha 111111", "مهرنوش لوگان", "مهرنوش سارا", "مهرنوش هالک", "مهرنوش هابیت", "مهرنوش اوستا", "مهرنوش هیچکس", "مهرنوش تتلو", "مهرنوش یاس", "مهرنوش باهوش", "مهرنوش ینیفر", "مهرنوش قاتل", "مهرنوش ققنوس", "مهرنوش شاهین", "مهرنوش دانته", "مهرنوش سام", "لوگان سارا", "لوگان هالک", "لوگان سالار", "لوگان هابیت", "لوگان مکزیکی", "لوگان رباب", "لوگان الطایر", "لوگان پریسا", "لوگان مونا", "لوگان اوستا", "لوگان هوندا*", "لوگان یاس", "لوگان گرالت", "لوگان ینیفر", "لوگان قاتل", "لوگان ققنوس", "لوگان شاهین", "لوگان دانته", "لوگان ابلیس", "لوگان بیچاره", "لوگان سام", "سارا لوگان", "شعبده باز", "سارای ممد", "سارا الفی", "سارا هابیت", "سارا مکزیکی", "سارا الطایر", "سارا اوستا", "سارا راپانزل", "سارا هوندا*", "سارا هیچکس", "سارا گومز", "سارا گرالت", "سارا ینیفر", "سارا قاتل", "سارا ققنوس", "سارا شاهین", "سارا سیندرلا", "سارا دانته", "یارا گریجوی", "سارا بیچاره", "کاظم مهرنوش", "کاظم لوگان", "کاظم سارا", "کاظم سالار", "کاظم هابیت", "کاظم مکزیکی", "کاظم الطایر", "کاظم بیتاااا", "کاظم اوستا", "کاظم راپانزل", "کاظم هوندا*", "کاظم هیچکس", "کاظم تتلو", "کاظم گرالت", "کاظم ینیفر", "کاظم حاجیتون", "کاظم قاتل", "کاظم امیرعلی", "کاظم ققنوس", "کاظم شاهین", "کاظم سیندرلا", "کاظم دانته", "کاظم لویسفر", "کاظم جانی", "دیه مهرنوش", "هدیه لوگان", "هدیه سارا", "هدیه هالک", "هدیه مکزیکی", "هدیه الطایر", "هدیه بیتاااا", "هدیه اوستا", "هدیه راپانزل", "هدیه هوندا*", "هدیه هیچکس", "هدیه تتلو", "هدیه گرالت", "هدیه ینیفر", "هدیه حاجیتون", "هدیه سمی", "هدیه امیرعلی", "هدیه ققنوس", "هدیه شاهین", "هدیه سیندرلا", "هدیه گیلانی", "هدیه لوسیفر", "هدیه بیچاره", "هدیه سام", "مرتضی مهرنوش", "مرتضی لوگان", "مرتضی سارا", "مرتضی هالک", "مرتضی هیچکس", "مرتضی سالار", "مرتضی هابیت", "مرتضی مکزیکی", "مرتضی رباب", "مرتضی الطایر", "مرتضی ینیفر", "مرتضی قاتل", "مرتضی ققنوس", "مرتضی شاهین", "مرتضی دانته", "مرتضی لوسیفر", "مرتضی بیچاره", "مرتضی سام", "آتوسا مهرنوش", "آتوسا لوگان", "آتوسا هالک", "آتوسا هیچکس", "آتوسا سالار", "آتوسا هابیت", "آتوسا مکزیکی", "آتوسا رباب", "آتوسا ملانیا", "آتوسا پریسا", "آتوسا مونا", "آتوسا اوستا", "آتوسا هوندا*", "آتوسا هیچکس", "آتوسا تتلو", "آتوسا یاس", "آتوسا گرالت", "آتوسا ینیفر", "آتوسا قاتل", "آتوسا ققنوس", "آتوسا شاهین", "آتوسا دانته", "آتوسا لویسفر", "آتوسا بیچاره", "الی سامورایی", "الی الطایر", "الی راپانزل", "الی هوندا*", "الی هیچکس", "الی تتلو", "الی یاس", "الی گرالت", "الی ینیفر", "الی قاتل", "الی ماکیاولی", "الی امیرعلی", "الی ققنوس", "الی شاهین", "الی سیندرلا", "الی دانته", "الی لویسفر", "الی بیچاره", "الی سام", "ممد لوگان", "ممد هالک", "ممد هیچکس", "ممد یاس", "ممد گرالت", "ممد ینیفر", "ممد حاجیتون", "ممد قاتل", "ممد ماکیاولی", "ممد امیرعلی", "ممد ققنوس", "ممد شاهین", "ممد سیندرلا", "ممد دانته", "ممد لوسیفر", "ممد بیچاره", "ممد سام", "سالار مهرنوش", "سالار لوگان", "سالار سارا", "سالار هابیت", "سالار مکزیکی", "سالار الطایر", "سالار اوستا", "سالار هوندا*", "سالار هیچکس", "سالار تتلو", "سالار گرالت", "سالار ینیفر", "سالار قاتل", "سالار ققنوس", "سالار شاهین", "سالار دانته", "سالار لوسیفر", "سالار بیچاره", "هابیت لوگان", "هابیت هالک", "هابیت هیچکس", "هابیت سالار", "هابیت مکزیکی", "هابیت پریسا", "هابیت مونا", "اسمیگل", "هابیت هیچکس", "هابیت قاتل", "هابیت شاهین", "بگینز", "هابیت بیچاره", "هابیت فرودو", "رباب مهرنوش", "رباب لوگان", "رباب سارا", "رباب هالک", "رباب هیچکس", "رباب ممد", "رباب سالار", "رباب هابیت", "رباب مکزیکی", "رباب الطایر", "رباب پریسا", "رباب مونا", "رباب بیتاااا", "رباب اوستا", "رباب راپانزل", "رباب هوندا*", "رباب هیچکس", "رباب تتلو", "رباب یاس", "رباب گرالت", "رباب ینیفر", "رباب قاتل", "رباب امیرعلی", "رباب ققنوس", "رباب شاهین", "رباب سیندرلا", "رباب دانته", "رباب لویسفر", "رباب بیچاره", "رباب سام", "سامورایی الی", "سامورایی ممد", "پریسا لوگان", "پریسا هالک", "پریسا هیچکس", "پریسا ممد", "پریسا سالار", "پریسا هابیت", "پریسا مکزیکی", "پریسا رباب", "پریسا کرافت", "پریسا مونا", "پریسا هوندا*", "پریسا هیچکس", "پریسا تتلو", "پریسا یاس", "پریسا گرالت", "پریسا ینیفر", "پریسا قاتل", "پریسا ققنوس", "پریسا شاهین", "پریسا دانته", "پریسا لوسیفر", "پریسا بیچاره", "پریسا سام", "مونا مهرنوش", "مونا لوگان", "مونا سارا", "مونا هالک", "مونا دانیال", "مونا هیچکس", "کلمبیا", "مونا هابیت", "مونا مکزیکی", "مونا فمنیزم", "مونا سامورایی", "مونا الطایر", "مونا اوستا", "مونا راپانزل", "مونا هوندا*", "مونا هیچکس", "مونا تتلو", "مونا یاس", "مونا گرالت", "مونا ینیفر", "دختر بابا", "مونا قاتل", "مونا ماکیاولی", "مونا ققنوس", "مونا شاهین", "مونا سیندرلا", "مونا دانته", "مونا لوسیفر", "مونا بیچاره", "مونا سام", "راپانزل هالک", "راپانزل قاتل", "میتسوبیشی", "ناکازاکی", "عشق هوندا*", "هیچکس یاس", "هیچکس قاتل", "گرالت لوگان", "گرالت ممد", "گرالت سالار", "گرالت هابیت", "گرالت مکزیکی", "گرالت الطایر", "گرالت اوستا", "گرالت هوندا*", "گرالت تتلو", "گرالت ینیفر", "گرالت قاتل", "گرالت ققنوس", "گرالت دانته", "گرالت لویسفر", "گرالت بیچاره", "گرالت سام", "ینیفر لوگان", "ینیفر دانیال", "ینیفر هیچکس", "ینیفر ممد", "ینیفر سالار", "ینیفر هابیت", "ینیفر مکزیکی", "ینیفر گرالت", "ویچر", "ینیفر بیچاره", "ممدشون", "بنیتا", "امیرعلی سارا", "امیرعلی هالک", "امیرعلی کاظم", "امیرعلی هدیه", "امیرعلی الی", "امیرعلی ممد", "امیرعلی رباب", "امیرعلی مونا", "امیرعلی تتلو", "امیرعلی یاس", "امیرعلی قاتل", "امیرعلی سام", "ققنوس مهرنوش", "ققنوس سارا", "ققنوس الی", "ققنوس سالار", "ققنوس مکزیکی", "شاهین ممد", "شاهین سالار", "شاهین هابیت", "شاهین مکزیکی", "شاهین الطایر", "شاهین مونا", "شاهین اوستا", "شاهین هوندا*", "شاهین هیچکس", "شاهین تتلو", "شاهین یاس", "شاهین گرالت", "شاهین ینیفر", "شاهین قاتل", "شاهین ققنوس", "شاهین دانته", "شاهین لویسفر", "شاهین بیچاره", "شاهین سام", "سام لوگان", "سام ممد", "سام سالار", "سام هابیت", "سام مکزیکی", "سام رباب", "سام سامورایی", "سام الطایر", "سام هوندا*", "سام ینیفر", "سام قاتل", "سام ماکیاولی", "سام امیرعلی", "سام ققنوس", "سام شاهین", "سام دانته", "سام لویسفر", "سام بیچاره"};
        shuffleArray(names);
        int[] keys = game.arenas.keys();
        Arena a;
        int index = 0;
        String nams = "\"";
        String nums = "";
        for (int k:keys)
        {
            a = game.arenas.get(k);
            int len = (int) Math.floor(names.length / keys.length);
            for ( int i=0;  i<len;   i++ )
            {
                nams += (names[index + i]  + "\", \"");
                nums += (RandomPicker.getInt(a.min, a.max-Math.max(15, (a.max-a.min)/10) ) + ", " );
            }
            index += len;
        }
        ext.trace(nams+"\"");
        ext.trace(nums);
    }
    void shuffleArray(String[] ar)
    {
        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            String a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
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

        ext.trace(stats);
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
        ext.trace(key + " get in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
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
        ext.trace(key + " get in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
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
        ext.trace("operations get in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }*/
}

