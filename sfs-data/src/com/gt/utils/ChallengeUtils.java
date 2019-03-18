package com.gt.utils;

import com.gt.data.ChallengeSFS;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.socials.Challenge;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 8/2/2018.
 */
public class ChallengeUtils extends UtilBase
{
    public static ChallengeUtils getInstance()
    {
        return (ChallengeUtils)UtilBase.get(ChallengeUtils.class);
    }
    public void loadAll()
    {
        if( ext.getParentZone().containsProperty("allChallenges") )
            return;

        int now = (int) Instant.now().getEpochSecond();
        ISFSArray challenges = new SFSArray();
        try {
            challenges = ext.getParentZone().getDBManager().executeQuery("SELECT * FROM challenges WHERE start_at > NOW() - INTERVAL 1 WEEK;", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        Map<Integer, ChallengeSFS> allChallenges = new ConcurrentHashMap();
        Map<Integer, ChallengeSFS> waitingChallenges = new ConcurrentHashMap();
        ISFSObject ch = null;
        ChallengeSFS challengeSFS;
        for( int i=0; i<challenges.size(); i++ )
        {
            ch = challenges.getSFSObject(i);
            challengeSFS = new ChallengeSFS(ch.getInt("id"), ch.getInt("type"), 0, Math.toIntExact(ch.getLong("start_at") / 1000), ch.getByteArray("attendees"));
            allChallenges.put(ch.getInt("id"), challengeSFS);

            if( challengeSFS.inWaiting(now) )
                waitingChallenges.put(challengeSFS.base.type, challengeSFS);
        }

        ext.getParentZone().setProperty("allChallenges", allChallenges);
        ext.getParentZone().setProperty("waitingChallenges", waitingChallenges);
        trace("loaded challenges data in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }
    public ConcurrentHashMap<Integer, ChallengeSFS> getAll()
    {
        return (ConcurrentHashMap<Integer, ChallengeSFS>) ext.getParentZone().getProperty("allChallenges");
    }

    public ChallengeSFS get(int challengeId)
    {
        ConcurrentHashMap<Integer, ChallengeSFS> all = getAll();
        if( !all.containsKey(challengeId) )
            return null;
        return all.get(challengeId);
    }
    private ChallengeSFS getWaiting(int type, int now)
    {
        ConcurrentHashMap<Integer, ChallengeSFS> ws = (ConcurrentHashMap<Integer, ChallengeSFS>) ext.getParentZone().getProperty("waitingChallenges");
        // if not exists any waiting challenge, then create new challenge of type requested.
        if( !ws.containsKey(type) || !ws.get(type).inWaiting(now) )
            ws.put(type, create(type, now));
        return ws.get(type);
    }

    private ChallengeSFS create(int type, int now)
    {
        int startAt = now + Challenge.getWaitTime(type);
        ChallengeSFS challenge = new ChallengeSFS(-1, type, 0, startAt, null);
        String query = "INSERT INTO challenges (type, start_at, attendees) VALUES (" + type + ", FROM_UNIXTIME(" + startAt + "), ?);";
        try {
            challenge.setId(Math.toIntExact((Long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{challenge.getAttendeesBytes()})));
        } catch (SQLException e) {  e.printStackTrace(); }

        getAll().put(challenge.base.id, challenge);
        return  challenge;
    }

    public ChallengeSFS join(int type, int playerId, String playerName, int now)
    {
        return join(getWaiting(type, now), playerId, playerName, now);
    }
    public ChallengeSFS join(ChallengeSFS challenge, int playerId, String playerName, int now)
    {
        ISFSObject attendee = new SFSObject();
        attendee.putInt("id", playerId);
        attendee.putInt("updateAt", now);
        attendee.putInt("point", 0);
        attendee.putText("name", playerName);
        challenge.getAttendees().addSFSObject(attendee);
        save(challenge);
        return challenge;
    }

    private void save(ChallengeSFS challenge)
    {
        String query = "UPDATE challenges SET attendees = ? WHERE id = " + challenge.base.id;
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{challenge.getAttendeesBytes()});
        } catch (SQLException e) {  e.printStackTrace(); }
        challenge.saveRequests = 0;
    }


    public ISFSArray getChallengesOfAttendee(int type, Player player, boolean createIfNotExists)
    {
        ISFSObject attendee;
        Map<Integer, Boolean> types = new HashMap();
        ISFSArray ret = new SFSArray();
        Iterator<Map.Entry<Integer, ChallengeSFS>> iterator = getAll().entrySet().iterator();
        while( iterator.hasNext() )
        {
            ChallengeSFS challenge = iterator.next().getValue();
            if( type > -1 && challenge.base.type != type )
                continue;
            for(int i=0; i < challenge.getAttendees().size(); i++)
            {
                attendee = challenge.getAttendees().getSFSObject(i);
                if( attendee.getInt("id").equals(player.id) && attendee.getInt("updateAt") > -1 )
                {
                    ret.addSFSObject(challenge);
                    types.put(challenge.base.type, true);
                    break;
                }
            }
        }
        if( createIfNotExists )
        {
            int now = (int) Instant.now().getEpochSecond();

            if( !types.containsKey(0) )
                ret.addSFSObject(getWaiting(0, now));

            if( !types.containsKey(1) )
            {
                ChallengeSFS ch = getWaiting(1, now);
                join(ch, player.id, player.nickName, now );
                ret.addSFSObject(ch);
            }
        }

        return ret;
    }


    public int collectReward(int challengeId, int playerId, int now)
    {
        ChallengeSFS challenge = get(challengeId);
        if( challenge == null )
            return MessageTypes.RESPONSE_NOT_FOUND;

        if( challenge.base.getState(now) != Challenge.STATE_END )
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

    public void scheduleSave(ChallengeSFS challenge)
    {
        challenge.saveRequests ++;
        if( challenge.saveRequests > 5 )
            save(challenge);
    }
}