/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 * <p/>
 * This file is part of OpenNetHome  (http://www.nethome.nu)
 * <p/>
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nu.nethome.home.items.web.servergui;

import nu.nethome.home.item.*;
import nu.nethome.home.items.infra.Plan;
import nu.nethome.home.items.web.HomeWebServer;
import nu.nethome.home.system.DirectoryEntry;
import nu.nethome.home.system.Event;
import nu.nethome.home.system.FinalEventListener;
import nu.nethome.home.system.HomeService;
import nu.nethome.util.plugin.Plugin;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Presents a WEB-GUI for the NetHomeServer. This class is the HomeItem, which is created and holds
 * basic configuration. This Item is also the actual HttpServlet which prints the WEB-pages.
 * This class prints the header of the interface with a navigation bar. The printing of the actual
 * content pages is delegated to plugin-classes, one class per page. These plugins are currently
 * instantiated in the activate()-method.
 *
 * @author Stefan Stromberg
 */
@SuppressWarnings("UnusedDeclaration")
@Plugin
@HomeItemType("GUI")
public class HomeGUI extends HttpServlet implements FinalEventListener, HomeItem {

    private static final int MS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"HomeGUI\" Category=\"GUI\" >"
            + "  <Attribute Name=\"WEBServer\" Type=\"Item\" Get=\"getWEBServer\" 	Set=\"setWEBServer\" />"
            + "  <Attribute Name=\"LocalURL\" Type=\"String\" Get=\"getLocalURL\" 	Set=\"setLocalURL\" Default=\"true\" />"
            + "  <Attribute Name=\"LeftBanner\" Type=\"String\" Get=\"getCustomLeftBannerFile\" 	Set=\"setCustomLeftBannerFile\" />"
            + "  <Attribute Name=\"RightBanner\" Type=\"String\" Get=\"getCustomRightBannerFile\" 	Set=\"setCustomRightBannerFile\" />"
            + "  <Attribute Name=\"PlanPage\" Type=\"Item\" Get=\"getPlanPage\" 	Set=\"setPlanPage\" />"
            + "  <Attribute Name=\"Location\" Type=\"Item\" Get=\"getDefaultLocation\" 	Set=\"setDefaultLocation\" />"
            + "  <Attribute Name=\"AllowEdit\" Type=\"Boolean\" Get=\"getAllowEdit\" 	Set=\"setAllowEdit\" />"
            + "</HomeItem> ");
    public static final int REPORT_ITEMS_PERIOD_MS = 60000;
    public static final int MAX_EVENT_ID_LENGTH = 60;

    protected HomeService homeServer;

    protected String m_Name = "NoNameYet";
    protected long m_ID = 0L;
    static final long serialVersionUID = 1;
    protected LinkedList<HomePageInterface> pages = new LinkedList<>();

    // Public attributes
    protected String webServer = "JettyWEB";
    protected String localURL = "/home";
    private String customLeftBannerFile = "";
    private String customRightBannerFile = "";
    private String defaultPlanPage = "HomePlan";
    private String defaultLocation = "";
    private String mediaDirectory = "";
    private boolean allowEdit = true;
    private CreationEventCache creationEvents = new CreationEventCache();
    private long lastItemReportTime = 0L;

    public HomeGUI() {
    }

    /* (non-Javadoc)
      * @see ssg.home.HomeItem#getModel()
      */
    public String getModel() {
        return MODEL;
    }

    /* (non-Javadoc)
      * @see ssg.home.HomeItem#getName()
      */
    public String getName() {
        return m_Name;
    }

    /* (non-Javadoc)
      * @see nu.nethome.home.HomeItem#getID()
      */
    public long getItemId() {
        return m_ID;
    }

    /* (non-Javadoc)
      * @see nu.nethome.home.HomeItem#setID(long)
      */
    public void setItemId(long id) {
        m_ID = id;
    }

    /* (non-Javadoc)
      * @see ssg.home.HomeItem#setName(java.lang.String)
      */
    public void setName(String name) {
        m_Name = name;
    }

    public boolean receiveEvent(Event event) {
        return false;
    }


    public void activate(HomeService hserver) {
        homeServer = hserver;
        // On activation we register this WEB-Component at the specified WEB-Server
        HomeItemProxy webServerItem = homeServer.openInstance(webServer);
        if (webServerItem != null) {
            Object possibleWebServer = webServerItem.getInternalRepresentation();
            if (possibleWebServer != null && possibleWebServer instanceof HomeWebServer) {
                ((HomeWebServer) possibleWebServer).registerServlet(localURL, this);
                mediaDirectory = ((HomeWebServer) possibleWebServer).getMediaDirectory();
            }
        }
        pages.add(new PlanPage(localURL, getDefaultPlanAccessor(), mediaDirectory));
        pages.add(new RoomsPage(localURL, getDefaultLocationAccessor()));
        pages.add(new ServerFloor(localURL));
        pages.add(new EditItemPage(localURL, homeServer, mediaDirectory, creationEvents, getEditPermission()));
        pages.add(new SettingsBasePage(localURL, homeServer, mediaDirectory, getEditPermission()));
        pages.add(new GraphPage(localURL, homeServer));
        homeServer.registerFinalEventListener(this);
        creationEvents.addItemInfo(hserver.listClasses());
    }

    private EditPermission getEditPermission() {
        return new EditPermission() {
            @Override
            public boolean isEditPermitted() {
                return allowEdit;
            }
        };
    }

    private DefaultPageIdentity getDefaultLocationAccessor() {
        return new DefaultPageIdentity() {
            public String getDefaultPage() {
                return defaultLocation;
            }

            public void setDefaultPage(String location) {
                defaultLocation = location;
            }
        };
    }

    private DefaultPageIdentity getDefaultPlanAccessor() {
        return new DefaultPageIdentity() {
            public String getDefaultPage() {
                return defaultPlanPage;
            }

            public void setDefaultPage(String planPage) {
                defaultPlanPage = planPage;
            }
        };
    }

    /**
     * HomeItem method which stops all object activity for program termination
     */
    public void stop() {
        homeServer.unregisterFinalEventListener(this);
    }

    /**
     * This is the main entrance point of the class. This is called when a http request is
     * routed to this servlet.
     */
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter p = res.getWriter();
        HomeGUIArguments arguments = new HomeGUIArguments(req);

        if (arguments.isAction("ajax")) {
            performAjax(req, res, arguments);
            return;
        }

        if (arguments.isAction("perform_action")) {
            performItemAction(req);
        }

        // Loop through all page plugins and find the appropriate one and call it
        for (HomePageInterface pagePlugin : pages) {
            if (arguments.getPage().equalsIgnoreCase(pagePlugin.getPageNameURL())) {

                // Print static start of page
                printHeader(p, pagePlugin);

                printPageData(p, arguments);

                // Print Navigation Bar
                if (allowEdit) {
                    printNavigationBar(p, pagePlugin, arguments);
                }

                // Let the plugin print the actual page
                pagePlugin.printPage(req, res, homeServer);

                // Print the end of the page and finish the loop
                printFooter(p);
                return;
            }
        }

        // We should never get here...
        p.flush();
        p.close();
    }

    private void printPageData(PrintWriter p, HomeGUIArguments arguments) {
        p.println("<script>var homeManager = {}; ");
        p.println("homeManager.baseURL=\"" + getLocalURL() + "\";");
        if (arguments.hasSubpage()) {
            p.println("homeManager.subpage=\"" + arguments.getSubpage() + "\";" +
                    "homeManager.baseURL=\"" + getLocalURL() + "\";");
        }
        p.println("</script>");
    }

    /**
     * Call an action in the specified item instance. Method extracts all
     * parameters from the Servlet Request.
     *
     * @param req The servlet request
     */
    private void performItemAction(HttpServletRequest req) {
        String name = req.getParameter("name");
        if (name != null) name = fromURL(name);

        // Open the instance and check it
        HomeItemProxy item = homeServer.openInstance(name);
        if (item == null) return;

        String action = req.getParameter("action");
        if (action != null) name = fromURL(action);
        try {
            item.callAction(name);
        } catch (ExecutionFailure e) {
            // TODO Auto-generated catch block
            // TODO Handle!
            e.printStackTrace();
        }
    }

    /**
     * Call an action in the specified item instance. Method extracts all
     * parameters from the Servlet Request.
     * Call this method as follows (for example):<br/>
     * <p>
     * <code>http://localhost:8020/home?a=ajax&name=Hyll-Lampa&action=on&attribute=5</code>
     * </p>
     * <p>
     * <code>http://localhost:8020/home?a=ajax&r=json&f=getHomeItems</code>
     * <p>
     * <code>http://localhost:8020/home?a=ajax&r=json&f=getActions&item=Hyll-Lampa</code>
     * </p>
     *
     */
    private void performAjax(HttpServletRequest req, HttpServletResponse res, HomeGUIArguments arguments) throws ServletException, IOException {
        String typeId = req.getParameter("r");
        String funcId = req.getParameter("f");
        String itemName = req.getParameter("item");
        String itemID = req.getParameter("itemid");
        PrintWriter p = res.getWriter();

        // Set standard HTTP/1.1 no-cache headers for AJAX requests. IE9 will cache first request otherwise
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        if (funcId != null && funcId.equals("eventtable")) {
            printEventsTable(p);
        } else if (funcId != null && funcId.compareToIgnoreCase("gethomeitems") == 0) {
            List<DirectoryEntry> names = homeServer.listInstances("");

            // Loop through all instances to find all home items
            p.format("{\"results\":[");
            boolean b = false;
            for (DirectoryEntry directoryEntry : names) {
                // Open the instance so we know class and category
                HomeItemProxy item = homeServer.openInstance(directoryEntry.getInstanceName());
                HomeItemModel model = item.getModel();
                List<Action> actions = model.getActions();
                if (actions.size() == 0)
                    continue;
                if (b) p.print(",");
                p.format("{\"class\":\"%s\", \"id\":\"%s\", \"name\":\"%s\"}",
                        model.getClassName(), item.getAttributeValue("ID"), item.getAttributeValue("Name"));
                b = true;
            }
            p.format("]}");
            return;
        } else if (funcId != null && funcId.compareToIgnoreCase("getactions") == 0) {
            if (itemName == null && itemID == null)
                return;
            if (itemName != null && itemName.length() == 0)
                return;
            if (itemID != null && itemID.length() == 0)
                return;
            HomeItemProxy item = homeServer.openInstance(itemID != null ? itemID : itemName);
            boolean b = false;
            List<Action> actions = (null != item) ? item.getModel().getActions() : new LinkedList<Action>();
            p.format("{\"results\":[");
            for (Action action : actions) {
                if (b) p.print(",");
                p.format("{\"name\":\"%s\"}", action.getName());
                b = true;
            }
            p.format("]}");
            return;
        } else if (funcId != null && funcId.compareToIgnoreCase("getattributes") == 0) {
            if (itemName == null && itemID == null)
                return;
            if (itemName != null && itemName.length() == 0)
                return;
            if (itemID != null && itemID.length() == 0)
                return;
            HomeItemProxy item = homeServer.openInstance(itemID != null ? itemID : itemName);
            boolean b = false;
            List<Attribute> attributes = (null != item) ? item.getAttributeValues() : Collections.<Attribute>emptyList();
            p.format("{\"results\":[");
            for (Attribute attribute : attributes) {
                if (b) p.print(",");
                p.format("{\"name\":\"%s\"}", attribute.getName());
                b = true;
            }
            p.format("]}");
            return;
        } else if (funcId != null && funcId.compareToIgnoreCase("getdefatts") == 0) {
            String itemIds = req.getParameter("items");
            if (itemIds == null) {
                return;
            }
            String[] idList = itemIds.split("-");
            p.format("{");
            boolean isFirst = true;
            for (String id : idList) {
                HomeItemProxy item = homeServer.openInstance(id);
                if (item == null) {
                    continue;
                }
                HomeItemModel model = item.getModel();
                String value = model.getDefaultAttribute() != null ? item.getAttributeValue(model.getDefaultAttribute().getName()) : "";
                if (!isFirst) {
                    p.format(",");
                }
                p.format("\"%s\":\"%s\"", item.getAttributeValue("ID"), value);
                isFirst = false;
            }
            p.format("}");
            p.flush();
            p.close();

            return;
        } else if (funcId != null && funcId.compareToIgnoreCase("reposition") == 0) {
            String itemId = req.getParameter("item");
            String planId = req.getParameter("plan");
            String x = req.getParameter("x");
            String y = req.getParameter("y");
            if (itemId == null || planId == null || x == null || y == null) {
                return;
            }
            HomeItemProxy planItem = homeServer.openInstance(planId);
            if (planItem == null || !(planItem.getInternalRepresentation() instanceof Plan)) {
                return;
            }
            Plan plan = (Plan) planItem.getInternalRepresentation();
            plan.setItemLocation(itemId, Math.round(Float.parseFloat(x)), Math.round(Float.parseFloat(y)), arguments.isIE());
            p.flush();
            p.close();
            return;
        }

        String name = req.getParameter("name");
        if (name == null) {
            return;
        }
        name = fromURL(name);

        // Open the instance and check it
        HomeItemProxy item = homeServer.openInstance(name);
        if (item == null) return;

        // Find name of action, and call it if it is found
        String action = req.getParameter("action");
        if (action != null) {
            action = fromURL(action);
            try {
                item.callAction(action);
            } catch (ExecutionFailure e) {
                // TODO Auto-generated catch block
                // TODO Handle the errors .. ??
                e.printStackTrace();
            }
            // TODO! Handle the errors .. ??
        }

        // Find name of attribute to retrieve and print value in result if found
        String attribute = req.getParameter("attribute");
        if (attribute != null) {
            attribute = fromURL(attribute);
            String value = item.getAttributeValue(attribute);
            p.println(name + "," + value);
        }
        p.flush();
        p.close();
    }

    protected void printHeader(PrintWriter p, HomePageInterface pagePlugin) throws ServletException, IOException {
        p.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        p.println("<html lang=\"en\"><head>");
        p.println("  <title>NewNetHome</title>");
        p.println("  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
        p.println("  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\"> ");
        p.println("  <link rel=\"shortcut icon\" type=\"image/ico\" href=\"web/home.ico\">");
        p.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"web/home/nethome.css\">");
        List<String> styles = pagePlugin.getCssFileNames();
        if (styles != null) {
            for (String style : styles) {
                p.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"" + style + "\">");
            }
        }
        List<String> scripts = pagePlugin.getJavaScriptFileNames();
        if (scripts != null) {
            for (String script : scripts) {
                p.println("	 <script type=\"text/javascript\" src=\"" + script + "\"></script>");
            }
        }
        String leftBannerFileName = customLeftBannerFile.length() > 0 ? customLeftBannerFile : "web/home/left_banner.png";
        String rightBannerFileName = customRightBannerFile.length() > 0 ? customRightBannerFile : "web/home/right_banner.png";
        p.println("</head>");
        p.println("<body>");
        p.println("<div id=\"pageBody\">");
        p.println("<div id=\"logobar\">");
        if (customLeftBannerFile.length() != 1 && customRightBannerFile.length() != 1) {
            p.println(" <a href=\"http://opennethome.org\"><img src=\"" + leftBannerFileName + "\" class=\"primary\" title=\"NetHomeServer\"></a>");
            p.println(" <a href=\"http://opennethome.org\"><img src=\"" + rightBannerFileName + "\" title=\"My Own Logo\" class=\"secondary\" width=\"313\" height=\"50\"></a>");
        }
        p.println(" <div class=\"floatClear\"></div>");
        p.println("</div>");
    }

    protected void printFooter(PrintWriter p) throws ServletException, IOException {
        p.println("</div>");
        p.println("</body>");
        p.println("</html>");

        p.flush();
        p.close();
    }

    protected void printNavigationBar(PrintWriter p, HomePageInterface selectedPage, HomeGUIArguments arguments) throws ServletException, IOException {
        String editClassString = "";
        if (arguments.isEditMode() && selectedPage.supportsEdit()) {
            editClassString = " edit";
        }
        p.println("<div class=\"menubarBorder\">");
        p.println(" <div class=\"menubar" + editClassString + "\">");
        p.println("  <ul>");
        p.println("<!-- main manu -->");

        if (arguments.isEditMode() && selectedPage.supportsEdit()) {
            p.println("   <li>Editing " + selectedPage.getPageName() + ":</li>");
            for (EditControl controlButton : selectedPage.getEditControls()) {
                p.println("   <li>" + controlButton.print(arguments, homeServer) + "</li>");
            }
        } else {
            // Loop through all page plugins and add their link to the nav bar
            for (HomePageInterface pagePlugin : pages) {
                if (pagePlugin.getPageName() != null) {
                    String classString = "";
                    if (selectedPage == pagePlugin) {
                        classString = " class=\"active\"";
                    }
                    p.println("   <li" + classString + "><a href=\"" + localURL + "?page=" + pagePlugin.getPageNameURL() + "\">" + pagePlugin.getPageName() + "</a></li>");
                    p.println("   <li><div class=\"menu_divider\"></div></li>");
                }
            }
        }

        p.println("<!-- preferences -->");
        String statusIcon = "";
        if (homeServer.getState().getCurrentAlarmCount() > 0) {
            statusIcon = "<img src=\"web/home/warn.png\"/>";
        }
        p.println("   <li class=\"pref\">");
        p.println(statusIcon + "&nbsp;<a href=\"" + localURL + "?page=settings&subpage=log\">Log</a>");
        p.println("   </li>");
        p.println("   <li class=\"pref\">");
        p.println("    <img src=\"web/home/info.png\"/>&nbsp;<a href=\"http://opennethome.org\">About</a>");
        p.println("   </li>");
        if (selectedPage.supportsEdit()) {
            p.println("   <li class=\"pref\">");
            String subpageArgument = arguments.hasSubpage() ? "&subpage=" + arguments.getSubpage() : "";
            if (arguments.isEditMode()) {
                p.println("     <img src=\"web/home/edit.png\"/>&nbsp;<a href=\"" + localURL + "?page=" + selectedPage.getPageNameURL() + subpageArgument + "\">End edit</a>");
            } else {
                p.println("     <img src=\"web/home/edit.png\"/>&nbsp;<a href=\"" + localURL + "?page=" + selectedPage.getPageNameURL() + subpageArgument + "&mode=edit\">Edit this page</a>");
            }
            p.println("   </li>");
        }
        p.println("  </ul>");
        p.println(" </div>");
        p.println(" <div class=\"menubarLeft" + editClassString + "\"></div>");
        p.println(" <div class=\"menubarRight" + editClassString + "\"></div>");
        p.println(" <div class=\"floatClear\"></div>");
        p.println("</div>");
    }


    public static String toURL(String aText) {
        String result;

        try {
            result = URLEncoder.encode(aText, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    public static String fromURL(String aURLFragment) {
        String result;
        try {
            result = URLDecoder.decode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    public static String itemIcon(String itemType, boolean small) {
        if (itemType.equals("Lamps")) {
            return small ? "lamp16_off.png" : "lamp_off.png";
        }
        if (itemType.equals("Timers")) {
            return small ? "clock16.png" : "timer.png";
        }
        if (itemType.equals("Ports")) {
            return small ? "port16.png" : "port.png";
        }
        if (itemType.equals("GUI")) {
            return small ? "gui16.png" : "gui.png";
        }
        if (itemType.equals("Hardware")) {
            return small ? "hw16.png" : "hw.png";
        }
        if (itemType.equals("Controls")) {
            return small ? "control16.png" : "control.png";
        }
        if (itemType.equals("Gauges")) {
            return small ? "gauge16.png" : "gauge.png";
        }
        if (itemType.equals("Thermometers")) {
            return small ? "thermometer16.png" : "thermometer.png";
        }
        if (itemType.equals("Infrastructure")) {
            return small ? "infra16.png" : "infra.png";
        }
        if (itemType.equals("Actuators")) {
            return small ? "coggwheels16.png" : "coggwheels.png";
        }
        return small ? "item16.png" : "item.png";
    }

    /**
     * @return Returns the m_WEBServer.
     */
    public String getWEBServer() {
        return webServer;
    }

    /**
     * @param WEBServer The m_WEBServer to set.
     */
    public void setWEBServer(String WEBServer) {
        webServer = WEBServer;
    }

    /**
     * @return Returns the m_LocalURL.
     */
    public String getLocalURL() {
        return localURL;
    }

    /**
     * @param LocalURL The m_LocalURL to set.
     */
    public void setLocalURL(String LocalURL) {
        localURL = LocalURL;
    }

    public String getCustomLeftBannerFile() {
        return customLeftBannerFile;
    }

    public void setCustomLeftBannerFile(String customLeftBannerFile) {
        this.customLeftBannerFile = customLeftBannerFile;
    }

    public String getCustomRightBannerFile() {
        return customRightBannerFile;
    }

    public void setCustomRightBannerFile(String customRightBannerFile) {
        this.customRightBannerFile = customRightBannerFile;
    }

    public String getPlanPage() {
        return defaultPlanPage;
    }

    public void setPlanPage(String planPage) {
        this.defaultPlanPage = planPage;
    }

    public String getDefaultLocation() {
        return defaultLocation;
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public String getAllowEdit() {
        return allowEdit ? "Yes" : "No";
    }

    public void setAllowEdit(String allowEdit) {
        this.allowEdit = allowEdit.equalsIgnoreCase("Yes") || allowEdit.equalsIgnoreCase("True");
    }

    @Override
    public void receiveFinalEvent(Event event, boolean isHandled) {
        creationEvents.newEvent(event, isHandled);
    }

    private void printEventsTable(PrintWriter p) {
        p.println(" <table>");
        p.println("  <tr><th></th><th>Identity</th><th>Create</th><th>Time since</th><th>Item Exists</th></tr>");
        for (ItemEvent itemEvent : creationEvents.getItemEvents()) {
            printEventRow(p, itemEvent);
        }
        p.println(" </table>");
        if (System.currentTimeMillis() - lastItemReportTime > REPORT_ITEMS_PERIOD_MS) {
            Event reportEvent = homeServer.createEvent("ReportItems", "");
            homeServer.send(reportEvent);
            lastItemReportTime = System.currentTimeMillis();
        }
    }

    private void printEventRow(PrintWriter p, ItemEvent event) {
        long age = (System.currentTimeMillis() - event.getReceived().getTime()) / MS_PER_SECOND;
        long ageMinutes = age / SECONDS_PER_MINUTE;
        long ageSeconds = age % SECONDS_PER_MINUTE;
        StringBuilder ageString = new StringBuilder();
        if (ageMinutes > 0) {
            ageString.append(ageMinutes).append(" Min ");
        }
        ageString.append(ageSeconds).append(" Sec");
        p.println("  <tr>");
        p.printf("   <td><img src=\"web/home/%s\" /></td>\n", (event.getWasHandled() ? "item16.png" : "item_new16.png"));
        if (event.getContent().length() < MAX_EVENT_ID_LENGTH) {
            p.printf("   <td>%s</td>\n", event.getContent());
        } else {
            p.printf("   <td title=\"%s\">%s...</td>\n", event.getContent(), event.getContent().substring(0, MAX_EVENT_ID_LENGTH));
        }
        p.printf("   <td><a onclick=\"location.href=homeManager.classUrl + '&event=%d';return false;\" href=\"%s?page=edit&event=%d\">Create Item</a></td>\n", event.getId(), localURL, event.getId());
        p.printf("   <td>%s</td>\n", ageString);
        p.printf("   <td>%s</td>\n", (event.getWasHandled() ? "Existing" : "New"));
        p.println("  </tr>");
    }
}
