package io.github.whetfire.lateral;

import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;

public class Assembler {
    static final private int JAVA_VERSION = 55; // 55.0 = Java 11

    static Keyword LABEL = Keyword.makeKeyword("label");

    static Keyword ICONST = Keyword.makeKeyword("iconst");
    static Keyword LDC = Keyword.makeKeyword("ldc");
    static Keyword ARETURN = Keyword.makeKeyword("areturn");
    static Keyword RETURN = Keyword.makeKeyword("return");
    static Keyword IRETURN = Keyword.makeKeyword("ireturn");
    static Keyword ALOAD = Keyword.makeKeyword("aload");
    static Keyword ASTORE = Keyword.makeKeyword("astore");
    static Keyword AALOAD = Keyword.makeKeyword("aaload");
    static Keyword AASTORE = Keyword.makeKeyword("aastore");
    static Keyword ARRAYLENGTH = Keyword.makeKeyword("arraylength");
    static Keyword ATHROW = Keyword.makeKeyword("athrow");

    static Keyword CHECKCAST = Keyword.makeKeyword("checkcast");
    static Keyword NEW = Keyword.makeKeyword("new");
    static Keyword ANEWARRAY = Keyword.makeKeyword("anewarray");

    static Keyword DUP = Keyword.makeKeyword("dup");

    static Keyword INVOKESTATIC = Keyword.makeKeyword("invokestatic");
    static Keyword INVOKEVIRTUAL = Keyword.makeKeyword("invokevirtual");
    static Keyword INVOKESPECIAL = Keyword.makeKeyword("invokespecial");
    static Keyword INVOKEINTERFACE = Keyword.makeKeyword("invokeinterface");

    static Keyword INVOKEDYNAMIC = Keyword.makeKeyword("invokedynamic");

    static Keyword IF_ICMPNE = Keyword.makeKeyword("if_icmpne");
    static Keyword IFNULL = Keyword.makeKeyword("ifnull");

    static Keyword GETSTATIC = Keyword.makeKeyword("getstatic");
    static Keyword PUTSTATIC = Keyword.makeKeyword("putstatic");
    static Keyword GETFIELD = Keyword.makeKeyword("getfield");
    static Keyword PUTFIELD = Keyword.makeKeyword("putfield");

    private static Map<Keyword, Integer> simpleOpMap;
    private static Map<Keyword, Integer> jumpOpMap;
    private static Map<Keyword, Integer> opMap;

    static {
        simpleOpMap = Map.ofEntries(
            Map.entry(ARETURN, Opcodes.ARETURN),
            Map.entry(RETURN, Opcodes.RETURN),
            Map.entry(IRETURN, Opcodes.IRETURN),
            Map.entry(DUP, Opcodes.DUP),
            Map.entry(AALOAD, Opcodes.AALOAD),
            Map.entry(AASTORE, Opcodes.AASTORE),
            Map.entry(ARRAYLENGTH, Opcodes.ARRAYLENGTH),
            Map.entry(ATHROW, Opcodes.ATHROW),
            Map.entry(Keyword.makeKeyword("dup2"), Opcodes.DUP2),
            Map.entry(Keyword.makeKeyword("dup_x1"), Opcodes.DUP_X1),
            Map.entry(Keyword.makeKeyword("pop"), Opcodes.POP),
            Map.entry(Keyword.makeKeyword("swap"), Opcodes.SWAP),
            Map.entry(Keyword.makeKeyword("isub"), Opcodes.ISUB),
            Map.entry(Keyword.makeKeyword("iadd"), Opcodes.IADD)
        );

        jumpOpMap = Map.ofEntries(
            Map.entry(IFNULL, Opcodes.IFNULL),
            Map.entry(Keyword.makeKeyword("ifnonnull"), Opcodes.IFNULL),
            Map.entry(Keyword.makeKeyword("ifne"), Opcodes.IFNE),
            Map.entry(Keyword.makeKeyword("ifeq"), Opcodes.IFEQ),
            Map.entry(Keyword.makeKeyword("ifgt"), Opcodes.IFGT),
            Map.entry(Keyword.makeKeyword("iflt"), Opcodes.IFLT),
            Map.entry(Keyword.makeKeyword("if_icmpgt"), Opcodes.IF_ICMPGT),
            Map.entry(IF_ICMPNE, Opcodes.IF_ICMPNE),
            Map.entry(Keyword.makeKeyword("goto"), Opcodes.GOTO)
        );

        opMap = Map.ofEntries(
            Map.entry(INVOKESTATIC, Opcodes.INVOKESTATIC),
            Map.entry(INVOKEVIRTUAL, Opcodes.INVOKEVIRTUAL),
            Map.entry(INVOKESPECIAL, Opcodes.INVOKESPECIAL),
            Map.entry(INVOKEINTERFACE, Opcodes.INVOKEINTERFACE),
            Map.entry(PUTSTATIC, Opcodes.PUTSTATIC),
            Map.entry(PUTFIELD, Opcodes.PUTFIELD),
            Map.entry(GETSTATIC, Opcodes.GETSTATIC),
            Map.entry(GETFIELD, Opcodes.GETFIELD),
            Map.entry(CHECKCAST, Opcodes.CHECKCAST),
            Map.entry(ANEWARRAY, Opcodes.ANEWARRAY),
            Map.entry(NEW, Opcodes.NEW)
        );
    }

    static Class<?>[] getParameterClasses(int count) {
        Class<?>[] classes = new Class[count];
        for(int i = 0; i < count; i ++) {
            classes[i] = Object.class;
        }
        return classes;
    }

    static String getMethodDescriptor(Class<?> ... classes) {
        if(classes == null || classes.length < 1)
            throw new RuntimeException("malformed method descriptor");
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for(int i = 0; i < classes.length - 1; i ++) {
            sb.append(Type.getDescriptor(classes[i]));
        }
        sb.append(')');
        sb.append(Type.getDescriptor(classes[classes.length - 1]));
        return sb.toString();
    }

    static String getMethodDescriptor(Class<?> returnType, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for(int i = 0; i < count; i ++) {
            sb.append(Type.getDescriptor(Object.class));
        }
        sb.append(')');
        sb.append(Type.getDescriptor(returnType));
        return sb.toString();
    }

    private static void visitOpCodes(MethodVisitor mv, Iterable<Object> opcodes) {
        HashMap<Symbol, Label> labelMap = new HashMap<>();
        for(Object opcode : opcodes) {
            // System.out.println(opcode);
            if(opcode instanceof Sequence) {
                Keyword head = (Keyword) ((Sequence) opcode).first();
                Sequence body = ((Sequence) opcode).rest();
                if(jumpOpMap.containsKey(head)) {
                    int opcodeValue = jumpOpMap.get(head);
                    Symbol labelName = (Symbol) body.first();
                    if(labelMap.containsKey(labelName)) {
                        mv.visitJumpInsn(opcodeValue, labelMap.get(labelName));
                    } else {
                        Label label = new Label();
                        mv.visitJumpInsn(opcodeValue, label);
                        labelMap.put(labelName, label);
                    }
                } else if(head.equals(LABEL)) {
                    Symbol labelName = (Symbol) body.first();
                    if(labelMap.containsKey(labelName)) {
                        mv.visitLabel(labelMap.get(labelName));
                    } else {
                        Label label = new Label();
                        mv.visitLabel(label);
                        labelMap.put(labelName, label);
                    }
                } else if(head.equals(LDC)) {
                    mv.visitLdcInsn(((Sequence) opcode).second());
                } else if(INVOKESTATIC.equals(head) || INVOKEVIRTUAL.equals(head)
                        || INVOKESPECIAL.equals(head) || INVOKEINTERFACE.equals(head)) {
                    mv.visitMethodInsn(opMap.get(head),
                            (String) body.first(),
                            (String) body.second(),
                            (String) body.third(),
                            INVOKEINTERFACE.equals(head));
                } else if(INVOKEDYNAMIC.equals(head)) {
                    // (:invokedynamic (handle-class handle-name handle-type) dyn-name dyn-type bsma ...)
                    Sequence handleArgs = (Sequence) body.first();
                    Handle dynamicHandle = new Handle(
                            Opcodes.H_INVOKESTATIC,
                            (String) handleArgs.first(),
                            (String) handleArgs.second(),
                            (String) handleArgs.third(),
                            false);

                    Object[] bootstrapArgs = new Object[body.length() - 3];
                    for(int i = 0; i < bootstrapArgs.length; i ++) {
                        bootstrapArgs[i] = body.nth(i + 3);
                    }

                    mv.visitInvokeDynamicInsn(
                            (String) body.second(),
                            (String) body.third(),
                            dynamicHandle,
                            bootstrapArgs
                    );
                } else if(head.equals(GETSTATIC) || head.equals(GETFIELD) ||
                head.equals(PUTSTATIC) || head.equals(PUTFIELD)) {
                    mv.visitFieldInsn(opMap.get(head),
                            (String) body.first(),
                            (String) body.second(),
                            (String) body.third());
                } else if(head.equals(ALOAD)) {
                    int value = (Integer) body.first();
                    mv.visitVarInsn(Opcodes.ALOAD, value);
                } else if(head.equals(ASTORE)) {
                    int value = (Integer) body.first();
                    mv.visitVarInsn(Opcodes.ASTORE, value);
                } else if(head.equals(ICONST)) {
                    int value = (Integer) body.first();
                    if (-1 <= value && value <= 5) {
                        mv.visitInsn(Opcodes.ICONST_0 + value);
                    } else {
                        mv.visitLdcInsn(value);
                    }
                } else if(head.equals(CHECKCAST) || head.equals(NEW) || head.equals(ANEWARRAY)) {
                    // checkcast, new, anewarray, instanceof
                    mv.visitTypeInsn(opMap.get(head), (String) body.first());
                } else {
                    throw new RuntimeException(head.toString());
                }
            } else {
                if(opcode instanceof Keyword && simpleOpMap.containsKey(opcode)) {
                    mv.visitInsn(simpleOpMap.get(opcode));
                } else {
                    throw new RuntimeException(opcode.toString());
                }
            }
        }
    }

    /**
     * Converts a tree representing a JVM class into the byte array representation of the class
     * @param asmTree Sequence based tree
     * @return byte array representation of the asmTree class
     */
    static byte[] buildClass(Sequence asmTree) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
        // TODO: assert first arguments match expected
        // first is defclass
        String name = (String) asmTree.second();
        // TODO: get meta
        Sequence meta = (Sequence) asmTree.third();

        classWriter.visit(JAVA_VERSION, Opcodes.ACC_PUBLIC, name, null,
                Type.getInternalName(Function.class), null);

        for(int i = 0; i < 3; i ++) {
            asmTree = asmTree.rest();
        }

        for(Object obj : asmTree) {
            if(obj instanceof Sequence) {
                Sequence member = (Sequence) obj;
                Object head = member.first();
                if(Compiler.DEFMETHOD.equals(head)) {
                    //System.out.println(member);
                    String mname = (String) member.second();
                    String descriptor = (String) member.third();
                    // TODO: get meta
                    Sequence mmeta = (Sequence) member.fourth();

                    MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, mname, descriptor,
                            null, null);
                    for (int i = 0; i < 4; i++) {
                        member = member.rest();
                    }
                    //System.out.println(member);
                    mv.visitCode();
                    visitOpCodes(mv, member);
                    mv.visitMaxs(-1, -1);
                    mv.visitEnd();
                } else if(Compiler.DEFFIELD.equals(head)) {
                    classWriter.visitField(Opcodes.ACC_PUBLIC,
                            (String) member.second(),
                            (String) member.third(),
                            null, null);
                } else {
                    // TODO subclass
                    throw new SyntaxException();
                }
            } else {
                throw new SyntaxException();
            }
        }

        byte[] classBytes = classWriter.toByteArray();
        /*
        PrintWriter printWriter = new PrintWriter(System.out);
        CheckClassAdapter.verify(new ClassReader(classBytes), true, printWriter);
        System.out.println("================");
        // */
        return classBytes;
    }
}
