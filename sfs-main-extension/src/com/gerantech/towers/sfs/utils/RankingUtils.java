package com.gerantech.towers.sfs.utils;

import com.gt.data.RankData;
import com.gt.towers.constants.ResourceType;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;

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

        ext.trace("start in-memory filling in " + (System.currentTimeMillis() - (long) ext.getParentZone().getProperty("startTime")) + " milliseconds.");
        // insert real champions
        try {
            IDBManager dbManager = ext.getParentZone().getDBManager();

            // fill active players
            String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type = " + ResourceType.BATTLES_WEEKLY + " AND resources.count > 0";
            ISFSArray dbResult = dbManager.executeQuery(query, new Object[]{});
            for (int p = 0; p < dbResult.size(); p++) {
                ISFSObject pp = dbResult.getSFSObject(p);
                users.put(pp.getInt("id"), new RankData(pp.getUtfString("name"), 0, pp.getInt("count"), 0));
            }
            ext.trace("filled in-memory actives in " + (System.currentTimeMillis() - (long) ext.getParentZone().getProperty("startTime")) + " milliseconds.");

            // fill top players
            query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type = " + ResourceType.POINT + " AND resources.count > 0";
            dbResult = dbManager.executeQuery(query, new Object[]{});
            for (int p = 0; p < dbResult.size(); p++) {
                ISFSObject pp = dbResult.getSFSObject(p);
                if (users.containsKey(pp.getInt("id"))) {
                    RankData rd = users.get(pp.getInt("id"));
                    rd.point = pp.getInt("count");
                    users.replace(pp.getInt("id"), rd);
                } else {
                    users.put(pp.getInt("id"), new RankData(pp.getUtfString("name"), pp.getInt("count"), 0, 0));
                }
            }
            ext.trace("filled in-memory tops in " + (System.currentTimeMillis() - (long) ext.getParentZone().getProperty("startTime")) + " milliseconds.", users.size());

            // fill stars of players
            query = "SELECT resources.player_id, resources.count FROM resources WHERE resources.type = " + ResourceType.STARS_WEEKLY + " AND resources.count > 0";
            dbResult = dbManager.executeQuery(query, new Object[]{});
            for (int p = 0; p < dbResult.size(); p++) {
                ISFSObject pp = dbResult.getSFSObject(p);
                if (users.containsKey(pp.getInt("player_id"))) {
                    RankData rd = users.get(pp.getInt("player_id"));
                    rd.weeklyStars = pp.getInt("count");
                    users.replace(pp.getInt("player_id"), rd);
                }
            }
            ext.trace("filled in-memory stars in " + (System.currentTimeMillis() - (long) ext.getParentZone().getProperty("startTime")) + " milliseconds.", users.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setWeeklyBattles(int id, int battles)
    {
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        RankData opponent = users.get(id);
        opponent.weeklyBattles = battles;
        users.replace(id, opponent);
    }

    public String getRandomName()
    {
        return names[(int) Math.floor(names.length * Math.random())];
    }

    /*public void fillStatistics()
    public void fillStatistics()
    {
        String time = "'2018-09-09 00:00:00' AND '2018-09-16 00:00:00'";
        Map<Integer, Map<Integer, Object>> statistics = new HashMap();
        getResourceStats(statistics, ResourceType.XP, time);
        getResourceStats(statistics, ResourceType.POINT, time);
        getResourceStats(statistics, ResourceType.CURRENCY_SOFT, time);
        getResourceStats(statistics, ResourceType.CURRENCY_HARD, time);
        getResourceStats(statistics, ResourceType.BATTLES, time);
        getResourceStats(statistics, ResourceType.BATTLES_WINS, time);
        getResourceStats(statistics, ResourceType.BOOK_OPENED_BATTLE, time);
        getResourceStats(statistics, ResourceType.BOOK_OPENED_FREE, time);
        getCardStats(statistics, BuildingType.B11_BARRACKS, time);
        getCardStats(statistics, BuildingType.B12_BARRACKS, time);
        getCardStats(statistics, BuildingType.B13_BARRACKS, time);
        getCardStats(statistics, BuildingType.B14_BARRACKS, time);
        getCardStats(statistics, BuildingType.B21_RAPID, time);
        getCardStats(statistics, BuildingType.B22_RAPID, time);
        getCardStats(statistics, BuildingType.B23_RAPID, time);
        getCardStats(statistics, BuildingType.B24_RAPID, time);
        getCardStats(statistics, BuildingType.B31_HEAVY, time);
        getCardStats(statistics, BuildingType.B32_HEAVY, time);
        getCardStats(statistics, BuildingType.B33_HEAVY, time);
        getCardStats(statistics, BuildingType.B34_HEAVY, time);
        getCardStats(statistics, BuildingType.B41_CRYSTAL, time);
        getCardStats(statistics, BuildingType.B42_CRYSTAL, time);
        getCardStats(statistics, BuildingType.B43_CRYSTAL, time);
        getCardStats(statistics, BuildingType.B44_CRYSTAL, time);
        getOperationStats(statistics, time);

        String stats = "";
        for (Map.Entry<Integer, Map<Integer, Object>> entry : statistics.entrySet() )
        {
            stats += entry.getKey() + ",";
            Map<Integer, Object> d = entry.getValue();

            stats += (   d.containsKey(ResourceType.XP)                  ? d.get(ResourceType.XP) : 0 )+","+
            (   d.containsKey(ResourceType.POINT)               ? d.get(ResourceType.POINT) : 0 )+","+
            (   d.containsKey(ResourceType.CURRENCY_SOFT)       ? d.get(ResourceType.CURRENCY_SOFT) : 0 )+","+
            (   d.containsKey(ResourceType.CURRENCY_HARD)       ? d.get(ResourceType.CURRENCY_HARD) : 0 )+","+
            (   d.containsKey(ResourceType.BATTLES)             ? d.get(ResourceType.BATTLES) : 0 )+","+
            (   d.containsKey(ResourceType.BATTLES_WINS)        ? d.get(ResourceType.BATTLES_WINS) : 0 )+","+
            (   d.containsKey(ResourceType.BOOK_OPENED_BATTLE)  ? d.get(ResourceType.BOOK_OPENED_BATTLE) : 0 )+","+
            (   d.containsKey(ResourceType.BOOK_OPENED_FREE)    ? d.get(ResourceType.BOOK_OPENED_FREE) : 0 )+","+
            (   d.containsKey(201)                              ? d.get(201) : 0 )+","+
            (   d.containsKey(202)                              ? d.get(202) : 0 )+",,,";

            stats += getCardValues(entry, d,  BuildingType.B11_BARRACKS, ",,");
            stats += getCardValues(entry, d,  BuildingType.B12_BARRACKS, ",,");
            stats += getCardValues(entry, d,  BuildingType.B13_BARRACKS, ",,");
            stats += getCardValues(entry, d,  BuildingType.B14_BARRACKS, ",,");
            stats += getCardValues(entry, d,  BuildingType.B21_RAPID, ",,");
            stats += getCardValues(entry, d,  BuildingType.B22_RAPID, ",,");
            stats += getCardValues(entry, d,  BuildingType.B23_RAPID, ",,");
            stats += getCardValues(entry, d,  BuildingType.B24_RAPID, ",,");
            stats += getCardValues(entry, d,  BuildingType.B31_HEAVY, ",,");
            stats += getCardValues(entry, d,  BuildingType.B32_HEAVY, ",,");
            stats += getCardValues(entry, d,  BuildingType.B33_HEAVY, ",,");
            stats += getCardValues(entry, d,  BuildingType.B34_HEAVY, ",,");
            stats += getCardValues(entry, d,  BuildingType.B41_CRYSTAL, ",,");
            stats += getCardValues(entry, d,  BuildingType.B42_CRYSTAL, ",,");
            stats += getCardValues(entry, d,  BuildingType.B43_CRYSTAL, ",,");
            stats += getCardValues(entry, d,  BuildingType.B44_CRYSTAL, ",\n");
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

