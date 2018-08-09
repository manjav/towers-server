/**
 * Created by ManJav on 8/6/2018.
 */
package com.gt.challenges;

import com.gt.towers.socials.Challenge;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.protocol.serialization.DefaultSFSDataSerializer;

public class ChallengeSFS extends SFSObject
{

    public com.gt.towers.socials.Challenge base;

    public ChallengeSFS()
    {
        super();
        base = new com.gt.towers.socials.Challenge();
    }
    public ChallengeSFS(int id, int type, int startAt, byte[] attendees)
    {
        super();
        base = new Challenge();
        setId(id);
        setType(type);
        setStartAt(startAt);
        setDuration(Challenge.getDuration(type));
        setCapacity(Challenge.getCapacity(type));
        setRewards(Challenge.getRewards(type));
        setRequirements(Challenge.getRequiements(type));
        setAttendees(attendees);
    }

    /**
     * Id
     * @return
     */
/*    public int getId()
    {
        return getInt("id");
    }*/
    public void setId(int id)
    {
        putInt("id", id);
        base.id = id;
    }

    /**
     * Type
     * @return
     */
    /*public int getType()
    {
        return getInt("type");
    }*/
    private void setType(int type)
    {
        putInt("type", type);
        base.type = type;
    }

    /**
     * StartAt
     * @return
     */
    /*public int getStartAt()
    {
        return getInt("start_at");
    }*/
    private void setStartAt(int startAt)
    {
        base.startAt = startAt;
        putInt("start_at", base.startAt);
    }

    /**
     * Duration
     * @return
     */
    /*public int getDuration()
    {
        return getInt("duration");
    }*/
    private void setDuration(int duration)
    {
        base.duration = duration;
        putInt("duration", base.duration);
    }


    /**
     * Rewards
     * @return
     */
    private void setRewards(IntIntMap rewards)
    {
        setMap("rewards", rewards);
        base.rewards = rewards;
    }


    /**
     * Requirements
     * @return
     */
    /*public ISFSArray getRequirements()
    {
        return getSFSArray("requirements");
    }*/
    private void setRequirements(IntIntMap requirements)
    {
        setMap("requirements", requirements);
        base.requirements = requirements;
    }

    private void setMap(String name, IntIntMap requirements)
    {
        ISFSObject sfs;
        ISFSArray ret = new SFSArray();
        int[] keys = requirements.keys();
        int i = 0;
        while ( i < keys.length )
        {
            sfs = new SFSObject();
            sfs.putInt("key", keys[i]);
            sfs.putInt("value", requirements.get(keys[i]));
            ret.addSFSObject(sfs);
            i ++;
        }
        putSFSArray(name, ret);
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

    /*public int getCapacity()
    {
        return getInt("capacity");
    }*/
    private void setCapacity(int capacity)
    {
        putInt("capacity", capacity);
        base.capacity = capacity;
    }

    public boolean isFull()
    {
        return getAttendees().size() >= base.capacity;
    }
    public boolean inWaiting(int now)
    {
        return getAttendees().size() < base.capacity && base.getState(now) == Challenge.STATE_WAIT;
    }
}
