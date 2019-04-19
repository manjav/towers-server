/**
 * Created by ManJav on 8/6/2018.
 */
package com.gt.data;

import com.gt.towers.socials.Attendee;
import com.gt.towers.socials.Challenge;
import com.gt.towers.utils.maps.IntArenaMap;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.protocol.serialization.DefaultSFSDataSerializer;
import haxe.root.Array;

public class ChallengeSFS extends SFSDataModel
{

    public Challenge base;
    public int saveRequests = 0;

    public ChallengeSFS()
    {
        super();
        base = new Challenge(0, 0);
    }
    public ChallengeSFS(int id, int type, int mode, int startAt, byte[] attendees)
    {
        super();
        base = new Challenge(0, 0);
        setId(id);
        setType(type);
        setMode(mode);
        setStartAt(startAt);
        setUnlockAt(Challenge.getUnlockAt(type));
        setDuration(Challenge.getDuration(type));
        setCapacity(Challenge.getCapacity(type));
        setRewards(Challenge.getRewards(type));
        setRequirements(0, Challenge.getJoinRequiements(type));
        setRequirements(1, Challenge.getRunRequiements(type));
        setAttendees(attendees);
    }

    /**
     * Id
     * @return
     */
    public void setId(int id)
    {
        putInt("id", id);
        base.id = id;
    }

    /**
     * Type
     * @return
     */
    private void setType(int type)
    {
        putInt("type", type);
        base.type = type;
    }

    /**
     * Mode
     * @return
     */
    private void setMode(int mode)
    {
        putInt("mode", mode);
        base.mode = mode;
    }

    /**
     * StartAt
     * @return
     */
    private void setStartAt(int startAt)
    {
        putInt("start_at", startAt);
        base.startAt = startAt;
    }

    /**
     * UnlockAt
     * @return
     */
    private void setUnlockAt(int unlockAt)
    {
        putInt("unlock_at", unlockAt);
        base.unlockAt = unlockAt;
    }

    /**
     * Duration
     * @return
     */
    private void setDuration(int duration)
    {
        base.duration = duration;
        putInt("duration", base.duration);
    }


    /**
     * Rewards
     * @return
     */
    private void setRewards(IntArenaMap rewards)
    {
        ISFSObject sfs;
        ISFSArray ret = new SFSArray();
        int[] keys = rewards.keys();
        int i = 0;
        while ( i < keys.length )
        {
            sfs = new SFSObject();
            sfs.putInt("key", keys[i]);
            sfs.putInt("min", rewards.get(keys[i]).min);
            sfs.putInt("max", rewards.get(keys[i]).max);
            sfs.putInt("prize", rewards.get(keys[i]).minWinStreak);
            ret.addSFSObject(sfs);
            i ++;
        }
        putSFSArray("rewards", ret);
        base.rewards = rewards;
    }


    /**
     * Requirements
     * @return
     */
    private void setRequirements(int type, IntIntMap requirements)
    {
        if( type == 0 )
        {
            setMap("joinRequirements", requirements);
            base.joinRequirements = requirements;
        }
        else if( type == 1 )
        {
            setMap("runRequirements", requirements);
            base.runRequirements = requirements;
        }
    }

    /**
     * all players who participate challenge
     * @return
     */
    public ISFSArray getAttendees()
    {
        if( !containsKey("attendees") )
            setAttendees(new SFSArray());

        return getSFSArray("attendees");
    }
    private void setAttendees(ISFSArray attendees)
    {
        putSFSArray("attendees", attendees);
    }

    public byte[] getAttendeesBytes()
    {
        return DefaultSFSDataSerializer.getInstance().array2binary(getAttendees());
    }
    private void setAttendees(byte[] attendees)
    {
        if( attendees == null )
            setAttendees(new SFSArray());
        else
            setAttendees(DefaultSFSDataSerializer.getInstance().binary2array(attendees));
    }

    public void representAttendees()
    {
        if( base.attendees == null || base.attendees.length != getAttendees().size() )
            base.attendees = new Array<Attendee>();

        if( base.attendees.length == getAttendees().size() )
            return;

        ISFSObject att;
        for (int a = 0; a < getAttendees().size(); a++)
        {
            att = getAttendees().getSFSObject(a);
            base.attendees.push(new Attendee(att.getInt("id"), att.getText("name"), att.getInt("point"), att.getInt("updateAt")));
        }
    }

    private void setCapacity(int capacity)
    {
        putInt("capacity", capacity);
        base.capacity = capacity;
    }

    public boolean isFull()
    {
        return base.capacity > 0 && base.capacity <= getAttendees().size() ;
    }
    public boolean isAvailabled(int now)
    {
        if( isFull() )
            return false;
        if( base.type == Challenge.TYPE_2_RANKING )
            return base.getState(now) == Challenge.STATE_0_WAIT;
        return base.getState(now) < Challenge.STATE_2_END;
    }
}
