// -*- mode: c++ -*- <!--#server-parsed--><!--#resources file="data-js" -->
// <!--#echo defaultEncoding="html,javaStr" -->
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


				// if this (no-op) statement is removed, a
				// bug in Netscape causes this entire script
document.write(" ");		// to be overlooked...



/***********************************************************************
 ***                      GLOBAL VARIABLES                           ***
 ***********************************************************************/

/*
 * debug
 *
 * setting this variable to true will cause debugging output to be inserted
 * in your HTML file, and will cause ongoing output to be sent to stderr as
 * events occur.
 */
var debug = false;

if (debug) { document.write("running data.js<P>"); }

/*
 * A tag that must be present in the data for this form to display this data.
 * This value is set by the presence of an <INPUT> element (probably of
 * TYPE=HIDDEN) with the NAME "requiredTag".  That element's VALUE property
 * will determine the value of this variable.
 */
var requiredTag = "";

var SILENT;

var ieVersion = 0;
var nsVersion = 0;

var AppletName = "DataApplet161";   // VERSION



/***********************************************************************
 ***             UTILITY ROUTINES (used by both browsers)            ***
 ***********************************************************************/

/*
 * The next block of code scans the document for form elements, and calls
 * a given function on each one.  The parameter must be a Function object
 * that contains a member called func which points to a function taking one
 * argument. Here is how to call someArbitraryFunction on all the elements
 * in the page:
 *
 *      function arbitraryObject() { this.func = someArbitraryFunction; }
 *      elementIterate(new arbitraryObject());
 *
 * This level of indirection is necessary because Netscape JavaScript does not
 * support passing functions as parameters.  Both Netscape and IE do support
 * passing objects as parameters, and objects can contain member functions.
 */

function elementIterate(funcObj) {
  var numForms, formNum, form, numElements, elementNum, elem;
  numForms = document.forms.length;
  for (formNum = 0; formNum < numForms; formNum++) {
    form = document.forms[formNum];
    numElements = document.forms[formNum].elements.length;
    for (elemNum = 0; elemNum < numElements; elemNum++) {
      funcObj.func(document.forms[formNum].elements[elemNum]);
    } // for elemNum...
  } // for formNum...
}

/*
 * escape backslashes and tabs.
 */

function escStr(s) { return s.replace(/\\/g,"\\\\").replace(/\t/g, "\\t"); }

/*
 * escape HTML entities.
 */

function textToHTML(text) {
    return text.replace(/&/, "&amp;").replace(/</, "&lt;")
        .replace(/>/, "&gt;").replace(/"/, "&quot;"); //")
}


/*
 * Should read-only data be unlocked?
 */

var unlocked = (window.location.search.indexOf("unlock") != -1);

var unlockURL;
if (unlocked) {
  unlockURL = window.location.href.replace(/unlock/, "")
              .replace(/([?&])&/, "$1").replace(/[?&]$/, "");
  unlockHTML =
    '<br><A HREF="javascript:gotoUnLockURL();"><!--#echo Lock_Message --></A>';
} else {
  if (window.location.search == "") {
    unlockURL = window.location.href + "?unlock";
  } else {
    unlockURL = window.location.href + "&unlock";
  }
  unlockHTML = 
    '<br><A HREF="javascript:displayUnlockWarning();"><!--#echo Unlock_Message --></A>';
}


/*
 * Functions used for unlocking
 */

function displayUnlockWarning() {
if (window.confirm("<!--#echo var="Unlock_Warning" encoding="javaStr" -->"))
  displayDefaultMessage();
}

function displayDefaultMessage() {
  window.alert("<!--#echo var="DEFAULT_Message" encoding="javaStr" -->");
  gotoUnLockURL();
}
function gotoUnLockURL() {
  window.location.replace(unlockURL);
}

/*
 * Functions used for exporting
 */

function eesc(str) {
    str = escape(str);
    str = str.replace(/\//g, "%2F");
    str = str.replace(/\./g, "%2E");
    str = str.replace(/\+/g, "%2B");
    return str;
}

function writeExportHTML() {
    document.writeln("&nbsp; &nbsp; &nbsp; &nbsp;<!--#echo Export_To --> ");
    document.writeln("<A HREF='/reports/form2html.class'><!--#echo Export_To_HTML --></A>");
    var url = eesc(window.location.pathname +
		   window.location.hash +
		   window.location.search);
    url = "/reports/form2html.class?uri=" + url;
    url = eesc(url);
	
    document.writeln("<A HREF='/reports/excel.iqy?uri=" +url+ 
		     "&fullPage'><!--#echo Export_To_Excel --></A>");
}

function writeHelpLink() {
  document.writeln("&nbsp; &nbsp; &nbsp; &nbsp;<A HREF='/help/Topics/Planning/EnteringData.html' TARGET='_blank'><I><!--#echo Help_Dots --></I></A>");
}

function writeFooter() {
    if (!SILENT) {
	document.write('<span class=doNotPrint>');
	document.write(unlockHTML);
	writeExportHTML();
	writeHelpLink();
	document.write('</span>');
    }
}



/***********************************************************************
 ***                  Internet Explorer definitions                  ***
 ***********************************************************************/

/*
 * global variables used by IE functions
 */
var IEparameterString = "";     // a list of parameters for the IEDataApplet.
var IEfieldNum = 0;             // start numbering elements with 0.

/*
 * determine if the given element is read only, and reconfigure its
 * appearance and properties appropriately. This routine should be only 
 * be called AFTER the page has finished loading and the IEDataApplet
 * has been created.
 */

function IEsetupReadOnly(elem) {
  if (elem.dataFld != null && IEDataAppl.readyState > 0) {
    if (! IEDataAppl.isEditable(elem.dataFld)) {
      elem.readOnly = true;
      elem.style.backgroundColor = IEDataAppl.readOnlyColor();
      elem.tabIndex = -1;
    } else {
      elem.readOnly = false;
      elem.style.backgroundColor = "";
      elem.tabIndex = "";
    }
  }
  //elem.disabled = false;
}

                                // call setupReadOnly on the "this" element.
function IEcheckEditable() { IEsetupReadOnly(this); }

    // call setupReadOnly on the element referenced by the current event.
function IEresetReadOnly() {
    IEsetupReadOnly(ieFields[parseInt(event.dataFld.substring(5, 99))]);
}
                                // call setupReadOnly on all form elements.
function IEsetupReadOnlyObj() { this.func = IEsetupReadOnly; }
function IEscanForReadOnly(event) { elementIterate(new IEsetupReadOnlyObj()); }



/*
 * if the current element is a "select-one" element, make sure it all its
 * <OPTION> elements have their "value" property set (since IE databinding
 * binds select elements based on option-values).  If any <OPTION> is missing
 * a value, give it a value equal to its "text" property.
 */

function IEsetupSelectValues(elem) {
  if (elem.type.toLowerCase() != "select-one") return;

  var numOptions, optionNum;
  numOptions = elem.options.length;
  for (optionNum = 0;   optionNum < numOptions;  optionNum++)
    if (elem.options(optionNum).value == "")
      elem.options(optionNum).value = elem.options(optionNum).text;
}


/*
 * Examine a form element during page startup.  Save information about the
 * element for IEDataApplet use. Initially make the element disabled, so the
 * user cannot enter the element or type into it until the applet is ready.
 */
var ieFields = new Array(10);

function IEregisterElement(elem) {
                         // only setup this element if it has a NAME property.
  if (elem.name != null && elem.name != "") {

                                                  // if this is the requiredTag
    if (elem.name.toLowerCase() == "requiredtag") // <INPUT> element, save the
      requiredTag = elem.value;                   // requiredTag value.

    else if (elem.name &&
	     elem.name.indexOf("NOT_DATA") == -1 &&
             elem.dataSrc == "") {         // if elem isn't already bound,
      elem.dataSrc = "#IEDataAppl";        // bind it to the IEDataApplet
      elem.dataFld = "field" + IEfieldNum; // with a new, unique dataFld value,
      ieFields[IEfieldNum] = elem;         // and add info about the element to
				           // the IEparameterString.
      IEparameterString = IEparameterString + 
	'<param name=field'+ IEfieldNum +
              ' value="'+ textToHTML(escStr(elem.name)) +'">'+
	'<param name=type' + IEfieldNum +' value="'+ elem.type         +'">';
      IEfieldNum++;
    }
    IEsetupSelectValues(elem);	// ensure <SELECT> elements are setup.

				// disable the element until the IEDataApplet
    //elem.disabled = true;	// is ready for the user to interact with it.

        // IEcheckEditable cannot be called until the IEDataApplet is
        // created.  We are safe setting up this handler, however, since the
        // element is disabled and thus cannot receive the focus until the
        // IEDataApplet is ready.
    elem.onfocus = IEcheckEditable;
    // elem.onblur = IEscanForReadOnly;
  }
}
				// function object for use by elementIterate
function IEregisterElementObj() { this.func = IEregisterElement; }



/*
 * Internet Explorer top-level setup procedure.
 */

function IEsetup() {
				// scan the document for form elements and
				// perform setup for each.
  elementIterate(new IEregisterElementObj());

				// if any elements were found,
  if (IEparameterString != "") {

				// add a data applet to the page.
    document.writeln('<applet id=IEDataAppl'+
		            ' archive="/help/Topics/Troubleshooting/DataApplet/SunPlugin.jar" ' +
		            ' code=pspdash.data.IEDataApplet'+
		            ' width=1 height=1>');
    document.writeln('<param name="cabbase" value="/'+AppletName+'.cab">');
    document.writeln(IEparameterString);
    if (requiredTag != "")
      document.writeln('<param name=requiredTag value="' + requiredTag +'">');
    if (unlocked)
      document.writeln('<param name=unlock value=true>');
    document.writeln('<param name=docURL value="'+window.location.href+'">');
    document.writeln('<param name=ieVersion value="'+ieVersion+'">');
    document.writeln('<param name=nsVersion value="'+nsVersion+'">');
    document.writeln('<param name=debug value="<!--#echo dataApplet.debug -->">');
    document.writeln('</applet>');

    writeFooter();

    IEDataAppl.ondatasetcomplete = IEscanForReadOnly;
    IEDataAppl.ondatasetchanged  = IEscanForReadOnly;
    IEDataAppl.oncellchange      = IEresetReadOnly;
  }
}


/*
 * return Microsoft Internet Explorer (major) version number, or 0 for other
 * browsers.  This function works by finding the "MSIE " string and
 * extracting the version number following the space, up to the decimal point
 * for the minor version, which is ignored.
 *
 * This code was extracted from the Microsoft Internet SDK.
 */
function MSIEversion() {
  var ua = window.navigator.userAgent;
  var msie = ua.indexOf ( "MSIE " );
  if ( msie > 0 )    // is Microsoft Internet Explorer; return version number
    return parseInt ( ua.substring ( msie+5, ua.indexOf ( ".", msie ) ) );
  else
    return 0;        // is other browser
}




/***********************************************************************
 ***                      Netscape definitions                       ***
 ***********************************************************************/

var pageContainsElements = false;
	 
/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will block the event from reaching the
 * object if the object has an "isEditable" property which is set to false.
 */

function NScheckEditable() {
/*if (debug) {
    java.lang.System.out.print("NScheckEditable called by ");
    java.lang.System.out.print(this);
    java.lang.System.out.print(", isEditable = ");
    java.lang.System.out.println(this.isEditable);
  }*/
  
  if (this.className == "readOnlyElem")
    return false;
  else
    return true;
}


/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will invoke the notifyListener method on
 * the DataApplet just before the event occurs.
 */

function NSchangeNotify()  {
  NSchangeNotifyElem(this);
  return NScheckEditable();
}

function NSchangeNotifyElem(elem) {
  if (document.applets["NSDataAppl"] != null)
    document.applets["NSDataAppl"].notifyListener(elem.id);
  else if (document.all && document.all.NSDataAppl)
    document.all.NSDataAppl.notifyListener(elem.id);
}


/*
 * Examine a form element during startup, and setup any required event
 * handlers.
 */

var NSelementList;

function NSregisterElement(elem) {

  pageContainsElements = true;

  if (elem.name.toLowerCase() == "requiredtag")
    requiredTag = elem.value;

  else if (elem.name && elem.name.indexOf("NOT_DATA") == -1) {
    NSelementList.push(elem);
    switch (elem.type.toLowerCase()) {

    case "select-one" :
    case "select-multiple":
      if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
      elem.onchange = NSchangeNotify;
      if (ieVersion > 0) IEsetupSelectValues(elem);
      break;

    case "text" :
    case "textarea" :
      if (debug) document.writeln("Setting "+elem.name+".onKeyDown<BR>");
      elem.onkeydown = NScheckEditable;
      if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
      elem.onchange = NSchangeNotify;
      break;

    case "checkbox":
      elem.onclick = NSchangeNotify;
      break;

    default:
      // elem is of type HIDDEN, RESET, SUBMIT, FILEUPLOAD, PASSWORD,
      // BUTTON, or RADIO.  no event handlers need to be setup
      // for these elements.
    }
  }
}
				// function object for use with elementIterate
function NSregisterElementObj() { this.func = NSregisterElement; }

function NSAssignIdentifiers() {
  for (i = NSelementList.length;   i > 0; ) {
    i--;
    NSelementList[i].id = "dashelem_" + i;
  }
}

function NSSetupElements() {
  NSelementList = new Array();
  elementIterate(new NSregisterElementObj());
  NSAssignIdentifiers();
}

/*
 * Netscape top-level setup procedure.
 */

function NSSetup() {

  if (debug) document.writeln("<p>Setting up under Netscape, ");

  NSSetupElements();

  if (pageContainsElements == true) {
    if (debug) document.writeln("<p>creating applet.");
    document.writeln('<applet id=NSDataAppl name=NSDataAppl'+
		            ' archive="/'+AppletName+'.jar" '+
		            ' code=net.sourceforge.processdash.data.applet.DataApplet'+
		            ' width=1 height=1 MAYSCRIPT>');
    if (requiredTag != "")
      document.writeln('<param name=requiredTag value="' + requiredTag +'">');
    if (unlocked)
      document.writeln('<param name=unlock value=true>');
    document.writeln('<param name=docURL value="'+window.location.href+'">');
    document.writeln('<param name=ieVersion value="'+ieVersion+'">');
    document.writeln('<param name=nsVersion value="'+nsVersion+'">');
    document.writeln('<param name=debug value="<!--#echo dataApplet.debug -->">');
    document.writeln('<param name=disableDOM value="<!--#echo dataApplet.disableDOM -->">');
    document.writeln('</applet>');

    writeFooter();
  }
}


/*
 * Top-level setup procedure to force the use of the sun plugin.
 */

function ForcePlugInSetup() {

  if (debug) document.writeln("<p>Setting up in IE, forcing use of plug-in ");

  NSSetupElements();

  if (pageContainsElements == true) {
    if (debug) document.writeln("<p>creating applet.");
    document.writeln
	('<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" '+
	     'width="1" height="1" id="NSDataAppl" '+
  	     'codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_4-windows-i586.cab#Version=1,4,0,0">'+
	     '<param name=type value="application/x-java-applet;version=1.4">'+
	     '<param name=CODE value="net.sourceforge.processdash.data.applet.DataApplet">'+
	     '<param name=ARCHIVE value="/'+AppletName+'.jar">'+
	     '<param name=NAME value="NSDataAppl">'+
	     '<param name=scriptable value="true">'+
  	     '<param name=MAYSCRIPT value=true>');
	 
    if (requiredTag != "")
      document.writeln('<param name=requiredTag value="' + requiredTag +'">');
    if (unlocked)
      document.writeln('<param name=unlock value=true>');
    document.writeln('<param name=docURL value="'+window.location.href+'">');
    document.writeln('<param name=ieVersion value="'+ieVersion+'">');
    document.writeln('<param name=nsVersion value="'+nsVersion+'">');
    document.writeln('<param name=debug value="<!--#echo dataApplet.debug -->">');
    document.writeln('<param name=disableDOM value="<!--#echo dataApplet.disableDOM -->">');
    document.writeln('</object>');

    writeFooter();
  }
}


/*
 * return Netscape version number, or 0 for other browsers.
 */

function NSversion() {
  var aN = window.navigator.appName;
  var aV = window.navigator.appVersion;

  if (aN == "Netscape")
    return parseInt (aV.substring (0, aV.indexOf(".", 0)));
  else
    return 0;
}


function isWindows() {
  var agt=navigator.userAgent.toLowerCase();
  return ( (agt.indexOf("win")!=-1) || (agt.indexOf("16bit")!=-1) )
}

function lookForToken(token, persist) {
    if (window.location.search.indexOf(token) != -1) {
        var ckie = "DataApplet=" + token + "; path=/";
	if (persist) {
	    ckie = ckie + "; expires=Wednesday, 31-Dec-08 23:00:00 GMT";
        }
	document.cookie = ckie;
	return true;
    }
    return (document.cookie.indexOf("DataApplet="+token) != -1);
}

function usingPlugIn() { return lookForToken("UsingJavaPlugIn", false); }
function forcePlugIn() { return lookForToken("ForceJavaPlugIn", true);  }


/***********************************************************************
 ***                         MAIN PROCEDURE                          ***
 ***********************************************************************/


if (debug) document.writeln("Starting setup process.");

ieVersion = MSIEversion();
nsVersion = NSversion();

if (ieVersion >= 4) {
    if (forcePlugIn())
	ForcePlugInSetup();
    else
	NSSetup();
}
else if (nsVersion >= 4)
    NSSetup();
else
    top.location.pathname =
        "/help/Topics/Troubleshooting/DataApplet/OtherBrowser.htm";

if (debug) document.writeln("<p>done with data.js.");
