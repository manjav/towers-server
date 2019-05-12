package com.gerantech.towers.sfs.socials.handlers;

import com.gt.Commands;
import com.gt.utils.BanUtils;
import com.gt.utils.InboxUtils;
import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gt.utils.LobbyUtils;
import com.gt.utils.OneSignalUtils;
import com.gt.data.LobbySFS;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

/**
 * Created by ManJav on 8/28/2017.
 */
public class LobbyModerationHandler extends BaseClientRequestHandler
{
    private Game game;
    private Room lobby;
    private LobbyRoom lobbyRoom;
    private ISFSArray members;
    private ISFSObject targetMember;
    private ISFSObject modMember;
    private LobbySFS lobbyData;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        game = ((Game) sender.getSession().getProperty("core"));
        lobby = getParentExtension().getParentRoom();
        lobbyRoom = (LobbyRoom) lobby.getExtension();
        lobbyData = lobbyRoom.getData();
        members = lobbyData.getMembers();
        targetMember = null;
        modMember = null;
        int action = params.getInt("pr");
        int allSize = members.size();
        for (int i = 0; i < allSize; i++)
        {
            if( members.getSFSObject(i).getInt("id").equals(params.getInt("id")) )
                targetMember = members.getSFSObject(i);
            else if( members.getSFSObject(i).getInt("id").equals(game.player.id) )
                modMember = members.getSFSObject(i);
        }

        boolean succeed = false;
        if( targetMember == null || modMember == null )
        {
            params.putBool("succeed", succeed);
            send(Commands.LOBBY_MODERATION, params, sender);
            return;
        }

        if( action == MessageTypes.M12_COMMENT_KICK )
            succeed = kick(params.getInt("id"), params.getUtfString("name"));
        else if( action == MessageTypes.M13_COMMENT_PROMOTE )
            succeed = promote(params.getInt("id"), params.getUtfString("name"));
        else if( action == MessageTypes.M14_COMMENT_DEMOTE )
            succeed = demote(params.getInt("id"), params.getUtfString("name"));

        params.putBool("succeed", succeed);
        send(Commands.LOBBY_MODERATION, params, sender);
    }

    private boolean kick(Integer targetId, String targetName)
    {
        if( modMember.getInt("pr") <= targetMember.getInt("pr") )
            return false;

        User targetUser = lobby.getUserByName(targetId.toString());
        if( targetUser != null )
            getApi().leaveRoom(targetUser, lobby, true, false);

        lobbyRoom.sendComment( MessageTypes.M12_COMMENT_KICK, game.player, targetName, -1);// mode = leave
        LobbyUtils.getInstance().removeUser(lobbyData, targetId);
        InboxUtils.getInstance().send(MessageTypes.M0_TEXT, "متأسفانه از دهکده اخراج شدی.", BanUtils.SYSTEM_ID, targetId, "");
        OneSignalUtils.getInstance().send(targetName + " متأسفانه " + game.player.nickName + " تو رو از دهکده اخراج کرد.", null, targetId);
        return true;
    }

    private boolean promote(Integer targetId, String targetName)
    {
        if( modMember.getInt("pr") <= targetMember.getInt("pr") + 1 || targetMember.getInt("pr") >= DefaultPermissionProfile.MODERATOR.getId() )
            return false;

        changePermission(targetMember.getInt("pr") + 1);
        lobbyRoom.sendComment( MessageTypes.M13_COMMENT_PROMOTE, game.player, targetName, targetMember.getInt("pr"));// mode = leave
        InboxUtils.getInstance().send(MessageTypes.M50_URL, "تبریک " + targetName + "، تو توسط " + game.player.nickName + " ریش سپید شدی!", BanUtils.SYSTEM_ID, targetId, "towers://open?controls=tabs&dashTab=3&socialTab=0");
        OneSignalUtils.getInstance().send("تبریک " + targetName + "، تو توسط " + game.player.nickName + " ریش سپید شدی!", null, targetId);
        return true;
    }

    private boolean demote(Integer targetId, String targetName)
    {
        if( modMember.getInt("pr") <= targetMember.getInt("pr") || targetMember.getInt("pr") < DefaultPermissionProfile.STANDARD.getId() )
            return false;

        changePermission(targetMember.getInt("pr") - 1);
        lobbyRoom.sendComment( MessageTypes.M14_COMMENT_DEMOTE, game.player, targetName, targetMember.getInt("pr"));// mode = leave
        //InboxUtils.getInstance().send(MessageTypes.M50_URL, game.player.nickName + " درجه تو رو به سرباز کاهش داد. ", game.player.nickName, game.player.id, targetId, "towers://open?controls=tabs&dashTab=3&socialTab=0");
        //OneSignalUtils.getInstance().send(targetName + "، " + game.player.nickName + " درجه تو رو به سرباز کاهش داد. ", null, targetId);
        return true;
    }

    private void changePermission(int permission)
    {
        int index = LobbyUtils.getInstance().getMemberIndex(lobbyData, targetMember.getInt("id"));
        if( index < 0 )
            return;
        lobbyData.getMembers().getSFSObject(index).putInt("pr", permission);
        LobbyUtils.getInstance().save(lobbyData, null, null,-1,-1,-1, -1, lobbyData.getMembersBytes(), null);
    }
}