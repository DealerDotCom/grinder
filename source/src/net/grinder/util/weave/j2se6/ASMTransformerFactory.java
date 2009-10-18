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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.grinder.util.Pair;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver.PointCutRegistry;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


/**
 * {@link ClassFileTransformerFactory} implementation that uses ASM to
 * advise methods.
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
   * <p>
   * We can't add fields to the class due to DCR limitations, so we have to wire
   * in the advice class using static methods.{@code adviceClass} should
   * implement two methods with the following names and signatures.
   * </p>
   *
   * <pre>
   * public static void enter(Object reference,
   *                          String location);
   *
   * public static void exit(Object reference,
   *                         String location,
   *                         boolean success);
   * </pre>
   *
   *
   * @param adviceClass
   *          Class that provides the advice.
   * @throws WeavingException
   *           If {@code adviceClass} does not implement {@code enter} and
   *           {@code exit} static methods.
   */
  public ASMTransformerFactory(Class<?> adviceClass) throws WeavingException {

    try {
      final Method enterMethod =
        adviceClass.getMethod("enter", Object.class, String.class);

      if (!Modifier.isStatic(enterMethod.getModifiers())) {
        throw new WeavingException("Enter method is not static");
      }

      final Method exitMethod =
        adviceClass.getMethod("exit", Object.class, String.class, Boolean.TYPE);

      if (!Modifier.isStatic(exitMethod.getModifiers())) {
        throw new WeavingException("Exit method is not static");
      }
    }
    catch (Exception e) {
      throw new WeavingException(
        adviceClass.getName() + " does not expected enter and exit methods",
        e);
    }

    m_adviceClass = Type.getInternalName(adviceClass);
  }

  /**
   * {@inheritDoc}
   */
  public ClassFileTransformer create(PointCutRegistry pointCutRegistry) {
    return new ASMTransformer(pointCutRegistry);
  }

  /**
   * {@link ClassFileTransformer} that advise methods using ASM.
   *
   * @author Philip Aston
   * @version $Revision:$
   */
  private class ASMTransformer implements ClassFileTransformer {

    private final PointCutRegistry m_pointCutRegistry;

    /**
     * Constructor.
     *
     * <p>
     * Each method has at most one advice. If the class is re-transformed,
     * perhaps with additional advised methods, we will be passed the original
     * class byte code . We rely on a {@link PointCutRegistry} to remember which
     * methods to advise.
     * </p>
     *
     * @param pointCutRegistry
     *          Remembers the methods to advice, and the location strings.
     */
    public ASMTransformer(PointCutRegistry pointCutRegistry) {
      m_pointCutRegistry = pointCutRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] transform(ClassLoader loader,
                            final String internalClassName,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] originalBytes)
      throws IllegalClassFormatException {

      // The PointCutRegistry provides us the constructors and methods to advise
      // organised by class. This allows us quickly to find the right methods,
      // and ignore classes that aren't to be advised. (Important, since we're
      // called for every class that is loaded).
      final Map<Constructor<?>, String> constructorToLocation =
        m_pointCutRegistry.getConstructorPointCutsForClass(internalClassName);

      final Map<Method, String> methodToLocation =
        m_pointCutRegistry.getMethodPointCutsForClass(internalClassName);

      final int size =
        (constructorToLocation != null ? constructorToLocation.size() : 0) +
        (methodToLocation != null ? methodToLocation.size() : 0);

      if (size == 0) {
        return null;
      }

      // Having found the right set of constructors methods, we transform the
      // key to a form that is easier for our ASM visitor to use.
      final Map<Pair<String, String>, String> nameAndDescriptionToLocation =
        new HashMap<Pair<String, String>, String>(size);

      if (constructorToLocation != null) {
        for (Entry<Constructor<?>, String> entry :
             constructorToLocation.entrySet()) {

          final Constructor<?> c = entry.getKey();

          nameAndDescriptionToLocation.put(
            new Pair<String, String>("<init>",
                                     Type.getConstructorDescriptor(c)),
            entry.getValue());
        }
      }

      if (methodToLocation != null) {
        for (Entry<Method, String> entry : methodToLocation.entrySet()) {
          final Method m = entry.getKey();

          nameAndDescriptionToLocation.put(
            new Pair<String, String>(m.getName(), Type.getMethodDescriptor(m)),
            entry.getValue());
        }
      }

      final ClassReader classReader = new ClassReader(originalBytes);

      final ClassWriter classWriter =
        new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

      ClassVisitor visitorChain = classWriter;

      // Uncomment to see the generated code:
//      visitorChain =
//        new TraceClassVisitor(visitorChain, new PrintWriter(System.err));

      visitorChain = new AddAdviceClassAdapter(
                           visitorChain,
                           Type.getType("L" + internalClassName + ";"),
                           nameAndDescriptionToLocation);

      // Uncomment to see the original code:
//      visitorChain =
//        new TraceClassVisitor(visitorChain, new PrintWriter(System.out));

      classReader.accept(visitorChain, 0);

      return classWriter.toByteArray();
    }
  }

  private final class AddAdviceClassAdapter extends ClassAdapter {

    private final Type m_internalClassType;
    private final Map<Pair<String, String>, String> m_locations;

    private AddAdviceClassAdapter(ClassVisitor classVisitor,
                                  Type internalClassType,
                                  Map<Pair<String, String>, String> locations) {
      super(classVisitor);
      m_internalClassType = internalClassType;
      m_locations = locations;
    }

    @Override
    public void visit(int originalVersion,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {

      cv.visit(Math.max(originalVersion & 0xFFFF, Opcodes.V1_5),
               access,
               name,
               signature,
               superName,
               interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String desc,
                                     final String signature,
                                     final String[] exceptions) {

      final MethodVisitor defaultVisitor =
        cv.visitMethod(access, name, desc, signature, exceptions);

      final String location = m_locations.get(
        new Pair<String, String>(name, desc));

      if (location != null) {
        assert defaultVisitor != null;

        return new AdviceMethodVisitor(defaultVisitor,
                                       m_internalClassType,
                                       access,
                                       name,
                                       location);
      }

      return defaultVisitor;
    }
  }

  /**
   * <p>
   * Generate our advice.
   * </p>
   * <p>
   * Originally this was based on
   * {@link org.objectweb.asm.commons.AdviceAdapter}. This had the following
   * problems:
   * </p>
   * <ul>
   * <li>
   * 95% of {@code AdviceAdapter} code exists to call
   * {@link org.objectweb.asm.commons.AdviceAdapter#onMethodEnter()} for a
   * constructor after it has called {@code this()} or {@code super()}. This
   * seems unnatural for our purposes, we really want to wrap our {@code
   * TRYCATCHBLOCK} around the whole constructor.</li>
   * <li>
   * {@code AdviceAdapter} doesn't handle exceptions that propagate through the
   * method, so we must add our own {@code TRYCATCHBLOCK} handling. We need to
   * ignore add code before other adapters in the chain {@see
   * #visitTryCatchBlock}), which the {@code onMethodEnter} callback doesn't let
   * us do.</li>
   * <li>
   * The {@code AdviceAdapter} class Javadoc doesn't match the implementation -
   * a smell.</li>
   * <li>
   * {@code AdviceAdapter} was the only reason we required the ASM {@code
   * commons} jar.</li>
   *
   * </ul>
   *
   * @author Philip Aston
   * @version $Revision:$
   */
  private final class AdviceMethodVisitor
    extends MethodAdapter implements Opcodes {

    private final Type m_internalClassType;
    private final String m_location;
    private final boolean m_isStatic;

    private final Label m_entryLabel = new Label();
    private final Label m_exceptionExitLabel = new Label();
    private boolean m_tryCatchBlockNeeded = true;
    private boolean m_entryCallNeeded = true;

    private AdviceMethodVisitor(MethodVisitor mv,
                                Type internalClassType,
                                int access,
                                String name,
                                String location) {
      super(mv);

      m_internalClassType = internalClassType;
      m_location = location;

      // We handle constructors like static methods, since the reference to
      // the new object is useless.
      m_isStatic = (access & Opcodes.ACC_STATIC) != 0 ||
                   name.equals("<init>");
    }

    private void generateTryCatchBlock() {
      if (m_tryCatchBlockNeeded) {
        super.visitTryCatchBlock(m_entryLabel,
                                 m_exceptionExitLabel,
                                 m_exceptionExitLabel,
                                 null);

        m_tryCatchBlockNeeded = false;
      }
    }

    private void generateEntryCall() {
      if (m_entryCallNeeded) {
        super.visitLabel(m_entryLabel);

        if (m_isStatic) {
          super.visitLdcInsn(m_internalClassType);
        }
        else {
          super.visitVarInsn(ALOAD, 0);
        }

        super.visitLdcInsn(m_location);

        super.visitMethodInsn(INVOKESTATIC,
                              m_adviceClass,
                              "enter",
                              "(Ljava/lang/Object;Ljava/lang/String;)V");

        m_entryCallNeeded = false;
      }
    }

    private void generateEntryBlocks() {
      generateTryCatchBlock();
      generateEntryCall();
    }

    private void generateExitCall(boolean success) {
      if (m_isStatic) {
        super.visitLdcInsn(m_internalClassType);
      }
      else {
        super.visitVarInsn(ALOAD, 0);
      }

      super.visitLdcInsn(m_location);
      super.visitInsn(success ? ICONST_1 : ICONST_0);

      super.visitMethodInsn(INVOKESTATIC,
                            m_adviceClass,
                            "exit",
                            "(Ljava/lang/Object;Ljava/lang/String;Z)V");
    }

    /**
     * To nest well if another similar transformation has been done. we must
     * ensure that any existing top level TryCatchBlock comes first. Otherwise
     * our TryCatchBlock would have higher precedence, and other catch blocks
     * would be skipped.
     *
     * <p>
     * This is the reason for the delayed generateEntryBlock*() calls.
     * Unfortunately, this considerably adds to the complexity of this adapter.
     * </p>
     */
    @Override public void visitTryCatchBlock(Label start,
                                             Label end,
                                             Label handler,
                                             String type) {

      super.visitTryCatchBlock(start, end, handler, type);
      generateTryCatchBlock();
    }

    @Override public void visitLabel(Label label) {
      generateEntryBlocks();
      super.visitLabel(label);
    }

    @Override public void visitFrame(int type,
                                     int nLocal,
                                     Object[] local,
                                     int nStack,
                                     Object[] stack) {
      generateEntryBlocks();
      super.visitFrame(type, nLocal, local, nStack, stack);
    }

    public void visitInsn(int opcode) {
      generateEntryBlocks();

      switch (opcode) {
        case RETURN:
        case IRETURN:
        case FRETURN:
        case ARETURN:
        case LRETURN:
        case DRETURN:
          generateExitCall(true);
          break;

        default:
          break;
      }

      super.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
      generateEntryBlocks();
      super.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(int opcode, int var) {
      generateEntryBlocks();
      super.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(int opcode, String type) {
      generateEntryBlocks();
      super.visitTypeInsn(opcode, type);
    }

    public void visitFieldInsn(int opcode,
                               String owner,
                               String name,
                               String desc) {
      generateEntryBlocks();
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode,
                                String owner,
                                String name,
                                String desc) {
      generateEntryBlocks();
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
      generateEntryBlocks();
      super.visitJumpInsn(opcode, label);
    }

    public void visitLdcInsn(Object cst) {
      generateEntryBlocks();
      super.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
      generateEntryBlocks();
      super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min,
                                     int max,
                                     Label dflt,
                                     Label[] labels) {
      generateEntryBlocks();
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt,
                                      int[] keys,
                                      Label[] labels) {
      generateEntryBlocks();
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
      generateEntryBlocks();
      super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitLocalVariable(String name,
                                   String desc,
                                   String signature,
                                   Label start,
                                   Label end,
                                   int index) {
      generateEntryBlocks();
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void visitLineNumber(int line, Label start) {
      generateEntryBlocks();
      super.visitLineNumber(line, start);
    }

    @Override public void visitMaxs(int maxStack, int maxLocals) {
      super.visitLabel(m_exceptionExitLabel);
      generateExitCall(false);
      super.visitInsn(ATHROW);       // Re-throw.
      super.visitMaxs(maxStack, maxLocals);
    }
  }
}
