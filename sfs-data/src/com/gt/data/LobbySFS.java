package com.gt.data;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.protocol.serialization.DefaultSFSDataSerializer;

/**
 * Created by ManJav on 8/13/2018.
 */
public class LobbySFS extends SFSDataModel
{
    public LobbySFS(ISFSObject sfsObject)
    {
        super();
        setId(sfsObject.getInt("id"));
        setName(sfsObject.getUtfString("name"));
        setBio(sfsObject.getUtfString("bio"));
        setEmblem(sfsObject.getInt("emblem"));
        setCapacity(sfsObject.getInt("capacity"));
        setMinPoint(sfsObject.getInt("min_point"));
        setPrivacy(sfsObject.getInt("privacy"));
        if( sfsObject.containsKey("members") )
            setMembers(sfsObject.getByteArray("members"));
        if( sfsObject.containsKey("messages") )
            setMessages(sfsObject.getByteArray("messages"));
    }

    public LobbySFS(int id, String name, String bio, int emblem, int capacity, int minPoint, int privacy, byte[] members, byte[] message)//, int createAt
    {
        super();
        setId(id);
        setName(name);
        setBio(bio);
        setEmblem(emblem);
        setCapacity(capacity);
        setMinPoint(minPoint);
        setPrivacy(privacy);
        setMembers(members);
        setMessages(message);
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
     * Name
     * @return
     */
    public String getName()
    {
        return getUtfString("name");
    }
    public void setName(String name)
    {
        putUtfString("name", name);
    }

    /**
     * Bio
     * @return
     */
    public String getBio()
    {
        return getUtfString("bio");
    }
    public void setBio(String name)
    {
        putUtfString("bio", name);
    }

    /**
     * emblem
     * @return
     */
    public int getEmblem()
    {
        return getInt("emblem");
    }
    public void setEmblem(int emblem)
    {
        putInt("emblem", emblem);
    }

    /**
     * Min Point
     * @return
     */
    public int getMinPoint()
    {
        return getInt("min_point");
    }
    public void setMinPoint(int capacity)
    {
        putInt("min_point", capacity);
    }

    /**
     * Privacy
     * @return
     */
    public int getPrivacy()
    {
        return getInt("privacy");
    }
    public void setPrivacy(int privacy)
    {
        putInt("privacy", privacy);
    }

    /**
     * Capacity
     * @return
     */
    public int getCapacity()
    {
        return getInt("capacity");
    }
    public void setCapacity(int capacity)
    {
        putInt("capacity", capacity);
    }


    /**
     * CreateAt
     * @return
     */
    public int getCreateAt()
    {
        return getInt("create_at");
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
    public void setMembers(ISFSArray members)
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


    /**
     * last messages
     * @return
     */
    public ISFSArray getMessages()
    {
        if( !containsKey("messages") )
            setMessages(new SFSArray());

        return getSFSArray("messages");
    }
    public void setMessages(ISFSArray messages)
    {
        putSFSArray("messages", messages);
    }

    public byte[] getMessagesBytes()
    {
        return DefaultSFSDataSerializer.getInstance().array2binary(getMessages());
    }
    private void setMessages(byte[] messages)
    {
        if( messages == null )
            setMessages(new SFSArray());
        else
            setMessages(DefaultSFSDataSerializer.getInstance().binary2array(messages));
    }

    public boolean isFull()
    {
        return getMembers().size() >= getCapacity();
    }

}
