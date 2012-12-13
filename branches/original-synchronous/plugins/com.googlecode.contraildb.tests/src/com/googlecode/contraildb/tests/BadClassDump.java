package com.googlecode.contraildb.tests;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
public class BadClassDump implements Opcodes {
	
	public static final boolean $isWoven = true;
	
	public static void main(String[] args) throws Exception {
		byte[] bytes= dump();
		File outFile= new File("com/googlecode/contraildb/tests/KilimConcurrencyTests$BadClass.class");
		OutputStream outputStream= new FileOutputStream(outFile);
		outputStream.write(bytes);
		outputStream.flush();
		outputStream.close();
		System.out.println("Wrote class to "+outFile.getCanonicalPath());
	}
	public static void dump(int pc) throws Exception {
		if (pc == 0) {
			System.out.println("0");
		}
		else if (pc == 1) {
			System.out.println("1");
		}
		else if (pc == 2) {
			System.out.println("2");
		}
		else if (pc == 65) {
			System.out.println("65");
		}
	}

	public static byte[] dump () throws Exception {

		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(V1_6, ACC_SUPER, "com/googlecode/contraildb/tests/KilimConcurrencyTests$BadClass", null, "java/lang/Object", null);

		cw.visitInnerClass("com/googlecode/contraildb/tests/KilimConcurrencyTests$BadClass", "com/googlecode/contraildb/tests/KilimConcurrencyTests", "BadClass", ACC_STATIC);

		{
			fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "$isWoven", "Z", null, new Integer(1));
			fv.visitEnd();
		}
		{
			mv = cw.visitMethod(0, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "doSomething", "(I)V", null, new String[] { "kilim/Pausable", "java/lang/Exception" });
			mv.visitCode();
			mv.visitVarInsn(ILOAD, 1);

			Label l1 = new Label();
			Label l3 = new Label();
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(IF_ICMPNE, l1);
			mv.visitTypeInsn(NEW, "java/lang/String");
			mv.visitJumpInsn(GOTO, l3);
			
			mv.visitLabel(l1);
			mv.visitTypeInsn(NEW, "java/lang/String");
			
			mv.visitLabel(l3);
			mv.visitInsn(POP);
			mv.visitInsn(RETURN);
			mv.visitMaxs(4, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "doSomething", "()V", null, new String[] { "kilim/Pausable", "java/lang/Exception" });
			mv.visitCode();
			mv.visitMethodInsn(INVOKESTATIC, "kilim/Task", "errNotWoven", "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(0, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "getBytes", "(Lkilim/Fiber;)[B", null, new String[] { "kilim/Pausable" });
			mv.visitCode();
			mv.visitLdcInsn("lkj;lkjad;fa");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "getBytes", "()[B", null, new String[] { "kilim/Pausable" });
			mv.visitCode();
			mv.visitMethodInsn(INVOKESTATIC, "kilim/Task", "errNotWoven", "()V");
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}
	
	

	
//	public static byte[] dump () throws Exception {
//
//		ClassWriter cw = new ClassWriter(0);
//		FieldVisitor fv;
//		MethodVisitor mv;
//
//		cw.visit(V1_6, ACC_SUPER, "com/googlecode/contraildb/tests/KilimConcurrencyTests$BadClass", null, "java/lang/Object", null);
//
//		cw.visitInnerClass("com/googlecode/contraildb/tests/KilimConcurrencyTests$BadClass", "com/googlecode/contraildb/tests/KilimConcurrencyTests", "BadClass", ACC_STATIC);
//
//		{
//			fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "$isWoven", "Z", null, new Integer(1));
//			fv.visitEnd();
//		}
//		{
//			mv = cw.visitMethod(0, "<init>", "()V", null, null);
//			mv.visitCode();
//			mv.visitVarInsn(ALOAD, 0);
//			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
//			mv.visitInsn(RETURN);
//			mv.visitMaxs(1, 1);
//			mv.visitEnd();
//		}
//		{
//			mv = cw.visitMethod(ACC_PUBLIC, "doSomething", "(Lkilim/Fiber;)V", null, new String[] { "kilim/Pausable", "java/lang/Exception" });
//			mv.visitCode();
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitFieldInsn(GETFIELD, "kilim/Fiber", "pc", "I");
//			Label l0 = new Label();
//			Label l1 = new Label();
//			Label l2 = new Label();
//			mv.visitTableSwitchInsn(0, 0, l2, new Label[] { l0, l1 });
//			mv.visitLabel(l2);
//			mv.visitMethodInsn(INVOKESTATIC, "kilim/Fiber", "wrongPC", "()V");
//			mv.visitLabel(l1);
//			
//			mv.visitInsn(ACONST_NULL);
//			mv.visitVarInsn(ALOAD, 0);
//			Label l3 = new Label();
//			mv.visitJumpInsn(GOTO, l3);
//			
//			mv.visitLabel(l0);
//			mv.visitTypeInsn(NEW, "java/lang/String");
//			mv.visitVarInsn(ALOAD, 0);
//			mv.visitLabel(l3);
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitMethodInsn(INVOKEVIRTUAL, "kilim/Fiber", "down", "()Lkilim/Fiber;");
//			mv.visitMethodInsn(INVOKEVIRTUAL, "com/googlecode/contraildb/tests/KilimConcurrencyTests$BadClass", "getBytes", "(Lkilim/Fiber;)[B");
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitMethodInsn(INVOKEVIRTUAL, "kilim/Fiber", "up", "()I");
//			Label l4 = new Label();
//			Label l5 = new Label();
//			Label l6 = new Label();
//			Label l7 = new Label();
//			mv.visitTableSwitchInsn(0, 3, l4, new Label[] { l4, l5, l6, l7 });
//			mv.visitLabel(l6);
//			mv.visitInsn(POP);
//			mv.visitTypeInsn(NEW, "kilim/S_O");
//			mv.visitInsn(DUP);
//			mv.visitMethodInsn(INVOKESPECIAL, "kilim/S_O", "<init>", "()V");
//			mv.visitVarInsn(ASTORE, 2);
//			mv.visitVarInsn(ALOAD, 2);
//			mv.visitVarInsn(ALOAD, 0);
//			mv.visitFieldInsn(PUTFIELD, "kilim/State", "self", "Ljava/lang/Object;");
//			mv.visitVarInsn(ALOAD, 2);
//			mv.visitInsn(ICONST_1);
//			mv.visitFieldInsn(PUTFIELD, "kilim/State", "pc", "I");
//			mv.visitVarInsn(ASTORE, 3);
//			mv.visitVarInsn(ALOAD, 2);
//			mv.visitVarInsn(ALOAD, 3);
//			mv.visitFieldInsn(PUTFIELD, "kilim/S_O", "f0", "Ljava/lang/Object;");
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitVarInsn(ALOAD, 2);
//			mv.visitMethodInsn(INVOKEVIRTUAL, "kilim/Fiber", "setState", "(Lkilim/State;)V");
//			mv.visitInsn(RETURN);
//			mv.visitLabel(l7);
//			mv.visitInsn(POP);
//			mv.visitInsn(POP);
//			mv.visitInsn(RETURN);
//			mv.visitLabel(l5);
//			mv.visitVarInsn(ASTORE, 2);
//			mv.visitInsn(POP);
//			mv.visitVarInsn(ALOAD, 1);
//			mv.visitFieldInsn(GETFIELD, "kilim/Fiber", "curState", "Lkilim/State;");
//			mv.visitTypeInsn(CHECKCAST, "kilim/S_O");
//			mv.visitVarInsn(ASTORE, 3);
//			mv.visitVarInsn(ALOAD, 3);
//			mv.visitFieldInsn(GETFIELD, "kilim/S_O", "f0", "Ljava/lang/Object;");
//			mv.visitTypeInsn(CHECKCAST, "java/lang/String");
//			mv.visitVarInsn(ALOAD, 2);
//			mv.visitLabel(l4);
//			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V");
//			mv.visitInsn(RETURN);
//			mv.visitMaxs(4, 4);
//			mv.visitEnd();
//		}
//		{
//			mv = cw.visitMethod(ACC_PUBLIC, "doSomething", "()V", null, new String[] { "kilim/Pausable", "java/lang/Exception" });
//			mv.visitCode();
//			mv.visitMethodInsn(INVOKESTATIC, "kilim/Task", "errNotWoven", "()V");
//			mv.visitInsn(RETURN);
//			mv.visitMaxs(0, 2);
//			mv.visitEnd();
//		}
//		{
//			mv = cw.visitMethod(ACC_PUBLIC, "getBytes", "(Lkilim/Fiber;)[B", null, new String[] { "kilim/Pausable" });
//			mv.visitCode();
//			mv.visitLdcInsn("lkj;lkjad;fa");
//			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B");
//			mv.visitInsn(ARETURN);
//			mv.visitMaxs(2, 2);
//			mv.visitEnd();
//		}
//		{
//			mv = cw.visitMethod(ACC_PUBLIC, "getBytes", "()[B", null, new String[] { "kilim/Pausable" });
//			mv.visitCode();
//			mv.visitMethodInsn(INVOKESTATIC, "kilim/Task", "errNotWoven", "()V");
//			mv.visitInsn(ACONST_NULL);
//			mv.visitInsn(ARETURN);
//			mv.visitMaxs(1, 2);
//			mv.visitEnd();
//		}
//		cw.visitEnd();
//
//		return cw.toByteArray();
//	}
}
