// // Copyright (C) 2009 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.util.weave.j2se6;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;


/**
 * {@link ClassFileTransformerFactory} implementation that uses ASM to
 * apply the instrumentation.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class ASMTransformerFactory
  implements ClassFileTransformerFactory {

  private final String m_adviceClass;

  /**
   * Constructor.
   *
   * @param adviceClass
   *          Class that provides the advice.
   */
  public ASMTransformerFactory(Class<?> adviceClass) {
    m_adviceClass = Type.getInternalName(adviceClass);
  }

  /**
   * {@inheritDoc}
   */
  public ClassFileTransformer create(Map<Method, String> methodsAndLocations) {
    return new ASMTransformer(methodsAndLocations);
  }


  /**
   * {@link ClassFileTransformer} that adds instrumentation using ASM.
   *
   * @author Philip Aston
   * @version $Revision:$
   */
  private class ASMTransformer implements ClassFileTransformer {

    private final Map<String, Map<String, String>>
      m_classNameToMethodNameToLocation =
        new HashMap<String, Map<String, String>>();

    /**
     * Constructor.
     *
     * <p>
     * We can't add fields to instrumented class due to DCR limitations, so we
     * have to wire in the advice class using static methods. We expect {@code
     * adviceClass} to implement two static methods, {@code enter} and {@code
     * exit}. Both methods should have the similar signatures:
     *
     * <ul>
     * <li>The first parameter is a {@link Object}, and will be passed the
     * instrumented instance, or {code null} if for static methods.</li>
     * <li>The second parameter is an opaque {@link String} that uniquely
     * identifies the instrumentation point.</li>
     * <li>The {@code exit} method has a third boolean parameter that indicates
     * whether or the return is normal ({@code true}) or due to an exception (
     * {@code false}).
     * <li>The return type of both methods is {@code void}.</li>
     * </ul>
     * </p>
     *
     * @param instrumentationPoints
     *          The methods to instrument, and the location strings.
     */
    public ASMTransformer(Map<Method, String> instrumentationPoints) {

      for (Entry<Method, String> e : instrumentationPoints.entrySet()) {

        final String internalClassName =
          Type.getInternalName(e.getKey().getDeclaringClass());

        final Map<String, String> methodNameToLocation;

        final Map<String, String> existing =
          m_classNameToMethodNameToLocation.get(internalClassName);

        if (existing != null) {
          methodNameToLocation = existing;
        }
        else {
          methodNameToLocation = new HashMap<String, String>();
          m_classNameToMethodNameToLocation.put(internalClassName,
                                                methodNameToLocation);
        }

        methodNameToLocation.put(e.getKey().getName(), e.getValue());
      }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] originalBytes)
      throws IllegalClassFormatException {

      final Map<String, String> methodNameToLocation =
        m_classNameToMethodNameToLocation.get(className);

      if (methodNameToLocation == null) {
        return null;
      }

      final ClassReader classReader = new ClassReader(originalBytes);

      final ClassWriter classWriter =
        new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

      ClassVisitor visitorChain = classWriter;

      // Uncomment to see the generated code:
      //  visitorChain =
      //    new TraceClassVisitor(visitorChain, new PrintWriter(System.err));

      visitorChain = new ClassAdapter(visitorChain) {
        @Override
        public MethodVisitor visitMethod(final int access,
                                         final String name,
                                         final String desc,
                                         final String signature,
                                         final String[] exceptions) {
            final MethodVisitor defaultVisitor =
              cv.visitMethod(access, name, desc, signature, exceptions);

            final String location = methodNameToLocation.get(name);

            if (location != null) {
              assert defaultVisitor != null;

              return new AdviceAdapter(defaultVisitor, access, name, desc) {
                @Override
                public void onMethodEnter() {
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitLdcInsn(location);
                  mv.visitMethodInsn(
                    INVOKESTATIC,
                    m_adviceClass,
                    "enter",
                    "(Ljava/lang/Object;Ljava/lang/String;)V");
                }


                @Override
                public void onMethodExit(int opCode) {
                  mv.visitVarInsn(ALOAD, 0);
                  mv.visitLdcInsn(location);
                  mv.visitInsn(opCode == ATHROW ? ICONST_0: ICONST_1);

                  mv.visitMethodInsn(
                    INVOKESTATIC,
                    m_adviceClass,
                    "exit",
                    "(Ljava/lang/Object;Ljava/lang/String;Z)V");
                }
              };
            }

            return defaultVisitor;
          }
      };

      // Uncomment to see the original code:
      //  visitorChain =
      //    new TraceClassVisitor(visitorChain, new PrintWriter(System.out));

      classReader.accept(visitorChain, 0);

      return classWriter.toByteArray();
    }
  }
}
