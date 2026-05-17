import org.objectweb.asm.*;
import java.io.*;
import java.util.Stack;
import static org.objectweb.asm.Opcodes.*;

public class bfc {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage:java -cp .:asm-8.0.jar bfc programname.bf
");
            return;
        }

        String bfFile = args[0];
        String className = bfFile.contains(".") ? bfFile.substring(0, bfFile.lastIndexOf('.')) : bfFile;
        String bfSource = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bfFile)));

        generateClassFromBF(bfSource, className);
    }

    private static void generateClassFromBF(String bf, String className) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null);

        MethodVisitor constructor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0,0);
        constructor.visitEnd();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, new String[]{"java/io/IOException"});
        mv.visitCode();

        mv.visitIntInsn(SIPUSH, 30000);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitVarInsn(ASTORE, 1); // memory
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2); // pointer

        mv.visitTypeInsn(NEW, "java/io/InputStreamReader");
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
        mv.visitMethodInsn(INVOKESPECIAL, "java/io/InputStreamReader", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitVarInsn(ASTORE, 3); // reader

        Stack<Label> loopStart = new Stack<>();
        Stack<Label> loopEnd = new Stack<>();

        for (int i = 0; i < bf.length(); i++) {
            char c = bf.charAt(i);

            if (c == '+' || c == '-') {
                int count = (c == '+') ? 1 : -1;
                while (i + 1 < bf.length() && (bf.charAt(i + 1) == '+' || bf.charAt(i + 1) == '-')) {
                    count += (bf.charAt(i + 1) == '+') ? 1 : -1;
                    i++;
                }
                if (count != 0) {
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ILOAD, 2);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ILOAD, 2);
                    mv.visitInsn(IALOAD);
                    if (count >= -1 && count <= 5) {
                        mv.visitInsn(ICONST_0 + count);
                    } else {
                        mv.visitLdcInsn(count);
                    }
                    mv.visitInsn(IADD);
                    mv.visitInsn(IASTORE);
                }
                continue;
            }

            if (c == '>' || c == '<') {
                int move = (c == '>') ? 1 : -1;
                while (i + 1 < bf.length() && (bf.charAt(i + 1) == '>' || bf.charAt(i + 1) == '<')) {
                    move += (bf.charAt(i + 1) == '>') ? 1 : -1;
                    i++;
                }
                mv.visitIincInsn(2, move);
                continue;
            }

            switch (c) {
                case '.':
                    mv.visitFieldInsn(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
                    mv.visitVarInsn(ALOAD,1); mv.visitVarInsn(ILOAD,2); mv.visitInsn(IALOAD);
                    mv.visitMethodInsn(INVOKEVIRTUAL,"java/io/PrintStream","print","(C)V",false);
                    break;
                case ',':
                    mv.visitVarInsn(ALOAD,1); mv.visitVarInsn(ILOAD,2); mv.visitVarInsn(ALOAD,3);
                    mv.visitMethodInsn(INVOKEVIRTUAL,"java/io/InputStreamReader","read","()I",false);
                    mv.visitInsn(DUP);
                    Label notEOF = new Label();
                    mv.visitJumpInsn(IFGE, notEOF);
                    mv.visitInsn(POP);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(notEOF);
                    mv.visitInsn(IASTORE);
                    break;
                case '[':
                    Label start = new Label(); Label end = new Label();
                    loopStart.push(start); loopEnd.push(end);
                    mv.visitLabel(start);
                    mv.visitVarInsn(ALOAD,1); mv.visitVarInsn(ILOAD,2); mv.visitInsn(IALOAD);
                    mv.visitJumpInsn(IFEQ,end);
                    break;
                case ']':
                    Label s = loopStart.pop(); Label e = loopEnd.pop();
                    mv.visitVarInsn(ALOAD,1); mv.visitVarInsn(ILOAD,2); mv.visitInsn(IALOAD);
                    mv.visitJumpInsn(IFNE,s);
                    mv.visitLabel(e);
                    break;
            }
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        cw.visitEnd();

        try (FileOutputStream fos = new FileOutputStream(className + ".class")) {
            fos.write(cw.toByteArray());
        }

        System.out.println(className + ".class generated!");
    }
}
