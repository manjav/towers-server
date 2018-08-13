package com.gt.challenges;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.protocol.serialization.DefaultSFSDataSerializer;

/**
 * Created by ManJav on 8/13/2018.
 */
public class LobbyData extends SFSObject
{
    public LobbyData()
    {
        super();
    }
    public LobbyData(int id, String name, int createAt, int capacity, byte[] members)
    {
        super();
        setId(id);
        setName(name);
        setCreateAt(createAt);
        setCapacity(capacity);
        setMembers(members);
    }


    /**
     * Id
     * @return
     */
    public int getId()
    {
        return getInt("id");
    }
    public void setId(int id)
    {
        putInt("id", id);
    }

    /**
     * Type
     * @return
     */
    public String getName()
    {
        return getUtfString("type");
    }
    private void setName(String name)
    {
        putUtfString("name", name);
    }

    /**
     * StartAt
     * @return
     */
    public int getCreateAt()
    {
        return getInt("start_at");
    }
    private void setCreateAt(int createAt)
    {
        putInt("create_at", createAt);
    }

    /**
     * all players who participate challenge
     * @return
     */
    public ISFSArray getMembers()
    {
        if( !containsKey("members") )
            setMembers(new SFSArray());

        return getSFSArray("members");
    }
    private void setMembers(ISFSArray members)
    {
        putSFSArray("members", members);
    }

    public byte[] getMembersBytes()
    {
        return DefaultSFSDataSerializer.getInstance().array2binary(getMembers());
    }
    private void setMembers(byte[] members)
    {
        if( members == null )
            setMembers(new SFSArray());
        else
            setMembers(DefaultSFSDataSerializer.getInstance().binary2array(members));
    }



    public int getCapacity()
    {
        return getInt("capacity");
    }
    private void setCapacity(int capacity)
    {
        putInt("capacity", capacity);
    }

    public boolean isFull()
    {
        return getMembers().size() >= getCapacity();
    }

}
