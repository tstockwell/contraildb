package com.googlecode.contraildb.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.attrs.StackMapFrame;
import org.objectweb.asm.attrs.StackMapTableAttribute;
import org.objectweb.asm.attrs.StackMapType;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CompileTestsDump implements Opcodes {

	public static byte[] dump () throws Exception {

		ClassWriter cw = new ClassWriter(false);
		MethodVisitor mv;

		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "com/googlecode/contraildb/tests/CompileTests", null, "java/lang/Object", null);

		cw.visitSource("CompileTests.java", null);

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
			mv.visitTypeInsn(NEW, "com/googlecode/contraildb/tests/CompileTests");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "com/googlecode/contraildb/tests/CompileTests", "<init>", "()V");
			mv.visitInsn(ICONST_0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/googlecode/contraildb/tests/CompileTests", "compare", "(I)V");
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
			mv.visitLdcInsn("klajsdhfkjh");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "(Ljava/lang/String;)V");
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitTypeInsn(NEW, "java/lang/String");
			mv.visitLdcInsn("asdfasdfasdf");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "(Ljava/lang/String;)V");
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			{
				// ATTRIBUTE
				List frames = new ArrayList();
				{
					List locals = new ArrayList();
					{
						StackMapType attrframe0Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Object);
						attrframe0Info0.setObject("com/googlecode/contraildb/tests/CompileTests");
						locals.add(attrframe0Info0);
					}
					List stack = Collections.EMPTY_LIST;
					StackMapFrame attrframe0 = new StackMapFrame(l0, locals, stack);
					frames.add(attrframe0);
				}
				{
					List locals = new ArrayList();
					{
						StackMapType attrframe1Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Object);
						attrframe1Info0.setObject("com/googlecode/contraildb/tests/CompileTests");
						locals.add(attrframe1Info0);
					}
					List stack = Collections.EMPTY_LIST;
					StackMapFrame attrframe1 = new StackMapFrame(l1, locals, stack);
					frames.add(attrframe1);
				}
				{
					List locals = new ArrayList();
					{
						StackMapType attrframe2Info0 = StackMapType.getTypeInfo( StackMapType.ITEM_Object);
						attrframe2Info0.setObject("com/googlecode/contraildb/tests/CompileTests");
						locals.add(attrframe2Info0);
					}
					List stack = Collections.EMPTY_LIST;
					StackMapFrame attrframe2 = new StackMapFrame(l2, locals, stack);
					frames.add(attrframe2);
				}
				StackMapTableAttribute attr = new StackMapTableAttribute(frames);
				mv.visitAttribute(attr);
			}
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}
}