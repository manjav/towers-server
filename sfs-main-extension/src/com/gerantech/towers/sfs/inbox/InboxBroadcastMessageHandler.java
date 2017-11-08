package com.gerantech.towers.sfs.inbox;

import com.gerantech.towers.sfs.utils.OneSignalUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by Babak on 17/11/08.
 */
public class InboxBroadcastMessageHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User user, ISFSObject params) {
        Collection<Integer> receivers = params.getIntArray("receivers");
        Integer[] receiverIds = receivers.toArray(new Integer[receivers.size()]);
        Iterator<Integer> it = receivers.iterator();
        for (int i = 0; i < receiverIds.length; i++) {

            InboxUtils.getInstance().send(params.getShort("type"),params.getUtfString("text"), "TowerStory", 10000, receiverIds[i], params.getText("data"));
        }
        if ( params.getBool("isPush") )
        {
            OneSignalUtils.getInstance().send(params.getUtfString("text"), params.getText("data"), receiverIds);
        }
    }
}
