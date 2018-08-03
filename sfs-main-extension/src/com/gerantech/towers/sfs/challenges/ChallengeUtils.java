package com.gerantech.towers.sfs.challenges;

import com.gt.towers.Player;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

        Map<Integer, Challenge> challengesData = new HashMap();
        ISFSObject ch = null;
        for( int i=0; i<challenges.size(); i++ )
        {
            ch = challenges.getSFSObject(i);
            challengesData.put(ch.getInt("id"), new Challenge(ch.getInt("id"), ch.getLong("start_at"), ch.getByteArray("attendees")));
        }

        ext.getParentZone().setProperty("challengesData", challengesData);
        ext.trace("loaded challenges data in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }
    public Map<Integer, Challenge> getAll()
    {
        return (Map<Integer, Challenge>) ext.getParentZone().getProperty("challengesData");
    }
    public Challenge get(String name)
    {
        return getAll().get(name);
    }
    public Challenge getByAttendee(int attendeeId)
    {
        ISFSArray all;
        ISFSObject member;
        Map<Integer, Challenge> lobbiesData = getAll();
        for (Map.Entry<Integer, Challenge> entry : lobbiesData.entrySet())
        {
            all = entry.getValue().getAttendees();
            for(int i=0; i<all.size(); i++)
            {
                member = all.getSFSObject(i);
                if( member.getInt("id").equals(attendeeId) )
                    return  entry.getValue();
            }
        }
        return null;
    }

    public Challenge findWaitingChallenge(int type, int now)
    {
        Map<Integer, Challenge> challenges = ChallengeUtils.getInstance().getAll();
        for( Map.Entry<Integer, Challenge> entry : challenges.entrySet() )
            if( entry.getValue().base.type == type && entry.getValue().base.getState(now) == com.gt.towers.socials.Challenge.STATE_WAIT && !entry.getValue().isFull() )
                return entry.getValue();
        return null;
    }

    public Challenge create(int type, int capacity, long startAt, Player player)
    {
        Challenge challenge = new Challenge();
        challenge.base.capacity = capacity;
        challenge.base.type = type;
        challenge.setStartAt(startAt);
        join(challenge, player, false);
        String query = "INSERT INTO challenges (type, start_at, attendees) VALUES (0, FROM_UNIXTIME(" + startAt + "), ?);";
        try {
            challenge.setId(Math.toIntExact((Long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{challenge.getAttendeesBytes()})));
        } catch (SQLException e) {  e.printStackTrace(); }

        getAll().put(challenge.getId(), challenge);
        return  challenge;
    }

    public void join(Challenge challenge, Player player, boolean saveToDB)
    {
        ISFSObject attendee = new SFSObject();
        attendee.putInt("id", player.id);
        attendee.putInt("lu", 0);
        attendee.putInt("point", 0);
        attendee.putText("name", player.nickName);
        challenge.getAttendees().addSFSObject(attendee);
        if( !saveToDB )
            return;

        String query = "UPDATE challenges attendees = ? WHERE id = " + challenge.getId();
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{challenge.getAttendeesBytes()});
        } catch (SQLException e) {  e.printStackTrace(); }
    }
}