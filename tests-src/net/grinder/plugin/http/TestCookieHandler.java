// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.plugin.http;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import net.grinder.plugininterface.LogCounter;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestCookieHandler extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestCookieHandler.class);
    }

    public TestCookieHandler(String name)
    {
	super(name);
    }

    private final String m_domain = "host.grinder.net";
    private final String m_path = "/resource";
    private CookieHandler m_cookieHandler = null;
    private LogCounter m_logCounter = null;
    private URL m_url = null;

    protected void setUp() throws Exception
    {
	m_logCounter = new LogCounter();
	m_cookieHandler = new CookieHandler(m_logCounter);
	m_url = new URL("http", m_domain, m_path);
    }

    public void testSimpleCaptureCookies()
    {
	m_cookieHandler.setCookies("abc=def", m_url);

	final CookieHandler.Store store = m_cookieHandler.getStore();
	assertEquals(1, store.getSize());

	final CookieHandler.Cookie cookie = store.get("abc");
	assertNotNull(cookie);

	assertEquals("abc", cookie.getName());
	assertEquals("def", cookie.getValue());
	assertNull(cookie.getComment());
	assertEquals(m_domain, cookie.getDomain());
	assertEquals(m_path, cookie.getPath());
	assert(!cookie.getSecure());
	assertEquals(-1, cookie.getVersion());

	assertEquals(0, m_logCounter.getNumberOfErrors());
    }

    public void testCaptureCookiesWithAttributes()
    {
	m_cookieHandler.setCookies(
	    "name= value;MaX-AgE =1;;;CoMMent=comment;secure;" +
	    "dOMAIN = .grinder.net;version=-1; path=/xx  ", m_url);

	final CookieHandler.Store store = m_cookieHandler.getStore();
	assertEquals(1, store.getSize());

	final CookieHandler.Cookie cookie = store.get("name");
	assertNotNull(cookie);

	assertEquals("name", cookie.getName());
	assertEquals("value", cookie.getValue());
	assertEquals("comment", cookie.getComment());
	assertEquals(".grinder.net", cookie.getDomain());
	assertEquals("/xx", cookie.getPath());
	assert(cookie.getSecure());
	assertEquals(-1, cookie.getVersion());

	assertEquals(0, m_logCounter.getNumberOfErrors());
    }

    public void testCaptureCookiesWithMultipleCookies()
    {
	m_cookieHandler.setCookies(
	    "n1=v1;comment=The first; Version=21 ;name2=value2;path=/apath",
	    m_url);

	final CookieHandler.Store store = m_cookieHandler.getStore();
	assertEquals(2, store.getSize());

	final CookieHandler.Cookie cookie1 = store.get("n1");
	assertNotNull(cookie1);
	assertEquals("v1", cookie1.getValue());
	assertEquals("The first", cookie1.getComment());
	assertEquals(21, cookie1.getVersion());
	assertEquals(m_path, cookie1.getPath());

	final CookieHandler.Cookie cookie2 = store.get("name2");
	assertNotNull(cookie2);
	assertEquals("value2", cookie2.getValue());
	assertNull(cookie2.getComment());
	assertEquals(-1, cookie2.getVersion());
	assertEquals("/apath", cookie2.getPath());

	assertEquals(0, m_logCounter.getNumberOfErrors());
    }

    public void testCaptureCookiesWithUpdatesToCookies()
    {
	m_cookieHandler.setCookies("name1=value1;name2=value2", m_url);

	final CookieHandler.Store store = m_cookieHandler.getStore();
	assertEquals(2, store.getSize());

	final CookieHandler.Cookie cookie1 = store.get("name1");
	assertEquals("value1", cookie1.getValue());

	final CookieHandler.Cookie cookie2 = store.get("name2");
	assertEquals("value2", cookie2.getValue());


	m_cookieHandler.setCookies("name1=value3", m_url);
	assertEquals(2, store.getSize());

	final CookieHandler.Cookie cookie3 = store.get("name1");
	assertEquals("value3", cookie3.getValue());


	m_cookieHandler.setCookies("name1=value4;Max-Age = 0", m_url);
	assertEquals(1, store.getSize());
	assertNotNull(store.get("name2"));


	m_cookieHandler.setCookies("name3=something;max-age=0;name4=", m_url);
	assertEquals(1, store.getSize());


	m_cookieHandler.setCookies("name2=;", m_url);
	assertEquals(0, store.getSize());


	assertEquals(0, m_logCounter.getNumberOfErrors());
    }

    public void testCaptureCookiesWithDomains() throws Exception
    {
	int cookies = 0;
	int errorMessages = 0;

	URL url = null;

	final CookieHandler.Store store = m_cookieHandler.getStore();

	// Examples from RFC 2109 - 4.3.2

	// Should be rejected.
	url = new URL("http", "y.x.foo.com", "/");
	m_cookieHandler.setCookies("n1=v1;domain=.foo.com", url);
	assertEquals(cookies, store.getSize());
	assertEquals(++errorMessages, m_logCounter.getNumberOfErrors());

	// Should be accepted.
	url = new URL("http", "x.foo.com", "/");
	m_cookieHandler.setCookies("n2=v1;dOmain=.foo.com", url);
	assertEquals(++cookies, store.getSize());
	assertEquals(errorMessages, m_logCounter.getNumberOfErrors());

	// Should be rejected.
	url = new URL("http", "anyhost", "/abc");
	m_cookieHandler.setCookies("n3=v1;doMain=.com", url);
	assertEquals(cookies, store.getSize());
	assertEquals(++errorMessages, m_logCounter.getNumberOfErrors());

	// Should be rejected.
	url = new URL("http", "anyhost", "/abc");
	m_cookieHandler.setCookies("n3=v1;doMain=.com.", url);
	assertEquals(cookies, store.getSize());
	assertEquals(++errorMessages, m_logCounter.getNumberOfErrors());

	// Should be rejected.
	url = new URL("http", "anyhost", "/abc");
	m_cookieHandler.setCookies("n4=v1;Domain=ajax.com", url);
	assertEquals(cookies, store.getSize());
	assertEquals(++errorMessages, m_logCounter.getNumberOfErrors());
    }

    public void testCaptureCookiesWithPaths() throws Exception
    {
	final URL url1 = new URL("http", "host", "/abc");
	m_cookieHandler.setCookies("name1=value1", url1);

	final URL url2 = new URL("http", "host", "/abc/");
	m_cookieHandler.setCookies("name2=value2", url2);

	final CookieHandler.Store store = m_cookieHandler.getStore();

	final CookieHandler.Cookie cookie1 = store.get("name1");
	assertEquals("/abc", cookie1.getPath());
	
	final CookieHandler.Cookie cookie2 = store.get("name2");
	assertEquals("/abc", cookie2.getPath());
    }

    public void testCaptureCookiesWithDuffStrings()
    {
	int cookies = 0;
	int errorMessages = 0;

	final CookieHandler.Store store = m_cookieHandler.getStore();

	// These should be ignored.
	m_cookieHandler.setCookies("", m_url);
	m_cookieHandler.setCookies("  ", m_url);
	m_cookieHandler.setCookies(";; ;;;", m_url);
	assertEquals(cookies, store.getSize());
	assertEquals(errorMessages, m_logCounter.getNumberOfErrors());

	// These should be ignored, but generate error messages.
	m_cookieHandler.setCookies("=def", m_url);
	m_cookieHandler.setCookies("=", m_url);
	assertEquals(cookies, store.getSize());
	assertEquals(errorMessages += 2, m_logCounter.getNumberOfErrors());

	// Secure with a value should generate an error message.
	m_cookieHandler.setCookies("n1=v;Secure=something", m_url);
	assertEquals(++cookies, store.getSize());
	assertEquals(errorMessages += 1, m_logCounter.getNumberOfErrors());

	// Version without an integer should generate error message.
	m_cookieHandler.setCookies("n2=v;Version=", m_url);
	m_cookieHandler.setCookies("n2=v;version=ABC", m_url);
	m_cookieHandler.setCookies("n2=v; version=1a", m_url);
	m_cookieHandler.setCookies("n2=v;Version = 1.1", m_url);
	m_cookieHandler.setCookies("n2=v;verSion=-0.1", m_url);
	assertEquals(++cookies, store.getSize());
	assertEquals(errorMessages += 5, m_logCounter.getNumberOfErrors());

	// Max-Age without a non-negative integer should generate error message.
	m_cookieHandler.setCookies("n3=v;max-age=", m_url);
	m_cookieHandler.setCookies("n3=v;maX-AGE=ABC", m_url);
	m_cookieHandler.setCookies("n3=v;MaX-aGe=1a", m_url);
	m_cookieHandler.setCookies("n3=v;MAX-AGE=1.1", m_url);
	m_cookieHandler.setCookies("n3=v;max-age=-0.1", m_url);
	m_cookieHandler.setCookies("n3=v;max-age=-1", m_url);
	assertEquals(++cookies, store.getSize());
	assertEquals(errorMessages += 6, m_logCounter.getNumberOfErrors());
    }

    public void testGetCookieString() throws Exception
    {
	final CookieHandler.Store store = m_cookieHandler.getStore();
	final URL url = new URL("http", "host.somewhere",
				"/resource/abc.html");

	m_cookieHandler.setCookies("name=value", url);

	assertEquals("$Version=\"0\"; name=value",
		     m_cookieHandler.getCookieString(url));


	m_cookieHandler.setCookies("name=value; version=10", url);
	assertEquals(1, store.getSize());

	assertEquals("$Version=\"10\"; name=value",
		     m_cookieHandler.getCookieString(url));


	m_cookieHandler.setCookies("name=value; version=10; path=/", url);
	assertEquals(2, store.getSize());

	assertEquals("$Version=\"10\"; " +
		     "name=value; $Path=\"/\"; " +
		     "name=value",
		     m_cookieHandler.getCookieString(url));

	m_cookieHandler.setCookies(
	    "name=value; version=10; path=/resource/abc.html", url);
	assertEquals(3, store.getSize());

	assertEquals("$Version=\"10\"; " +
		     "name=value; $Path=\"/resource/abc.html\"; " +
		     "name=value; $Path=\"/\"; " +
		     "name=value",
		     m_cookieHandler.getCookieString(url));

	m_cookieHandler.setCookies("name=value; version=10; path=/resource/",
				   url);
	assertEquals(4, store.getSize());

	assertEquals("$Version=\"10\"; " +
		     "name=value; $Path=\"/resource/abc.html\"; " +
		     "name=value; $Path=\"/resource\"; " +
		     "name=value; $Path=\"/\"; " +
		     "name=value",
		     m_cookieHandler.getCookieString(url));
		     
	assertEquals(0, m_logCounter.getNumberOfErrors());
    }

    public void testRFC2109Example1() throws Exception
    {
	final URL url1 = new URL("http", m_domain, "/acme/login");

	m_cookieHandler.setCookies(
	    "Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\"", url1);

	final CookieHandler.Store store = m_cookieHandler.getStore();
	assertEquals(1, store.getSize());
	CookieHandler.Cookie cookie = store.get("Customer");
	assertEquals("\"WILE_E_COYOTE\"", cookie.getValue());
	assertEquals(1, cookie.getVersion());
	assertEquals("/acme", cookie.getPath());

	final URL url2 = new URL("http", m_domain, "/acme/pickitem");

	final String cookieString1 = m_cookieHandler.getCookieString(url2);
	final String expectedVersionString = "$Version=\"1\"";
	final String expectedCookie1String =
	    "; Customer=\"WILE_E_COYOTE\"; $Path=\"/acme\"";

	assertEquals(expectedVersionString + expectedCookie1String,
		     cookieString1);

	m_cookieHandler.setCookies(
	    "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\";\n\t" +
	    "Path=\"/acme\"", url2);

	assertEquals(2, store.getSize());
	cookie = store.get("Part_Number");
	assertEquals("\"Rocket_Launcher_0001\"", cookie.getValue());
	assertEquals(1, cookie.getVersion());
	assertEquals("/acme", cookie.getPath());

	final URL url3 = new URL("http", m_domain, "/acme/shipping");

	final String cookieString2 = m_cookieHandler.getCookieString(url3);
	final String expectedCookie2String =
	    "; Part_Number=\"Rocket_Launcher_0001\"; $Path=\"/acme\"";

	assert(cookieString2.equals(expectedVersionString +
				    expectedCookie1String + 
				    expectedCookie2String) ||
	       cookieString2.equals(expectedVersionString +
				    expectedCookie2String +
				    expectedCookie1String));

	m_cookieHandler.setCookies(
	    "Shipping=\"FedEx\"; Version=\"1\"; Path=\"/acme\"", url3);

	assertEquals(3, store.getSize());
	cookie = store.get("Shipping");
	assertEquals("\"FedEx\"", cookie.getValue());
	assertEquals(1, cookie.getVersion());
	assertEquals("/acme", cookie.getPath());

	final URL url4 = new URL("http", m_domain, "/acme/process");

	final String cookieString3 = m_cookieHandler.getCookieString(url4);
	final String expectedCookie3String =
	    "; Shipping=\"FedEx\"; $Path=\"/acme\"";

	assertEquals(expectedVersionString.length() +
		     expectedCookie1String.length() +
		     expectedCookie2String.length() +
		     expectedCookie3String.length(),
		     cookieString3.length());

	assert(cookieString3.indexOf(expectedVersionString) != -1);
	assert(cookieString3.indexOf(expectedCookie1String) != -1);
	assert(cookieString3.indexOf(expectedCookie2String) != -1);
	assert(cookieString3.indexOf(expectedCookie3String) != -1);
    }

    public void testRFC2109Example2() throws Exception
    {
	final URL url1 = new URL("http", "host.domain", "/acme/");

	m_cookieHandler.setCookies("Part_Number=\"Rocket_Launcher_0001\"; " +
				   "Version=\"1\";Path=\"/acme\"", url1);

	m_cookieHandler.setCookies("Part_Number=\"Riding_Rocket_0023\"; " +
				   "Version=\"1\";Path=\"/acme/ammo\"", url1);

	final CookieHandler.Store store = m_cookieHandler.getStore();
	assertEquals(2, store.getSize());

	final URL url2 = new URL("http", "host.domain",
				 "/acme/ammo/something");

	assertEquals("$Version=\"1\"; " +
		     "Part_Number=\"Riding_Rocket_0023\"; " +
		     "$Path=\"/acme/ammo\"; " +
		     "Part_Number=\"Rocket_Launcher_0001\"; $Path=\"/acme\"",
		     m_cookieHandler.getCookieString(url2));

	final URL url3 = new URL("http", "host.domain",
				 "/acme/parts");

	assertEquals("$Version=\"1\"; " +
		     "Part_Number=\"Rocket_Launcher_0001\"; $Path=\"/acme\"",
		     m_cookieHandler.getCookieString(url3));
    }

    public void testMaxAge()
    {
	final CookieHandler.Store store = m_cookieHandler.getStore();

	m_cookieHandler.setCookies("n1=v;max-age=0", m_url);
	assertEquals(0, store.getSize());

	m_cookieHandler.setCookies("n2=v;max-age=1", m_url);
	m_cookieHandler.setCookies("n3=v;max-age=3", m_url);
	assertEquals(2, store.getSize());

	sleep(1000);

	m_cookieHandler.getCookieString(m_url);
	assertEquals(1, store.getSize());
    }

    private void sleep(long time)
    {
	final long startTime = System.currentTimeMillis();
	
	while ((System.currentTimeMillis() - startTime) < time) {
	    try {
		Thread.sleep(time/10);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }
}
