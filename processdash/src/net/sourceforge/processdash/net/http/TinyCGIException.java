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


package net.sourceforge.processdash.net.http;

import java.io.IOException;

public class TinyCGIException extends IOException {

    private int status;
    private String title, text, otherHeaders;

    public TinyCGIException(int status, String title) {
        this(status, title, null, null);
    }

    public TinyCGIException(int status, String title, String text) {
        this(status, title, text, null);
    }

    public TinyCGIException(int status, String title, String text,
                            String otherHeaders) {
        super(title);
        this.status = status;
        this.title = title;
        this.text = text;
        this.otherHeaders = otherHeaders;
    }

    public int getStatus() { return status; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getOtherHeaders() { return otherHeaders; }

}
