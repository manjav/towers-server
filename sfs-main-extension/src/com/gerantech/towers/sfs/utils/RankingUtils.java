package com.gerantech.towers.sfs.utils;

import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.arenas.Arena;
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
import com.smartfoxserver.v2.entities.data.*;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ManJav on 7/22/2017.
 */
public class RankingUtils
{
    private final SFSExtension ext;
    private static final String[] names = new String[]{"آتوسا لوگان", "فرودو بگینز", "ون هل سینگ", "ممدشون", "هابیت مونا", "مونا قاتل", "کریستوفر نولان", "پریسا یاس", "غرب وحشی", "مونا مهرنوش", "امیرعلی رباب", "DMC mohammad", "TNT Gama", "استاد کوجیما ", "selena gomes", "Gama XFACTOR", "سام قاتل", "سام الطایر", "هدیه سام", "مرتضی الکسیس", "fallout", "هدیه حاجیتون", "Joel 77", "پریسا دانته", "عقاب آسیا", "alpha ^~^", "سام هوندا*", "ققنوس مهرنوش", "سالار اوستا", "سام ممد", "Leon *king*", "*king* DMC", "شاهین تتلو", "پریسا کرافت", "هدیه هوندا*", "43-حامد کرزای", "گرالت قاتل", "اسمیگل", "Leon DMC", "TNT Denerys", "امیرعلی سارا", "گرالت سالار", "reyhan TNT", "LeonRoxana", "گرالت دانته", "هابیت هیچکس", "DMC XFACTOR", "ben10Roxana", "TNT Killer", "باز تو؟", "سام شاهین", "یارا گریجوی", "هری پاتر", "سالار سارا", "عشق فراری", "نهنگ آبی", "تک شاخ جادویی", "Jon snow", "جوانمرد قاتل", "RICKI Ilidan", "آتوسا اوستا", "مونا فمنیزم", "آتوسا دانته", "alpha Joel", "Ilidan ^~^", "مهرنوش هابیت", "مهرنوش هیچکس", "کاظم راپانزل", "mad Joker", "شاهین ینیفر", "Gama Pip-boy", "alpha TNT", "هابیت مکزیکی", "پریسا هیچکس", "ارتش برتر", "Denerys ^~^", "شاهین هابیت", "@lone Killer", "آتوسا مونا", "Joel Gama", "جک غول کش", "Joel *king*", "سام امیرعلی", "Joker 1378", "ینیفر سالار", "هارد کور !~!", "کاظم بیتاااا", "mad max2", "پریسا لوسیفر", "٧١- سهراب سپهری", "drogon", "گرالت ممد", "جمشید هاشم پور", "TNT mojibazi", "آتوسا لویسفر", "آتوسا الکسیس", "ینیفر گرالت", "ممد لوگان", "سام ینیفر", "Ronaldo10", "Batman ^~^", "Eli wonder", "TNT Kreitos", "Elimohammad", "TNT *king*", "الی امیرعلی", "رباب لوگان", "مهرنوش سارا", "کاظم حاجیتون", "مونا لوگان", "مهرنوش شاهین", "پرنسس فاطمه", "فقط تراکتور", "مرتضی سالار", "لوگان الطایر", "خدای جنگ", "الی الطایر", "Gama ben10", "Sargeras lord", "امیرعلی ممد", "*king* alpha", "مرتضی هالک", "تام کروز", "میتسوبیشی", "مونا شاهین", "مهرنوش هالک", "کامران محمدی", "*king* Joker", "مزدک میرزایی", "گرالت ققنوس", "مرتضی شاهین", "سالار مکزیکی", "الی راپانزل", "مرتضی مکزیکی", "مونا دانته", "الی گرالت", "ققنوس مکزیکی", "الی شاهین", "کاظم ققنوس", "DMC Gama", "Naked snake", "سلطان ", "سارا دانته", "مونا هیچکس", "مونا مکزیکی", "گرالت لوگان", "امیر رضا", "Joker jester", "سارا الفی", "@lone *king*", "ممد گرالت", "جعفر ", "هدیه الکسیس", "RICKI Joker", "مونا سام", "DMC Sargeras", "سارا هوندا*", "سامورایی ممد", "رضا گلدمن", "کاظم هوندا*", "رباب مونا", "ینیفر هابیت", "امیرعلی قاتل", "علی رضا", "ben10 alpha", "Pip-boy TNT", "مونا ماکیاولی", "رباب مهرنوش", "hamed Joker", "رباب دانته", "پریسا قاتل", "blizzard", "*king* Gama", "کاظم هابیت", "پیتر بیلیش", "@lone Batman", "سارای ممد", "الی الکسیس", "alpha ben10", "گرالت سام", "الی دانته", "Gama girl", "Eli Kreitos", "جیمز موریارتی", "سالار ینیفر", "سارا مکزیکی", "هدیه هالک", "رباب راپانزل", "مرتضی لوگان", "رباب پریسا", "DMC Kreitos", "Ahmad mahmood", "لوگان گرالت", "هابیت هیچکس", "Mad max", "سارا گرالت", "آریا استارک", "سام ققنوس", "TNT Leon", "TNT @lone", "RICKI Joel", "آتوسا مهرنوش", "ریک و مورتی", "رباب هابیت", "کاظم مکزیکی", "TNT Ronaldo", "TNT Roxana", "رباب بیتاااا", "Batman Eli", "مونا سامورایی", "سالار الکسیس", "*king* TNT", "RICKI", "دونالد ترامپ", "Walter white", "last hope", "شاهین الطایر", "مهرنوش قاتل", "دختر بابا", "غارت گر", "سارا راپانزل", "حمید صفت", "narges 111111", "ینیفر الکسیس", "گرگ تنها", "cris Ronaldo", "سارا گومز", "mohammad TNT", "Ilidan DMC", "Devil may cry", "مجیدی فرهادی", "*king* ^~^", "سالار هیچکس", "کاظم سالار", "سارا لوگان", "Joker ben10", "هابیت شاهین", "آتوسا هالک", "دیجی الکس", "رحیم نجفی", "نیمار جونیور", "لوگان قاتل", "امیرعلی هدیه", "هدیه سارا", "RICKI 34738947", "Ramsy snow", "کارآگاه پوآرو", "مرد خشمگین", "alpha 111111", "DMC TNT", "لوگان هابیت", "team alpha", "شاهین لویسفر", "Leon Killer", "RICKI ben10", "سام سامورایی", "پاپ اسپنجی", "مرتضی ققنوس", "کاظم ینیفر", "Joel hana", "Killer Leon", "لوگان یاس", "شرلوک ", "ممد الکسیس", "DMC Leon", "visirion", "مونا هابیت", "alpha Eli", "شاهین الکسیس", "حسن مرادی", "No one", "سارا هابیت", "مرتضی مهرنوش", "آتوسا شاهین", "الی سامورایی", "RICKI legolas", "شاهین ققنوس", "گرالت اوستا", "پریسا ینیفر", "سام مکزیکی", "رباب شاهین", "رباب قاتل", "alpha Leon", "TNT Eli", "فرار کن! منم", "پریسا ممد", "کاظم الطایر", "شاهین گرالت", "RICKI @lone", "مرتضی الطایر", "لوگان ابلیس", "لوگان مکزیکی", "کاظم تتلو", "alpha Ilidan", "بازم تویی؟", "دیوید کاپرفیلد", "مونا راپانزل", "ققنوس الی", "رباب لویسفر", "استاد قلعه ها", "هستی ^-^", "شاهین هوندا*", "لوگان الکسیس", "TNT Pip-boy", "اژدها کش", "TNT alpha", "Shadow wariror", "Joker pink", "امیرعلی الی", "کاپیتان آمریکا", "گرالت الکسیس", "eli-Joel", "شاهین مونا", "مرتضی دانته", "مهرنوش باهوش", "Leon XFACTOR", "هابیت سالار", "پریسا رباب", "ارباب قلعه ها", "lord of light", "Max pain", "Roxana ^~^", "ممد هیچکس", "شاهین دانته", "Joel Pip-boy", "راپانزل هالک", "مونا سیندرلا", "پریسا هوندا*", "بنیتا", "سارا قاتل", "گیسو کمند", "آتوسا ینیفر", "هابیت فرودو", "DMC Joker", "Storm rage", "Joel Gameek", "TNT XFACTOR", "آتوسا پریسا", "winner man", "مونا الکسیس", "لوگان دانته", "ممد ماکیاولی", "ینیفر دانیال", "پریسا سام", "کاظم اوستا", "ben10 Killer", "Ilidan @lone", "بگینز", "سالار ققنوس", "آتوسا ملانیا", "هدیه بیتاااا", "ممد ققنوس", "پریسا سالار", "آتوسا ققنوس", "مهرنوش دانته", "تام کروز", "Batmancatgirl", "لوگان هالک", "کاظم لویسفر", "alpha Killer", "استیون هاوکینگز", "*king* Eli", "TNT Finarmin", "کاظم دانته", "alpha Batman", "امیرعلی تتلو", "مونا سارا", "شاهین سام", "سالار تتلو", "XFACTOR ^~^", "@lone girl", "هدیه ینیفر", "آتوسا گرالت", "ینیفر ممد", "سالار لوسیفر", "ممد قاتل", "DMC Assassin", "پریسا هابیت", "Denerys jon", "مرتضی هیچکس", "هابیت الکسیس", "آتوسا تتلو", "سلام بازنده", "alpha Gama", "ویچر", "Doom metal", "کریس ردفیلد", "هدیه گرالت", "Joker *king*", "زیبای خفته", "شاهین قاتل", "برنده نهایی", "سام هابیت", "پریسا شاهین", "DMC Denerys", "سارا هیچکس", "رباب سیندرلا", "مرتضی ینیفر", "شاهین مکزیکی", "ققنوس سالار", "ققنوس سارا", "مرتضی سارا", "لوگان مونا", "مونا ققنوس", "امیر حسین", "ممد دیکابریو", "اژدها سوار", "TNT Joker", "پریسا ققنوس", "TNT Assassin", "راپانزل قاتل", "ناکازاکی", "Dr.gordon freeman", "RICKI ^~^", "Roxana@lone", "game start", "مونا دانیال", "گرالت هابیت", "DMC Eli", "منصور پیروز", "الی هوندا*", "الی تتلو", "کریس رونالدو", "alpha Gameek", "black cat", "هابیت پریسا", "The egle", "حامد همایگون", "TNT ^~^", "سام الکسیس", "پریسا مونا", "Leon Joker", "good fireman", "DMC RICKI", "Leon Batman", "۶۶-علی حرفه ای", "آتوسا سالار", "Wonder woman", "زن گربه ای", "romina Eli", "رباب سام", "EliRoxana", "آتوسا هیچکس", "آقا علی کریمی", "DMC mojibazi", "آدم خوار", "هابیت قاتل", "سالار شاهین", "سالار مهرنوش", "سوار بی سر", "DMC Gameek", "alpha Roxana", "عشق هوندا*", "Eli single", "DMC Ronaldo", "کاظم جانی", "هدیه مکزیکی", "سام لویسفر", "الی ققنوس", "الی هیچکس", "سارا ینیفر", "ممد یاس", "مونا الطایر", "گرالت الطایر", "مرتضی قاتل", "Gameek amir", "آتوسا هیچکس", "رباب تتلو", "مونا تتلو", "رباب مکزیکی", "Captain price", "سالید اسنیک", "مونا لوسیفر", "Gameek alpha", "TNT DMC", "کاظم سارا", "امیرعلی مونا", "الی سیندرلا", "کلمبیا", "گرالت تتلو", "Gameekalone", "alpha @lone", "princes cat", "شاه دیوانه", "هدیه ققنوس", "پریسا الکسیس", "ینیفر مکزیکی", "شعبده باز", "just ps4", "rehana", "هدیه سمی", "جان ویک", "شاهین هیچکس", "Eli RICKI", "RICKI Eli", "الی لویسفر", "پریسا لوگان", "Killer Eli", "گرالت هوندا*", "پریسا تتلو", "هیچکس قاتل", "هدیه لوسیفر", "رباب هالک", "ممد شاهین", "سام لوگان", "آتوسا قاتل", "رباب هیچکس", "الی ماکیاولی", "TNT Ilidan", "TNT RICKI", "ممد دانته", "@lone", "امیرعلی سام", "rock star", "alpha Joker", "پریسا هالک", "سالار گرالت", "مهرنوش تتلو", "شاهین یاس", "مرتضی لوسیفر", "ینیفر لوگان", "لوگان ققنوس", "Joel ben10", "رباب هوندا*", "لوگان شاهین", "مونا هیچکس", "alpha RICKI", "امیرعلی هالک", "Eli Joker", "نیما بوفون", "Roxana ", "هدیه گیلانی", "ینیفر هیچکس", "حسین استارک", "الی سام", "GamaKiller", "RICKI Batman", "مهرنوش اوستا", "$بیل گیتز$", "سام ماکیاولی", "Batman Joker", "DMC Roxana", "ممد حاجیتون", "*king* @lone", "مهرنوش ققنوس", "کاظم گرالت", "سارا الکسیس", "شکارچی قلب ها", "رباب سارا", "مرتضی رباب", "پادشاه نامیرا", "DMC 111111", "شاهین ممد", "دیه مهرنوش", "آتوسا رباب", "Batman ben10", "لوگان پریسا", "هابیت هالک", "Eli *queen*", "ben10 Batman", "magician", "Killer ^~^", "پریسا هیچکس", "ممد امیرعلی", "دارک سایدرز", "سامورایی الی", "مرتضی هابیت", "مهرنوش یاس", "سارا شاهین", "Kreitos ***", "Leon Pip-boy", "رباب یاس", "Delta force", "سام رباب", "رباب گرالت", "Joel alpha", "مونا اوستا", "Killer Joel", "فقط خدا", "سالار لوگان", "Eli ashari", "سارا اوستا", "آتوسا هوندا*", "رباب الکسیس", "RICKI Killer", "TNT Joel", "ممد سام", "سالار دانته", "سارا الطایر", "alpha *king*", "گاو چران خسته", "ben10 Ilidan", "Eli @lone", "XFACTOR Gama", "لوگان هوندا*", "سارا سیندرلا", "hahahahaha", "الی ینیفر", "سارا ققنوس", "DMC @lone", "Leon@lone", "سالار هوندا*", "برنده ی بازی", "سالار هابیت", "ben10 *king*", "Dark souls", "شاهین اوستا", "TNT ben10", "فاتح قلعه ها", "Leon Ilidan", "الی قاتل", "میر حسین ١٣٨٨", "هدیه راپانزل", "Joel Eli", "رباب ینیفر", "لوگان سارا", "هدیه الطایر", "رباب سالار", "ممد ینیفر", "The last of us", "TNT 111111", "شاه شب", "Joel Batman", "مهرنوش سام", "negin italian", "هدیه امیرعلی", "شاهین سالار", "گاندولف سفید", "کاظم مهرنوش", "raymanGama @lone", "Joker Killer", "Sargeras 000", "*king* RICKI", "سام دانته", "لوگان سالار", "ممد سیندرلا", "علی دراکولا", "Finarmin Eli", "هدیه اوستا", "آتوسا مکزیکی", "آتوسا یاس", "DMC Killer", "alpha DMC", "DMC ^~^", "رباب ممد", "Leon alpha", "هدیه شاهین", "سالار الطایر", "DMC Pip-boy", "کاظم امیرعلی", "پرنسس سوفیا", "رباب ققنوس", "هدیه هیچکس", "DMC Batman", "*king* ben10", "کاظم شاهین", "Gama alpha", "مونا ینیفر", "کاظم سیندرلا", "Leon Joel", "مونا هالک", "پریسا مکزیکی", "TNT Sargeras", "سالار قاتل", "آتوسا هابیت", "Gameek Joker", "مهرنوش لوگان", "پسر جهنمی", "Joel Killer", "رباب امیرعلی", "Sepehr to", "مونا گرالت", "DMC Joel", "مونا یاس", "آخرش میبازی", "لوگان رباب", "zeos", "Pip-boy Eli", "TNT Batman", "لوگان ینیفر", "لارا کرفت", "Eli Assassin", "DMC *king*", "DMC Finarmin", "Ford Mostang", "هابیت لوگان", "رباب الطایر", "مرتضی سام", "ریییس بزرگ", "Roxana ben10", "Dark lord", "Leon 111111", "هیچکس یاس", "مسی ١٠", "پریسا گرالت", "Leon ^~^", "Killer ben10", "هدیه تتلو", "امیرعلی کاظم", "مهرنوش ینیفر", "Batman vs super man", "alone ninja", "ممد هالک", "هدیه سیندرلا", "Eli Killer", "امیرعلی یاس", "TNT Gameek", "علی حاتمی", "DMC Ilidan", "Marcos fenix", "گرالت لویسفر", "ممد لوسیفر", "رباب اوستا", "گرالت ینیفر", "لوگان سام", "سام سالار", "کاظم قاتل", "Batman @lone", "لوگان اوستا", "RICKI alpha", "هدیه لوگان", "Assassin man", "DMC ben10", "کاظم لوگان", "کریم هستم", "رباب هیچکس", "ben10Gameek", "DMC alpha", "الی یاس", "Killer Joker", "مونا هوندا*", "*king* Joel", "Ilidan nakon", "TNT mohammad", "*king* Leon", "گرالت مکزیکی", "Joel ***", "یوز ایرانی#", "الناز خانوم", "کاظم هیچکس"};


    public RankingUtils()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static RankingUtils getInstance()
    {
        return new RankingUtils();
    }

    public void fillByIds(IMap<Integer, RankData> users, ISFSArray members)
    {
        List<Integer> ids = new ArrayList();
        for (int i = members.size()-1; i >= 0 ; i--)
            if( !users.containsKey(members.getSFSObject(i).getInt("id")) )
                ids.add (members.getSFSObject(i).getInt("id"));

        if( ids.size() == 0 )
            return;

        String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type = 1001 AND (";
        for (int i = ids.size()-1; i >=0 ; i--)
            query += " players.id = " + ids.get(i) + ( i==0 ? " );" : " OR ");

        try {
            ISFSArray players = ext.getParentZone().getDBManager().executeQuery(query, new Object[] {});
            for( int p=0; p<players.size(); p++ )
            {
                ISFSObject pp = players.getSFSObject(p);
                users.put(pp.getInt("id"), new RankData(pp.getInt("id"), pp.getUtfString("name"), pp.getInt("count"), 0));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public IMap<Integer, RankData> fill(IMap<Integer, RankData> users, Game game)
    {
        if( users.size() > 10 )
             return users;

        long now = Instant.now().toEpochMilli();
        // insert real champions
        try {
            IDBManager dbManager = ext.getParentZone().getDBManager();
            Arena arena;
            int[] arenaKeys = game.arenas.keys();
            for( int a=0; a<arenaKeys.length; a++ )
            {
                arena = game.arenas.get(arenaKeys[a]);
                String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type =1001 AND resources.count BETWEEN " + (arena.min==0?1:arena.min) + " AND " + arena.max + " ORDER BY resources.count DESC LIMIT 500";
                ISFSArray players = dbManager.executeQuery(query, new Object[] {});

                query = "SELECT player_id, count FROM resources WHERE type=1204 AND ( ";
                for( int p=0; p<players.size(); p++ )
                {
                    ISFSObject pp = players.getSFSObject(p);
                    query += "player_id=" + pp.getInt("id") + (p<players.size()-1?" OR ":" );");
                    users.put(pp.getInt("id"), new RankData(pp.getInt("id"), pp.getUtfString("name"), pp.getInt("count"), 0));
                }

                if( players.size() > 0 )
                {
                    ISFSArray resources = dbManager.executeQuery(query, new Object[]{});
                    for (int r = 0; r < resources.size(); r++)
                    {
                        RankData rd = users.get(resources.getSFSObject(r).getInt("player_id"));
                        rd.xp = resources.getSFSObject(r).getInt("count");
                        users.replace(resources.getSFSObject(r).getInt("player_id"), rd );
                    }
                }
            }
            ext.trace("time : ",(Instant.now().toEpochMilli()-now));
        } catch (SQLException e) { e.printStackTrace(); }

        // insert npcs
        //logRandomBots(game);
        int[] points = new int[]{ 6, 32, 0, 8, 14, 9, 21, 18, 4, 14, 6, 11, 24, 30, 12, 29, 22, 11, 9, 17, 32, 7, 31, 22, 11, 0, 29, 34, 17, 6, 19, 30, 8, 27, 3, 3, 2, 24, 33, 9, 21, 15, 3, 11, 4, 26, 20, 31, 21, 0, 33, 34, 31, 32, 24, 3, 23, 34, 30, 23, 20, 30, 9, 28, 23, 9, 15, 23, 1, 22, 31, 20, 24, 31, 67, 73, 73, 53, 62, 56, 65, 66, 74, 83, 70, 82, 68, 64, 53, 51, 78, 82, 81, 63, 62, 70, 52, 67, 65, 72, 78, 54, 75, 64, 65, 70, 76, 53, 61, 71, 71, 73, 56, 60, 74, 69, 83, 65, 80, 75, 72, 57, 70, 56, 62, 61, 79, 67, 61, 65, 60, 77, 80, 81, 74, 70, 75, 83, 84, 83, 62, 70, 83, 80, 62, 81, 55, 57, 148, 111, 141, 105, 154, 145, 146, 102, 126, 144, 137, 133, 115, 112, 107, 102, 139, 154, 102, 107, 126, 131, 154, 126, 110, 126, 132, 107, 120, 133, 128, 151, 115, 120, 145, 138, 104, 106, 123, 113, 117, 135, 132, 152, 153, 139, 143, 152, 109, 139, 144, 117, 121, 106, 152, 114, 113, 144, 132, 102, 143, 125, 139, 144, 128, 120, 148, 120, 147, 118, 143, 150, 123, 145, 212, 213, 184, 215, 217, 201, 207, 201, 187, 182, 172, 220, 178, 177, 187, 189, 172, 202, 174, 208, 181, 176, 208, 202, 217, 207, 180, 201, 177, 201, 179, 220, 172, 187, 209, 220, 202, 186, 185, 186, 192, 222, 182, 195, 176, 174, 181, 193, 180, 185, 196, 184, 223, 189, 179, 176, 178, 186, 195, 175, 194, 215, 197, 216, 188, 190, 183, 216, 173, 223, 207, 178, 196, 171, 294, 277, 259, 262, 273, 246, 244, 284, 287, 258, 264, 260, 251, 275, 281, 243, 277, 282, 279, 266, 265, 271, 280, 244, 269, 254, 250, 243, 293, 280, 268, 250, 264, 294, 242, 280, 251, 259, 275, 275, 254, 283, 244, 280, 291, 241, 262, 283, 255, 280, 242, 271, 269, 278, 245, 265, 242, 251, 267, 276, 289, 292, 257, 289, 283, 252, 242, 257, 281, 260, 269, 284, 264, 257, 390, 379, 368, 385, 453, 326, 397, 464, 379, 325, 405, 411, 449, 339, 378, 344, 435, 422, 450, 446, 429, 411, 407, 367, 361, 433, 392, 356, 448, 344, 397, 463, 408, 444, 474, 327, 461, 454, 325, 356, 427, 477, 355, 355, 459, 345, 362, 373, 473, 326, 343, 371, 431, 443, 407, 390, 356, 466, 342, 378, 367, 456, 455, 378, 461, 400, 423, 346, 452, 379, 320, 467, 331, 326, 530, 547, 521, 542, 665, 504, 620, 645, 556, 526, 556, 672, 576, 593, 542, 547, 580, 525, 564, 656, 611, 679, 563, 559, 678, 654, 535, 638, 522, 670, 665, 607, 660, 528, 516, 678, 664, 588, 529, 619, 567, 590, 606, 550, 645, 673, 651, 527, 515, 511, 637, 593, 555, 627, 674, 563, 506, 612, 505, 589, 647, 614, 562, 565, 518, 651, 679, 574, 582, 680, 624, 675, 628, 679, 903, 920, 954, 765, 878, 783, 801, 883, 733, 746, 754, 743, 793, 794, 899, 964, 787, 919, 845, 919, 893, 806, 758, 705, 709, 861, 701, 764, 929, 802, 957, 708, 802, 829, 907, 727, 880, 941, 898, 718, 961, 923, 780, 859, 946, 932, 739, 826, 806, 967, 820, 815, 763, 824, 870, 922, 828, 797, 932, 718, 951, 743, 897, 828, 737, 708, 867, 739, 843, 736, 740, 898, 856, 745, 1342, 1224, 1087, 1172, 1061, 1129, 1176, 1173, 1230, 1073, 1143, 1267, 1157, 1066, 1162, 1081, 1248, 1170, 1198, 1028, 1116, 1164, 1118, 1025, 1283, 1154, 1148, 1205, 1292, 1200, 1311, 1318, 1176, 1013, 1161, 1006, 1212, 1011, 1283, 1257, 1120, 1037, 1303, 1196, 1330, 1264, 1154, 1005, 1160, 1154, 1284, 1345, 1185, 1151, 1093, 1184, 1310, 1115, 1350, 1357, 1202, 1170, 1009, 1321, 1090, 1018, 1278, 1296, 1011, 1338, 1004, 1011, 1296, 1092, 6852, 1508, 3365, 7385, 7360, 1941, 5467, 3250, 4403, 8179, 5583, 3179, 6493, 6295, 7653, 7033, 1503, 4406, 8382, 4520, 8609, 2755, 4627, 1669, 7200, 7387, 4555, 7091, 7219, 2669, 6287, 8805, 2973, 6248, 4252, 9121, 3922, 3412, 7317, 2409, 5076, 9073, 4192, 4766, 1528, 6680, 2252, 6062, 2114, 8529, 5734, 6235, 8606, 6830, 4468, 1958, 2621, 1537, 8882, 3882, 4944, 4809, 4240, 5976, 1971, 7619, 5001, 8512, 7371, 7676, 5719, 4616, 5186, 6636 };
        int start = 0;
        {
            int len = points.length;
            for ( int i=0;  i<len; i++,start++ )
                users.put(start , new RankData(start, names[i], points[i], -1));
        }
        ext.trace("time2 : ", Instant.now().toEpochMilli()-now, start);
        return users;
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

    public void setXP(int id, int xp)
    {
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        RankData opponent = users.get(id);
        opponent.xp = xp;
        users.replace(opponent.id, opponent);
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
        Predicate sqlQuery = eo.get("id").notEqual(myId).and(eo.get("point").between(point-to, point-from).or(eo.get("point").between(point+from, point+to)));

        //PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, 20);
        returnList.addAll(users.values(sqlQuery));

        if ( returnList.size() < 4 && to < 100 )
            getFakeRankResult(users, myId, to, to==0?1:to*2, returnList);
    }


    private void logRandomBots(Game game)
    {
        String[] names = {"Ford Mostang", "Mad max", "Wonder woman", "Max pain", "Delta force", "Batman vs super man", "Jon snow", "Walter white", "Ahmad mahmood", "No one", "Ramsy snow", "Dr.gordon freeman", "The last of us", "Captain price", "The egle", "Sepehr to", "game start", "winner man", "Shadow wariror", "Doom metal", "Dark souls", "Marcos fenix", "Naked snake", "Dark lord", "Storm rage", "Devil may cry", "lord of light", "علی رضا", "منصور پیروز", "امیر رضا", "کامران محمدی", "نیمار جونیور", "۶۶-علی حرفه ای", "شاه دیوانه", "$بیل گیتز$", "٧١- سهراب سپهری", "هستی ^-^", "عقاب آسیا", "نیما بوفون", "هارد کور !~!", "دیجی الکس", "مرد خشمگین", "پسر جهنمی", "آقا علی کریمی", "رضا گلدمن", "43-حامد کرزای", "حسن مرادی", "علی حاتمی", "کریس رونالدو", "الناز خانوم", "تام کروز", "امیر حسین", "حامد همایگون", "جوانمرد قاتل", "فقط تراکتور", "عشق فراری", "مسی ١٠", "گرگ تنها", "ریک و مورتی", "پرنسس فاطمه", "میر حسین ١٣٨٨", "جعفر ", "برنده نهایی", "لارا کرفت", "سلطان ", "حسین استارک", "رحیم نجفی", "دیوید کاپرفیلد", "پرنسس سوفیا", "گیسو کمند", "مجیدی فرهادی", "حمید صفت", "یوز ایرانی#", "نهنگ آبی", "کارآگاه پوآرو", "غرب وحشی", "ممد دیکابریو", "دونالد ترامپ", "گاندولف سفید", "فرودو بگینز", "ریییس بزرگ", "کریم هستم", "شرلوک ", "جمشید هاشم پور", "آریا استارک", "کریستوفر نولان", "فاتح قلعه ها", "هری پاتر", "جیمز موریارتی", "استاد کوجیما ", "ارباب قلعه ها", "ارتش برتر", "پیتر بیلیش", "علی دراکولا", "سالید اسنیک", "غارت گر", "اژدها کش", "جک غول کش", "استیون هاوکینگز", "ون هل سینگ", "شکارچی قلب ها", "تام کروز", "برنده ی بازی", "آدم خوار", "دارک سایدرز", "اژدها سوار", "گاو چران خسته", "سوار بی سر", "تک شاخ جادویی", "استاد قلعه ها", "مزدک میرزایی", "زیبای خفته", "زن گربه ای", "بازم تویی؟", "باز تو؟", "فرار کن! منم", "کاپیتان آمریکا", "خدای جنگ", "پادشاه نامیرا", "شاه شب", "سلام بازنده", "آخرش میبازی", "فقط خدا", "جان ویک", "پاپ اسپنجی", "کریس ردفیلد", "Roxana ben10", "Roxana ", "Roxana@lone", "romina Eli", "black cat", "hamed Joker", "eli-Joel", "Roxana ^~^", "mohammad TNT", "XFACTOR Gama", "XFACTOR ^~^", "ben10Roxana", "ben10 Killer", "ben10Gameek", "ben10 Batman", "ben10 Ilidan", "ben10 *king*", "ben10 alpha", "LeonRoxana", "Leon XFACTOR", "Leon@lone", "Leon Killer", "cris Ronaldo", "Leon Batman", "Leon Joker", "Leon Pip-boy", "Leon Joel", "RICKI", "Leon Ilidan", "Leon DMC", "Leon *king*", "Leon ^~^", "Leon 111111", "Leon alpha", "@lone girl", "@lone", "@lone Killer", "@lone Batman", "mad Joker", "@lone *king*", "Killer ben10", "Killer Leon", "Killer Eli", "Killer Joker", "Killer Joel", "Killer ^~^", "Denerys jon", "visirion", "drogon", "Denerys ^~^", "zeos", "Kreitos ***", "EliRoxana", "Elimohammad", "Eli wonder", "Eli single", "Eli @lone", "Eli Killer", "Eli Kreitos", "Eli Assassin", "Eli Joker", "Eli RICKI", "Eli ashari", "negin italian", "reyhan TNT", "Eli *queen*", "narges 111111", "team alpha", "Gama girl", "Gama XFACTOR", "Gama ben10", "raymanGama @lone", "GamaKiller", "Gama Pip-boy", "Gama alpha", "Gameek amir", "Gameekalone", "Gameek Joker", "Gameek alpha", "Finarmin Eli", "rock star", "good fireman", "Ronaldo10", "Assassin man", "Batman ben10", "Batmancatgirl", "Batman @lone", "Batman Eli", "Batman Joker", "Batman ^~^", "rehana", "selena gomes", "Joker ben10", "Joker jester", "Joker Killer", "just ps4", "Joker *king*", "Joker 1378", "Joker pink", "fallout", "Pip-boy Eli", "princes cat", "magician", "Pip-boy TNT", "Joel hana", "hahahahaha", "Joel ben10", "mad max2", "Joel Killer", "Joel Eli", "Joel Gama", "Joel Gameek", "Joel Batman", "Joel Pip-boy", "Joel *king*", "Joel ***", "Joel 77", "Joel alpha", "last hope", "RICKI legolas", "RICKI ben10", "alone ninja", "RICKI @lone", "RICKI Killer", "RICKI Eli", "RICKI Batman", "RICKI Joker", "RICKI Joel", "RICKI Ilidan", "RICKI ^~^", "RICKI 34738947", "RICKI alpha", "Sargeras lord", "blizzard", "Sargeras 000", "Ilidan nakon", "Ilidan @lone", "Ilidan DMC", "Ilidan ^~^", "TNT Roxana", "TNT mohammad", "TNT XFACTOR", "TNT ben10", "TNT Leon", "TNT @lone", "TNT Killer", "TNT Denerys", "TNT Kreitos", "TNT Eli", "TNT Gama", "TNT Gameek", "TNT Finarmin", "TNT Ronaldo", "TNT Assassin", "TNT Batman", "TNT Joker", "TNT mojibazi", "TNT Pip-boy", "TNT Joel", "TNT RICKI", "TNT Sargeras", "TNT Ilidan", "TNT DMC", "TNT *king*", "TNT ^~^", "TNT 111111", "TNT alpha", "DMC Roxana", "DMC mohammad", "DMC XFACTOR", "DMC ben10", "DMC Leon", "DMC @lone", "DMC Killer", "DMC Denerys", "DMC Kreitos", "DMC Eli", "DMC Gama", "DMC Gameek", "DMC Finarmin", "DMC Ronaldo", "DMC Assassin", "DMC Batman", "DMC Joker", "DMC mojibazi", "DMC Pip-boy", "DMC Joel", "DMC RICKI", "DMC Sargeras", "DMC Ilidan", "DMC TNT", "DMC *king*", "DMC ^~^", "DMC 111111", "DMC alpha", "*king* ben10", "*king* Leon", "*king* @lone", "*king* Eli", "*king* Gama", "*king* Joker", "*king* Joel", "*king* RICKI", "*king* TNT", "*king* DMC", "*king* ^~^", "*king* alpha", "alpha Roxana", "alpha ben10", "alpha Leon", "alpha @lone", "alpha Killer", "alpha Eli", "alpha Gama", "alpha Gameek", "alpha Batman", "alpha Joker", "alpha Joel", "alpha RICKI", "alpha Ilidan", "alpha TNT", "alpha DMC", "alpha *king*", "alpha ^~^", "alpha 111111", "مهرنوش لوگان", "مهرنوش سارا", "مهرنوش هالک", "مهرنوش هابیت", "مهرنوش اوستا", "مهرنوش هیچکس", "مهرنوش تتلو", "مهرنوش یاس", "مهرنوش باهوش", "مهرنوش ینیفر", "مهرنوش قاتل", "مهرنوش ققنوس", "مهرنوش شاهین", "مهرنوش دانته", "مهرنوش سام", "لوگان سارا", "لوگان هالک", "لوگان سالار", "لوگان هابیت", "لوگان مکزیکی", "لوگان رباب", "لوگان الطایر", "لوگان پریسا", "لوگان مونا", "لوگان اوستا", "لوگان هوندا*", "لوگان یاس", "لوگان گرالت", "لوگان ینیفر", "لوگان قاتل", "لوگان ققنوس", "لوگان شاهین", "لوگان دانته", "لوگان ابلیس", "لوگان الکسیس", "لوگان سام", "سارا لوگان", "شعبده باز", "سارای ممد", "سارا الفی", "سارا هابیت", "سارا مکزیکی", "سارا الطایر", "سارا اوستا", "سارا راپانزل", "سارا هوندا*", "سارا هیچکس", "سارا گومز", "سارا گرالت", "سارا ینیفر", "سارا قاتل", "سارا ققنوس", "سارا شاهین", "سارا سیندرلا", "سارا دانته", "یارا گریجوی", "سارا الکسیس", "کاظم مهرنوش", "کاظم لوگان", "کاظم سارا", "کاظم سالار", "کاظم هابیت", "کاظم مکزیکی", "کاظم الطایر", "کاظم بیتاااا", "کاظم اوستا", "کاظم راپانزل", "کاظم هوندا*", "کاظم هیچکس", "کاظم تتلو", "کاظم گرالت", "کاظم ینیفر", "کاظم حاجیتون", "کاظم قاتل", "کاظم امیرعلی", "کاظم ققنوس", "کاظم شاهین", "کاظم سیندرلا", "کاظم دانته", "کاظم لویسفر", "کاظم جانی", "دیه مهرنوش", "هدیه لوگان", "هدیه سارا", "هدیه هالک", "هدیه مکزیکی", "هدیه الطایر", "هدیه بیتاااا", "هدیه اوستا", "هدیه راپانزل", "هدیه هوندا*", "هدیه هیچکس", "هدیه تتلو", "هدیه گرالت", "هدیه ینیفر", "هدیه حاجیتون", "هدیه سمی", "هدیه امیرعلی", "هدیه ققنوس", "هدیه شاهین", "هدیه سیندرلا", "هدیه گیلانی", "هدیه لوسیفر", "هدیه الکسیس", "هدیه سام", "مرتضی مهرنوش", "مرتضی لوگان", "مرتضی سارا", "مرتضی هالک", "مرتضی هیچکس", "مرتضی سالار", "مرتضی هابیت", "مرتضی مکزیکی", "مرتضی رباب", "مرتضی الطایر", "مرتضی ینیفر", "مرتضی قاتل", "مرتضی ققنوس", "مرتضی شاهین", "مرتضی دانته", "مرتضی لوسیفر", "مرتضی الکسیس", "مرتضی سام", "آتوسا مهرنوش", "آتوسا لوگان", "آتوسا هالک", "آتوسا هیچکس", "آتوسا سالار", "آتوسا هابیت", "آتوسا مکزیکی", "آتوسا رباب", "آتوسا ملانیا", "آتوسا پریسا", "آتوسا مونا", "آتوسا اوستا", "آتوسا هوندا*", "آتوسا هیچکس", "آتوسا تتلو", "آتوسا یاس", "آتوسا گرالت", "آتوسا ینیفر", "آتوسا قاتل", "آتوسا ققنوس", "آتوسا شاهین", "آتوسا دانته", "آتوسا لویسفر", "آتوسا الکسیس", "الی سامورایی", "الی الطایر", "الی راپانزل", "الی هوندا*", "الی هیچکس", "الی تتلو", "الی یاس", "الی گرالت", "الی ینیفر", "الی قاتل", "الی ماکیاولی", "الی امیرعلی", "الی ققنوس", "الی شاهین", "الی سیندرلا", "الی دانته", "الی لویسفر", "الی الکسیس", "الی سام", "ممد لوگان", "ممد هالک", "ممد هیچکس", "ممد یاس", "ممد گرالت", "ممد ینیفر", "ممد حاجیتون", "ممد قاتل", "ممد ماکیاولی", "ممد امیرعلی", "ممد ققنوس", "ممد شاهین", "ممد سیندرلا", "ممد دانته", "ممد لوسیفر", "ممد الکسیس", "ممد سام", "سالار مهرنوش", "سالار لوگان", "سالار سارا", "سالار هابیت", "سالار مکزیکی", "سالار الطایر", "سالار اوستا", "سالار هوندا*", "سالار هیچکس", "سالار تتلو", "سالار گرالت", "سالار ینیفر", "سالار قاتل", "سالار ققنوس", "سالار شاهین", "سالار دانته", "سالار لوسیفر", "سالار الکسیس", "هابیت لوگان", "هابیت هالک", "هابیت هیچکس", "هابیت سالار", "هابیت مکزیکی", "هابیت پریسا", "هابیت مونا", "اسمیگل", "هابیت هیچکس", "هابیت قاتل", "هابیت شاهین", "بگینز", "هابیت الکسیس", "هابیت فرودو", "رباب مهرنوش", "رباب لوگان", "رباب سارا", "رباب هالک", "رباب هیچکس", "رباب ممد", "رباب سالار", "رباب هابیت", "رباب مکزیکی", "رباب الطایر", "رباب پریسا", "رباب مونا", "رباب بیتاااا", "رباب اوستا", "رباب راپانزل", "رباب هوندا*", "رباب هیچکس", "رباب تتلو", "رباب یاس", "رباب گرالت", "رباب ینیفر", "رباب قاتل", "رباب امیرعلی", "رباب ققنوس", "رباب شاهین", "رباب سیندرلا", "رباب دانته", "رباب لویسفر", "رباب الکسیس", "رباب سام", "سامورایی الی", "سامورایی ممد", "پریسا لوگان", "پریسا هالک", "پریسا هیچکس", "پریسا ممد", "پریسا سالار", "پریسا هابیت", "پریسا مکزیکی", "پریسا رباب", "پریسا کرافت", "پریسا مونا", "پریسا هوندا*", "پریسا هیچکس", "پریسا تتلو", "پریسا یاس", "پریسا گرالت", "پریسا ینیفر", "پریسا قاتل", "پریسا ققنوس", "پریسا شاهین", "پریسا دانته", "پریسا لوسیفر", "پریسا الکسیس", "پریسا سام", "مونا مهرنوش", "مونا لوگان", "مونا سارا", "مونا هالک", "مونا دانیال", "مونا هیچکس", "کلمبیا", "مونا هابیت", "مونا مکزیکی", "مونا فمنیزم", "مونا سامورایی", "مونا الطایر", "مونا اوستا", "مونا راپانزل", "مونا هوندا*", "مونا هیچکس", "مونا تتلو", "مونا یاس", "مونا گرالت", "مونا ینیفر", "دختر بابا", "مونا قاتل", "مونا ماکیاولی", "مونا ققنوس", "مونا شاهین", "مونا سیندرلا", "مونا دانته", "مونا لوسیفر", "مونا الکسیس", "مونا سام", "راپانزل هالک", "راپانزل قاتل", "میتسوبیشی", "ناکازاکی", "عشق هوندا*", "هیچکس یاس", "هیچکس قاتل", "گرالت لوگان", "گرالت ممد", "گرالت سالار", "گرالت هابیت", "گرالت مکزیکی", "گرالت الطایر", "گرالت اوستا", "گرالت هوندا*", "گرالت تتلو", "گرالت ینیفر", "گرالت قاتل", "گرالت ققنوس", "گرالت دانته", "گرالت لویسفر", "گرالت الکسیس", "گرالت سام", "ینیفر لوگان", "ینیفر دانیال", "ینیفر هیچکس", "ینیفر ممد", "ینیفر سالار", "ینیفر هابیت", "ینیفر مکزیکی", "ینیفر گرالت", "ویچر", "ینیفر الکسیس", "ممدشون", "بنیتا", "امیرعلی سارا", "امیرعلی هالک", "امیرعلی کاظم", "امیرعلی هدیه", "امیرعلی الی", "امیرعلی ممد", "امیرعلی رباب", "امیرعلی مونا", "امیرعلی تتلو", "امیرعلی یاس", "امیرعلی قاتل", "امیرعلی سام", "ققنوس مهرنوش", "ققنوس سارا", "ققنوس الی", "ققنوس سالار", "ققنوس مکزیکی", "شاهین ممد", "شاهین سالار", "شاهین هابیت", "شاهین مکزیکی", "شاهین الطایر", "شاهین مونا", "شاهین اوستا", "شاهین هوندا*", "شاهین هیچکس", "شاهین تتلو", "شاهین یاس", "شاهین گرالت", "شاهین ینیفر", "شاهین قاتل", "شاهین ققنوس", "شاهین دانته", "شاهین لویسفر", "شاهین الکسیس", "شاهین سام", "سام لوگان", "سام ممد", "سام سالار", "سام هابیت", "سام مکزیکی", "سام رباب", "سام سامورایی", "سام الطایر", "سام هوندا*", "سام ینیفر", "سام قاتل", "سام ماکیاولی", "سام امیرعلی", "سام ققنوس", "سام شاهین", "سام دانته", "سام لویسفر", "سام الکسیس"};
        shuffleArray(names);
        Arena[] arenas = game.arenas.values();
        int index = 0;
        String nams = "\"";
        String nums = "";
        for (Arena a:arenas)
        {
            int len = (int) Math.floor(names.length/arenas.length);
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
}
