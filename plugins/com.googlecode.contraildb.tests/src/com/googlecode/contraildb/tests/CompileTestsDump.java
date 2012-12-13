package com.googlecode.contraildb.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CompileTestsDump implements Opcodes {
	
	public static void main(String[] args) throws Exception {
		byte[] bytes= dump();
		File file= new File("com/googlecode/contraildb/tests/GeneratedTest.class");
		OutputStream out= new FileOutputStream(file);
		out.write(bytes);
		out.flush();
		out.close();
		System.out.println("Successfully generated "+file);
	}
	
	public static byte[] dump () throws Exception {

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		MethodVisitor mv;

		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "com/googlecode/contraildb/tests/GeneratedTest", null, "java/lang/Object", null);

		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "com/googlecode/contraildb/tests/GeneratedTest");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "com/googlecode/contraildb/tests/GeneratedTest", "<init>", "()V");
			mv.visitInsn(ICONST_0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/googlecode/contraildb/tests/GeneratedTest", "compare", "(I)V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "compare", "(I)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ILOAD, 1);
			Label l1 = new Label();
			mv.visitJumpInsn(IFNE, l1);
			mv.visitTypeInsn(NEW, "java/lang/String");
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitTypeInsn(NEW, "java/lang/String");
			mv.visitLabel(l2);
			mv.visitInsn(POP);
			mv.visitInsn(RETURN);
			
//			// CREATE STACKMAP FRAMES
//			{
//				List frames = new ArrayList();
//				{
//					List locals = new ArrayList();
//					{
//						StackMapType attrframe0Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Object);
//						attrframe0Info0.setObject("com/googlecode/contraildb/tests/GeneratedTest");
//						locals.add(attrframe0Info0);
//					}
//					List stack = Collections.EMPTY_LIST;
//					StackMapFrame attrframe0 = new StackMapFrame(l0, locals, stack);
//					frames.add(attrframe0);
//				}
//				{
//					List locals = new ArrayList();
//					{
//						StackMapType attrframe1Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Object);
//						attrframe1Info0.setObject("com/googlecode/contraildb/tests/GeneratedTest");
//						locals.add(attrframe1Info0);
//					}
//					List stack = Collections.EMPTY_LIST;
//					StackMapFrame attrframe1 = new StackMapFrame(l1, locals, stack);
//					frames.add(attrframe1);
//				}
//				{
//					List locals = new ArrayList();
//					{
//						StackMapType attrframe2Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Object);
//						attrframe2Info0.setObject("com/googlecode/contraildb/tests/GeneratedTest");
//						locals.add(attrframe2Info0);
//					}
//					List stack = new ArrayList();
//					{
//						StackMapType attrframe2Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Top);
//						stack.add(attrframe2Info0);
//					}
//					StackMapFrame attrframe2 = new StackMapFrame(l2, locals, stack);
//					frames.add(attrframe2);
//				}
//				StackMapTableAttribute attr = new StackMapTableAttribute(frames);
//				mv.visitAttribute(attr);
//			}
			
			
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}

}