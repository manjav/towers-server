package com.grantech.towers.http;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.extensions.ISFSExtension;

@SuppressWarnings("serial")
public class ExtensionHTTPFacade extends HttpServlet
{
    private final static String CMD_SET_UM_TIME = "setumtime";
    private final static String CMD_SERVER_CHECK = "servercheck";
    private final static String CMD_RESET_KEY_LIMIT = "resetkeylimit";
    private final static String CMD_RESET_LOBBIES_ACTIVENESS = "resetlobbiesactiveness";
    private final static String CMD_GET_PLAYER_NAME_BY_IC = "getplayernamebyic";
    private final static String CMD_GET_LOBBY_NAME_BY_IC = "getlobbynamebyic";
    private final static String CMD_CUSTOM = "custom";

    private ISFSExtension myExtension;

    @Override
    public void init() throws ServletException
    {
        myExtension = SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        Object result = null;
        Map<String, String[]> params = req.getParameterMap();
        for ( String key : params.keySet() )
        {
            switch ( key )
            {
                case CMD_SET_UM_TIME:
                case CMD_RESET_KEY_LIMIT:
                case CMD_RESET_LOBBIES_ACTIVENESS:
                case CMD_SERVER_CHECK:
                case CMD_CUSTOM:
                case CMD_GET_PLAYER_NAME_BY_IC:
                case CMD_GET_LOBBY_NAME_BY_IC:
                    result = myExtension.handleInternalMessage(key, req.getParameter(key));
                    break;
                default:
                    result = "[" + key + "] not found. Please pass a list of comma separated values called 'numbers'. Example ?numbers=1,2,3,4";
            }
        }
        resp.setHeader("Content-Type", "text/plain; charset=utf-8");
        resp.getWriter().write(result.toString());
    }
}