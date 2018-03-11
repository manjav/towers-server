package com.grantech.towers.http;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.extensions.ISFSExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@SuppressWarnings("serial")
public class ExtensionHTTPFacade extends HttpServlet
{
    private final static String CMD_SET_UM_TIME = "setumtime";
    private final static String CMD_SERVER_CHECK = "servercheck";
    private final static String CMD_RESET_KEY_LIMIT = "resetkeylimit";
    private final static String CMD_RESET_LOBBIES_ACTIVENESS = "resetlobbiesactiveness";
    private final static String CMD_GET_PLAYER_NAME_BY_IC = "getplayernamebyic";
    private final static String CMD_GET_LOBBY_NAME_BY_ID = "getlobbynamebyid";
    private final static String CMD_BAN = "ban";
    private final static String CMD_CUSTOM = "custom";

    private ISFSExtension myExtension;

    @Override
    public void init() throws ServletException
    {
        myExtension = SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Object result = null;
        Map<String, String[]> params = request.getParameterMap();
        for ( String key : params.keySet() )
        {
            System.out.println("key: " + key);
            switch ( key )
            {
                case CMD_SET_UM_TIME:
                case CMD_RESET_KEY_LIMIT:
                case CMD_RESET_LOBBIES_ACTIVENESS:
                case CMD_SERVER_CHECK:
                case CMD_CUSTOM:
                case CMD_BAN:
                    result = myExtension.handleInternalMessage(key, request.getParameter(key));
                    break;
                case CMD_GET_PLAYER_NAME_BY_IC:
                case CMD_GET_LOBBY_NAME_BY_ID:
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-cache");
                    result = "callBackFunc(" + myExtension.handleInternalMessage(key, request.getParameter(key))+ ");";
                    break;
                case "callback":
                case "_":
                    break;
                default:
                    result = "[" + key + "] not found. Please pass a list of comma separated values called 'numbers'. Example ?numbers=1,2,3,4";
            }
        }
        response.getWriter().print(result);
    }
}
