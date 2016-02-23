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
import nu.nethome.home.system.DirectoryEntry;
import nu.nethome.home.system.HomeService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PlanPage implements HomePageInterface {

    protected String localURL;
    private DefaultPageIdentity defaultPlanIdentity;
    private String mediaDirectory;

    public PlanPage(String localURL, DefaultPageIdentity defaultPlanIdentity, String mediaDirectory) {
        this.localURL = localURL;
        this.defaultPlanIdentity = defaultPlanIdentity;
        this.mediaDirectory = mediaDirectory;
    }

    /* (non-Javadoc)
      * @see nu.nethome.home.items.web.servergui.HomePageInterface#getCssFileName()
      */
    public List<String> getCssFileNames() {
        List<String> styles = new ArrayList<>();
        styles.add("web/home/plan.css");
        return styles;
    }

    /* (non-Javadoc)
      * @see nu.nethome.home.items.web.servergui.HomePageInterface#getJavaScriptFileName()
      */
    public List<String> getJavaScriptFileNames() {
        List<String> scripts = new ArrayList<>();
        scripts.add("web/home/js/jquery-1.4.3.min.js");
        scripts.add("web/home/js/jquery-ui-1.8.24.custom.min.js");
        scripts.add("web/home/newplan.js");
        return scripts;
    }

    /* (non-Javadoc)
      * @see nu.nethome.home.items.web.servergui.HomePageInterface#getPageNameURL()
      */
    public String getPageNameURL() {
        return "plan";
    }

    /* (non-Javadoc)
      * @see nu.nethome.home.items.web.servergui.HomePageInterface#getPageName()
      */
    public String getPageName() {
        return "Plan";
    }

    public boolean supportsEdit() {
        return true;
    }

    public List<EditControl> getEditControls() {
        return Arrays.asList(
                new AddItemEditControl(),
                new RemoveItemEditControl(),
                new BackgroundEditControl(),
                new ClickActionEditControl());
    }

    /* (non-Javadoc)
    * @see nu.nethome.home.items.web.servergui.HomePageInterface#printPage(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, nu.nethome.home.HomeService)
    */
    public void printPage(HttpServletRequest req, HttpServletResponse res, HomeService server) throws ServletException, IOException {
        PrintWriter p = res.getWriter();
        HomeGUIArguments arguments = new HomeGUIArguments(req);
        Plan viewedPlan = findPlan(server, arguments, defaultPlanIdentity);
        if (arguments.isAction("add")) {
            viewedPlan.addItem(arguments.getName());
        } else if (arguments.isAction("remove")) {
            viewedPlan.removeItem(arguments.getName());
        } else if (arguments.isAction("background")) {
            viewedPlan.setImageFile(arguments.getName());
        } else if (arguments.isAction("click")) {
            viewedPlan.setClickAction(arguments.getName());
        }
        printPlanUpdateScript(p, viewedPlan, arguments.isEditMode());
        printPlanPageStart(p, viewedPlan);
        printPlanItems(server, p, viewedPlan, arguments);
        printPlanPageEnd(p);
        if (arguments.isEditMode()) {
            printEditInfo(p);
        }
    }

    private void printEditInfo(PrintWriter p) {
        p.println("<div class=\"draggable ui-draggable\" style=\"top:170px;left:41px;\">\n" +
                "<img src=\"web/home/info16.png\" /><span class=\"text\">&nbsp;Drag and drop to move Items on the page" +
                "</span></div>");
        p.println("<div class=\"draggable ui-draggable\" style=\"top:250px;left:41px;\">\n" +
                "<img src=\"web/home/info16.png\" /><span class=\"text\">&nbsp;To add a new background image, go to <a href=\"" +
                localURL + "?page=settings&subpage=media" + "\">Settings->Media</a>" +
                "</span></div>");
    }

    private void printPlanUpdateScript(PrintWriter p, Plan viewedPlan, boolean editMode) {
        int updateInterval = viewedPlan.getUpdateIntervalInt() * 1000;
        if (!editMode && (updateInterval > 0)) {
            p.println("<script>window.setInterval(getItemValues," + updateInterval + ");</script>");
        }
    }

    private void printPlanPageStart(PrintWriter p, Plan viewedPlan) {
        p.println("<span id=\"dummy\"></span>");
        p.println("<div class=\"plan\" data-item=\"" + viewedPlan.getItemId() + "\" style=\"background:url(" + viewedPlan.getImageFile() + ") no-repeat;\">");
    }

    private void printPlanPageEnd(PrintWriter p) {
        p.println("</div>");
    }

    private void printPlanItems(HomeService server, PrintWriter p, Plan viewedPlan, HomeGUIArguments arguments) throws ServletException, IOException {
        for (Plan.PlanItem planItem : viewedPlan.getPlanItems()) {
            // Open the instance
            HomeItemProxy item = server.openInstance(planItem.getItemId());
            if (item != null) {
                printPlanHomeItem(p, viewedPlan, item, planItem, arguments);
            }
        }
    }

    private Plan findPlan(HomeService server, HomeGUIArguments arguments, DefaultPageIdentity defaultPlanIdentity) {
        HomeItemProxy foundPlanItem = null;
        if (arguments.hasSubpage()) {
            foundPlanItem = server.openInstance(arguments.getSubpage());
        }
        if (foundPlanItem == null) {
            foundPlanItem = server.openInstance(defaultPlanIdentity.getDefaultPage());
        }
        if (foundPlanItem == null) {
            foundPlanItem = findAnyPlanItem(server);
            if (foundPlanItem != null) {
                defaultPlanIdentity.setDefaultPage(foundPlanItem.getAttributeValue("ID"));
            }
        }
        if (foundPlanItem == null) {
            foundPlanItem = server.createInstance("Plan", "CreatedPlan");
            try {
                foundPlanItem.callAction("activate");
            } catch (ExecutionFailure executionFailure) {
                // Should not fail...
            }
            defaultPlanIdentity.setDefaultPage(foundPlanItem.getAttributeValue("ID"));
        }
        return (Plan) foundPlanItem.getInternalRepresentation();
    }

    private HomeItemProxy findAnyPlanItem(HomeService server) {
        List<DirectoryEntry> names = server.listInstances("");
        for (DirectoryEntry directoryEntry : names) {
            // Open the instance so we know class and category
            HomeItemProxy planItem = server.openInstance(directoryEntry.getInstanceName());
            HomeItemModel model = planItem.getModel();
            // Check if it is a Plan-item
            if (model.getClassName().equals("Plan")) {
                return planItem;
            }
        }
        return null;
    }

    /**
     * Prints a HomeItem instance to the output stream.
     *
     * @param p          Output stream
     * @param viewedPlan Current Plan
     * @param item       HomeItem to print
     * @param arguments  @throws ServletException
     * @throws java.io.IOException
     */
    private void printPlanHomeItem(PrintWriter p,
                                   Plan viewedPlan, HomeItemProxy item, Plan.PlanItem planItem, HomeGUIArguments arguments) throws ServletException, IOException {

        HomeItemModel model = item.getModel();
        String category = model.getCategory();

        if (model.getClassName().equals("Plan") && !arguments.isEditMode()) {
            printPlanHomeItemLink(p, item, planItem);
            return;
        }
        if (model.getClassName().equals("ActionButton") && !arguments.isEditMode()) {
            printActionButton(p, item, planItem);
            return;
        }

        String arrowIconAttributes = "";
        String arrowIconImageClass = arrowIcon(category);
        if (category.equals("Lamps")) {
            if (item.getAttributeValue("State").equals("On")) {
                arrowIconImageClass = "lamp_on";
            } else {
                arrowIconImageClass = "lamp_off";
            }
            arrowIconAttributes = "data-item=\"" + item.getAttributeValue("ID") + "\" data-On=\"lamp_on\" data-Off=\"lamp_off\" data-lastclass=\"" + arrowIconImageClass + "\"";
        }
        String locationClass = "icon " + arrowIconImageClass;
        String itemName = item.getAttributeValue("Name");
        String itemId = item.getAttributeValue("ID");
        String mainAttribute = HomeGUI.toURL(model.getDefaultAttribute() != null ? model.getDefaultAttribute().getName() : "");
        String mainAttributeValue = item.getAttributeValue(mainAttribute);
        if (mainAttributeValue.length() > 0 && model.getDefaultAttribute().getUnit().length() > 0) {
            mainAttributeValue += " " + model.getDefaultAttribute().getUnit();
        }
        String itemText = arguments.isEditMode() ? itemName : mainAttributeValue;

        String iconClass;
        String title;
        if (arguments.isEditMode()) {
            iconClass = "draggable";
            title = "Drag and drop to move Item";
        } else if (!viewedPlan.isPopupOnClick() && model.getDefaultAction().length() > 0) {
            iconClass = "clickable";
            title = item.getAttributeValue("Name") + "\n<Click to " + model.getDefaultAction() + ">";
        } else {
            iconClass = "poppable";
            title = item.getAttributeValue("Name") + "\n<Click for details>";
        }

        p.println("<div class=\"" + iconClass + "\" data-item=\"" + itemId + "\" title=\"" + title + "\" data-plan=\"" + viewedPlan.getItemId() +
                "\" style=\"top:" +
                Integer.toString(planItem.getY(arguments.isIE())) + "px;left:" +
                Integer.toString(planItem.getX(arguments.isIE())) + "px;\">");
        if (iconClass.equals("poppable") || iconClass.equals("draggable")) {
            p.println("    <ul class=\"itemlocation\">");
        } else {
            p.println("    <ul class=\"itemlocation\" onclick=\"callItemAction('" + item.getAttributeValue("ID") + "', '" + model.getDefaultAction() + "');\" href=\"javascript:void(0)\">");
        }
        p.println("        <li class=\"" + locationClass + "\" " + arrowIconAttributes + "/>");
        p.println("        <li class=\"itemvalue\"  " + getUnitAttribute(model) + " data-item=\"" + itemId + "\">" + itemText + "</li>");
        p.println("    </ul>");
        p.println("</div>");
        if (!arguments.isEditMode() && iconClass.equals("poppable")) {
            printItemPopup(p, item, planItem, arguments, model);
        }
    }

    private void printItemPopup(PrintWriter p, HomeItemProxy item, Plan.PlanItem planItem, HomeGUIArguments arguments, HomeItemModel model) {
        String itemName = item.getAttributeValue("Name");
        String itemId = item.getAttributeValue("ID");
        String category = model.getCategory();
        String popupIconImageFileName = "web/home/" + HomeGUI.itemIcon(category, false);

        List<Action> actions = model.getActions();
        // Make an estimate of how many rows of action buttons there will be
        int size = 0;
        for (Action action : actions) {
            size += action.getName().length();
            size += 2;
        }
        int actionRowCount = size / 50 + 1;

        // Adjust the height of the panel to the number of rows of action buttons
        String noActionRows = "";
        if ((actionRowCount > 1) && (actionRowCount < 7)) {
            noActionRows = " row" + Integer.toString(actionRowCount);
        } else if (actionRowCount > 6) {
            noActionRows = " row9";
        }

        p.println("<div class=\"phomeitem" + noActionRows + "\" data-item=\"" + itemId + "\" style=\"top:" +
                Integer.toString(planItem.getY(false)) + "px;left:" +
                Integer.toString(planItem.getX(false)) + "px;\">");
        p.println(" <ul>");
        p.println("  <li class=\"close\"><img class=\"closebutton\" src=\"web/home/close.png\" /></li>");
        p.println("  <li><img src=\"" + popupIconImageFileName + "\" /></li>");
        p.println("  <li><img class=\"dividerimg\" src=\"web/home/pitem_divider.png\" /></li>");
        p.println("  <li>");
        p.println("   <ul>");
        p.println("    <li><a href=\"" + localURL + "?page=edit&name=" + itemId + "&return=" + this.getPageNameURL() +
                this.subPageArg(arguments) + "\">" + itemName + ": </a><span class=\"itemvalue\" " + getUnitAttribute(model) + " data-item=\"" + itemId + "\"></span></li>");
        p.println("    <li><ul>");
        if (hasLogFile(item)) {
            p.println("		<li><a href=\"" + localURL + "?page=graphs&subpage=" +
                    item.getAttributeValue(HomeItemProxy.ID_ATTRIBUTE) + "\">View graph...</a></li>");
        }
        for (Action action : actions) {
            p.println("     <li><a href=\"javascript:void(0)\" onclick=\"callItemAction('" + itemId + "','" + HomeGUI.toURL(action.getName()) + "');\">" + action.getName() + "</a></li>");
        }
        p.println("	&nbsp;");
        p.println("	</ul></li>");
        p.println("   </ul>");
        p.println("  </li>");
        p.println(" </ul>");
        p.println("</div>");
    }

    private String getUnitAttribute(HomeItemModel model) {
        AttributeModel att = model.getDefaultAttribute();
        if (att != null && att.getUnit() != null && att.getUnit().length() > 0) {
            return " data-unit=\"" + att.getUnit() + "\" ";
        }
        return "";
    }

    private boolean hasLogFile(HomeItemProxy item) {
        return item.getAttributeValue("LogFile").length() > 0;
    }

    private String subPageArg(HomeGUIArguments arguments) {
        if (arguments.hasSubpage()) {
            return "&returnsp=" + arguments.getSubpage();
        }
        return "";
    }

    private void printPlanHomeItemLink(PrintWriter p,
                                       HomeItemProxy item, Plan.PlanItem planItem) throws ServletException, IOException {

        String title = "Click to follow link to the " + item.getAttributeValue("Name") + " page";
        p.println("<div class=\"planlink\" title=\"" + title + "\" style=\"top:" +
                Integer.toString(planItem.getY(false)) + "px;left:" +
                Integer.toString(planItem.getX(false)) + "px;\">");
        p.println("    <ul class=\"itemlocation\" onclick=\"location.href='" + localURL + "?page=" + getPageNameURL() + "&subpage=" + item.getAttributeValue("ID") + "';\" >");
        p.println("        <li class=\"icon link\" />");
        p.println("        <li>" + item.getAttributeValue("Name") + "</li>");
        p.println("    </ul>");
        p.println("</div>");
    }

    private void printActionButton(PrintWriter p,
                                   HomeItemProxy item, Plan.PlanItem planItem) throws ServletException, IOException {

        String title = item.getAttributeValue("Title");
        p.println("<div class=\"actionbutton\" title=\"" + title + "\" style=\"top:" +
                Integer.toString(planItem.getY(false)) + "px;left:" +
                Integer.toString(planItem.getX(false)) + "px;\">");
        String playSound = "";
        if (item.getAttributeValue("ClickSound").length() > 0) {
            playSound = "playSound('" + item.getAttributeValue("ClickSound") + "');";
        }
        p.println("    <ul class=\"itemlocation\" onclick=\"" + playSound + "callItemAction('" + item.getAttributeValue("ID") + "', 'pushAction');\" href=\"javascript:void(0)\">");
        String mouseDown = "";
        if (item.getAttributeValue("ClickIcon").length() > 0) {
            mouseDown = " onmousedown=\"this.src='" + item.getAttributeValue("ClickIcon") +
                    "'\" onmouseup=\"this.src='" + item.getAttributeValue("Icon") + "'\"";
        }
        p.println("        <li class=\"icon custom\"><img src=\"" + item.getAttributeValue("Icon") + "\" " + mouseDown + "></li>");
        p.println("        <li>" + item.getAttributeValue("Text") + "</li>");
        p.println("    </ul>");
        p.println("</div>");
    }

    private static String arrowIcon(String itemType) {
        if (itemType.equals("Lamps")) {
            return "lamp_off";
        }
        if (itemType.equals("Timers")) {
            return "timer";
        }
        if (itemType.equals("Ports")) {
            return "port";
        }
        if (itemType.equals("GUI")) {
            return "gui";
        }
        if (itemType.equals("Hardware")) {
            return "hw";
        }
        if (itemType.equals("Controls")) {
            return "control";
        }
        if (itemType.equals("Gauges")) {
            return "gauge";
        }
        if (itemType.equals("Thermometers")) {
            return "temp";
        }
        if (itemType.equals("Infrastructure")) {
            return "house";
        }
        if (itemType.equals("Actuators")) {
            return "actuator";
        }
        return "item.png";
    }

    class AddItemEditControl implements EditControl {
        @Override
        public String print(HomeGUIArguments arguments, HomeService server) {
            String subpageArgument = arguments.hasSubpage() ? "&subpage=" + arguments.getSubpage() : "";
            String result = "";
            result += "<form action=\"" + localURL + "?page=" + getPageNameURL() + subpageArgument + "&mode=edit\" method=\"POST\">";
            result += "<input type=\"hidden\" name=\"a\" value=\"add\">";
            result += "  <select   onchange=\"this.form.submit()\" name=\"name\">";
            result += "  <option value=\"\">Add Item to plan</option>";
            Map<String, CategorizedItemList> categories = CategorizedItemList.categorizeItems(server);
            for (String category : HomeItemModel.HOME_ITEM_CATEGORIES) {
                if (categories.containsKey(category)) {
                    CategorizedItemList itemsInCategory = categories.get(category);
                    result += "  <optgroup label=\"" + category + "\">";
                    for (HomeItemProxy item : itemsInCategory.getItems()) {
                        result += "  <option value=\""
                                + item.getAttributeValue("ID")
                                + "\""
                                + ">" + item.getAttributeValue(HomeItemProxy.NAME_ATTRIBUTE)
                                + "</option>";
                    }
                    result += "  </optgroup>";
                }
            }
            result += "  </select>";
            result += "</form>";
            return result;
        }
    }

    class RemoveItemEditControl implements EditControl {
        @Override
        public String print(HomeGUIArguments arguments, HomeService server) {
            String subpageArgument = arguments.hasSubpage() ? "&subpage=" + arguments.getSubpage() : "";
            String result = "";
            result += "<form action=\"" + localURL + "?page=" + getPageNameURL() + subpageArgument + "&mode=edit\" method=\"POST\">";
            result += "<input type=\"hidden\" name=\"a\" value=\"remove\">";
            result += "  <select   onchange=\"this.form.submit()\" name=\"name\">";
            result += "  <option value=\"\">Remove Item from plan</option>";
            Plan viewedPlan = findPlan(server, arguments, defaultPlanIdentity);
            String[] itemIds = viewedPlan.getItems().split(",");
            for (String itemId : itemIds) {
                HomeItemProxy item = server.openInstance(itemId);
                if (item != null) {
                    result += "  <option value=\""
                            + item.getAttributeValue("ID")
                            + "\""
                            + ">" + item.getAttributeValue(HomeItemProxy.NAME_ATTRIBUTE)
                            + "</option>";
                }
            }
            result += "  </select>";
            result += "</form>";
            return result;
        }
    }

    class BackgroundEditControl implements EditControl {
        @Override
        public String print(HomeGUIArguments arguments, HomeService server) {
            String subpageArgument = arguments.hasSubpage() ? "&subpage=" + arguments.getSubpage() : "";
            String result = "";
            result += "<form action=\"" + localURL + "?page=" + getPageNameURL() + subpageArgument + "&mode=edit\" method=\"POST\">";
            result += "<input type=\"hidden\" name=\"a\" value=\"background\">";
            result += "  <select   onchange=\"this.form.submit()\" name=\"name\">";
            result += "  <option value=\"\">Select background</option>";
            File f = new File(mediaDirectory);
            if (f.exists() && f.isDirectory()) {
                ArrayList<String> names = new ArrayList<>(Arrays.asList(f.list()));
                for (String fileName : names) {
                    result += "  <option value=\""
                            + "media/" + fileName
                            + "\""
                            + ">" + fileName
                            + "</option>";
                }
            }
            result += "  </select>";
            result += "</form>";
            return result;
        }
    }

    class ClickActionEditControl implements EditControl {
        @Override
        public String print(HomeGUIArguments arguments, HomeService server) {
            String subpageArgument = arguments.hasSubpage() ? "&subpage=" + arguments.getSubpage() : "";
            String result = "";
            result += "<form action=\"" + localURL + "?page=" + getPageNameURL() + subpageArgument + "&mode=edit\" method=\"POST\">";
            result += "<input type=\"hidden\" name=\"a\" value=\"click\">";
            result += "  <select   onchange=\"this.form.submit()\" name=\"name\">";
            result += "  <option value=\"\">Action on click</option>";
            result += "  <option value=\"DefaultAction\">" + "Default Action</option>";
            result += "  <option value=\"Popup\">" + "Popup</option>";
            result += "  </select>";
            result += "</form>";
            return result;
        }
    }
}
