// -*- mode: c++ -*-
/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
// 
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// 
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
// 
// E-Mail POC:  processdash-devel@lists.sourceforge.net
****************************************************************************/

/* 
 * This useful function prints out a small version of a chart, hyperlinked
 * with an appropriate URL to draw a full screen version of the same chart.
 */
function write(query, small) {
    var chart = "line.class";
    if (query.indexOf("chart=") != -1) {
        if      (query.indexOf("chart=xy")  != -1) chart = "xy.class";
        else if (query.indexOf("chart=pie") != -1) chart = "pie.class";
        else if (query.indexOf("chart=bar") != -1) chart = "bar.class";
        else if (query.indexOf("chart=radar") != -1) chart = "radar.class";
    }
    document.writeln("<A HREF='../full.htm?" + query + "'>");
    document.writeln("<img src='../"+chart+"?"+query+"&"+small+"'></a>");
}
