package net.grinder.testutility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.grinder.util.StreamCopier;


/**
 * <p>
 * Create objects that unknown to the standard class loaders. Useful for testing
 * ClassNotFoundException handling.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class IsolatedObjectFactory {

  public static Class getIsolatedObjectClass() {
    try {
      return new SimpleObjectClassLoader().loadClass("SimpleObject");
    }
    catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  public static Object getIsolatedObject() {
    try {
      return getIsolatedObjectClass().newInstance();
    }
    catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private static class SimpleObjectClassLoader extends ClassLoader {

    protected synchronized Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException {

      if (name.equals("SimpleObject")) {
        Class c = findLoadedClass(name);

        if (c == null) {
          final InputStream resource =
            getParent().getResourceAsStream(
               getClass().getPackage().getName().replace('.', '/') +
              "/resources/SimpleObject.clazz");

          final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

          try {
            new StreamCopier(1000, true).copy(resource, byteStream);
            final byte[] bytes = byteStream.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
          }
          catch (IOException e) {
            throw new ClassNotFoundException(name, e);
          }
        }

        if (resolve) {
          resolveClass(c);
        }

        return c;
      }

      return super.loadClass(name, resolve);
    }
  }
}
