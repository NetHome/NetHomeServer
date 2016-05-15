/*
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome  (http://www.nethome.nu)
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


$(document).ready(function () {
    refreshTable();
});

function refreshTable(){
    $('#includeState').load(homeManager.baseURL + "?a=ajax&f=addnodestate");
    $('#eventsTableHolder').load(homeManager.baseURL + "?a=ajax&f=eventtable", function(){
        setTimeout(refreshTable, 1000);
    });
}

function startInclude() {
    $.post(homeManager.baseURL + "?a=ajax&f=startinclude");
}

function endInclude() {
    $.post(homeManager.baseURL + "?a=ajax&f=endinclude");
}

function startExclude() {
    $.post(homeManager.baseURL + "?a=ajax&f=startexclude");
}

function endExclude() {
    $.post(homeManager.baseURL + "?a=ajax&f=endexclude");
}
