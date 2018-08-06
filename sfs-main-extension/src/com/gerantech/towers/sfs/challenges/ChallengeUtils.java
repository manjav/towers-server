package com.gerantech.towers.sfs.challenges;

import com.gt.challenges.ChallengeSFS;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 8/2/2018.
 */
public class ChallengeUtils
{
    private static ChallengeUtils _instance;
    private final SFSExtension ext;
    public ChallengeUtils() {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static ChallengeUtils getInstance()
    {
        if( _instance == null )
            _instance = new ChallengeUtils();
        return _instance;
    }

    public void loadAll()
    {
        if( ext.getParentZone().containsProperty("challengesData") )
            return;

        ISFSArray challenges = new SFSArray();
        try {
            challenges = ext.getParentZone().getDBManager().executeQuery("SELECT * FROM challenges WHERE start_at > NOW() - INTERVAL 1 WEEK;", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        Map<Integer, ChallengeSFS> challengesData = new ConcurrentHashMap();
        ISFSObject ch = null;
        for( int i=0; i<challenges.size(); i++ )
        {
            ch = challenges.getSFSObject(i);
            challengesData.put(ch.getInt("id"), new ChallengeSFS(ch.getInt("id"), ch.getInt("type"),  Math.toIntExact(ch.getLong("start_at") / 1000), ch.getByteArray("attendees")));
        }

        ext.getParentZone().setProperty("challengesData", challengesData);
        ext.trace("loaded challenges data in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }
    public ConcurrentHashMap<Integer, ChallengeSFS> getAll()
    {
        return (ConcurrentHashMap<Integer, ChallengeSFS>) ext.getParentZone().getProperty("challengesData");
    }
    public ChallengeSFS get(int challengeId)
    {
        ConcurrentHashMap<Integer, ChallengeSFS> challenges = getAll();
        if( !challenges.containsKey(challengeId) )
            return null;
        return getAll().get(challengeId);
    }
    public ISFSArray getChallengesOfAttendee(int type, int attendeeId)
    {
        ISFSArray ret = new SFSArray();
        ISFSArray all;
        ISFSObject member;
        ConcurrentHashMap<Integer, ChallengeSFS> lobbiesData = getAll();

        Iterator<Map.Entry<Integer, ChallengeSFS>> iterator = lobbiesData.entrySet().iterator();
        while( iterator.hasNext() )
        {
            ChallengeSFS challenge = iterator.next().getValue();
            if( type > -1 && challenge.base.type != type )
                continue;
            all = challenge.getAttendees();
            for(int i=0; i<all.size(); i++)
            {
                member = all.getSFSObject(i);
                if( member.getInt("id").equals(attendeeId) && member.getInt("updateAt") > -1 )
                {
                    ret.addSFSObject(challenge);
                    break;
                }
            }
        }
        return ret;
    }

    public ChallengeSFS findWaitingChallenge(int type, int now)
    {
        ConcurrentHashMap<Integer, ChallengeSFS> challenges = ChallengeUtils.getInstance().getAll();
        for( Map.Entry<Integer, ChallengeSFS> entry : challenges.entrySet() )
        {
            ChallengeSFS challenge = entry.getValue();
            if( challenge.base.type == type && challenge.base.getState(now) == com.gt.towers.socials.Challenge.STATE_WAIT && !challenge.isFull() )
                return challenge;
        }

        return null;
    }

    public ChallengeSFS create(int type, int capacity, int startAt, int now, Player player)
    {
        ChallengeSFS challenge = new ChallengeSFS();
        challenge.setType(type);
        challenge.setStartAt(startAt);
        challenge.base.capacity = capacity;
        join(challenge, player, now, false);
        String query = "INSERT INTO challenges (type, start_at, attendees) VALUES (" + type + ", FROM_UNIXTIME(" + startAt + "), ?);";
        try {
            challenge.setId(Math.toIntExact((Long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{challenge.getAttendeesBytes()})));
        } catch (SQLException e) {  e.printStackTrace(); }

        getAll().put(challenge.getId(), challenge);
        return  challenge;
    }

    private void save(ChallengeSFS challenge)
    {
        String query = "UPDATE challenges SET attendees = ? WHERE id = " + challenge.getId();
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{challenge.getAttendeesBytes()});
        } catch (SQLException e) {  e.printStackTrace(); }
    }

    public void join(ChallengeSFS challenge, Player player, int now, boolean saveToDB)
    {
        ISFSObject attendee = new SFSObject();
        attendee.putInt("id", player.id);
        attendee.putInt("updateAt", now);
        attendee.putInt("point", 0);
        attendee.putText("name", player.nickName);
        challenge.getAttendees().addSFSObject(attendee);
        if( !saveToDB )
            return;

        save(challenge);
    }

    public int collectReward(int challengeId, int playerId, int now)
    {
        ChallengeSFS challenge = get(challengeId);
        if( challenge == null )
            return MessageTypes.RESPONSE_NOT_FOUND;

        if( challenge.base.getState(now) != com.gt.towers.socials.Challenge.STATE_END )
            return MessageTypes.RESPONSE_MUST_WAIT;

        ISFSObject attendee = getAttendee(playerId, challenge);
        if( attendee == null )
            return MessageTypes.RESPONSE_NOT_ALLOWED;

        if( attendee.getInt("updateAt") <= -1 )
            return MessageTypes.RESPONSE_ALREADY_SENT;
        attendee.putInt("updateAt", -1);

        save(challenge);
        return MessageTypes.RESPONSE_SUCCEED;
    }

    public ISFSObject getAttendee(int playerId, ChallengeSFS challenge)
    {
        ISFSArray attendees = challenge.getAttendees();
        for(int i=0; i<attendees.size(); i++)
            if( attendees.getSFSObject(i).getInt("id").equals(playerId) )
                return attendees.getSFSObject(i);
        return null;
    }
}