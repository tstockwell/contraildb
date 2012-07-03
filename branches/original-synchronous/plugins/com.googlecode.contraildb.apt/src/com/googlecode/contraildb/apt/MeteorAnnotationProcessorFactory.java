package com.googlecode.contraildb.apt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.apt.core.env.EclipseAnnotationProcessorEnvironment;

import com.googlecode.meteorframework.core.annotation.ModelElement;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.AnnotationProcessors;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

public class MeteorAnnotationProcessorFactory 
implements AnnotationProcessorFactory
{
	public Collection<String> supportedOptions() {
		return Collections.emptyList();
	}

	public Collection<String> supportedAnnotationTypes() {
		ArrayList<String> annotations = new ArrayList<String>();
		annotations.add( ModelElement.class.getName() );
		return annotations;
	}

	public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> atds, AnnotationProcessorEnvironment env) {
		if (atds == null || atds.isEmpty())
			return AnnotationProcessors.NO_OP;
		for (AnnotationTypeDeclaration declaration : atds) {
			if (declaration.getQualifiedName().equals(ModelElement.class.getName())) {
				return new MeteorAnnotationProcessor( (EclipseAnnotationProcessorEnvironment)env );
			}
		}
		return null;
	}
}
