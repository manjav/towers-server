package com.gerantech.towers.sfs.challenges;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.protocol.serialization.DefaultSFSDataSerializer;

import java.time.Instant;

public class Challenge extends SFSObject
{

    public com.gt.towers.socials.Challenge base;

    public Challenge()
    {
        super();
        base = new com.gt.towers.socials.Challenge();
    }
    public Challenge(Integer id, Long startAt, byte[] attendees)
    {
        super();
        base = new com.gt.towers.socials.Challenge();
        setId(id);
        setStartAt(startAt);
        setAttendees(attendees);
    }

    public int getId()
    {
        return containsKey("id") ? getInt("id") : -1;
    }
    public void setId(int id)
    {
        putInt("id", id);
        base.id = id;
    }

    public Long getStartAt()
    {
        return containsKey("start_at") ? getLong("start_at") : -1;
    }
    public void setStartAt(Long startAt)
    {
        base.startAt = Math.toIntExact(startAt / 1000l);
        putInt("start_at", base.startAt);
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
    public void setAttendees(ISFSArray attendees)
    {
        putSFSArray("attendees", attendees);
    }

    public byte[] getAttendeesBytes()
    {
        return DefaultSFSDataSerializer.getInstance().array2binary(getAttendees());
    }
    public void setAttendees(byte[] attendees)
    {
        setAttendees(DefaultSFSDataSerializer.getInstance().binary2array(attendees));
    }

    public boolean isFull()
    {
        return getAttendees().size() >= base.capacity;
    }
}
