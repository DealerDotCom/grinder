// Base64FormatException.java
// $Id$
// (c) COPYRIGHT MIT and INRIA, 1996.
// Please first read the full copyright statement in file COPYRIGHT.html

package w3c.tools.codec ;

/**
 * Exception for invalid BASE64 streams.
 */

public class Base64FormatException extends Exception {
    public Base64FormatException (String msg) {
	super(msg) ;
    }
}
