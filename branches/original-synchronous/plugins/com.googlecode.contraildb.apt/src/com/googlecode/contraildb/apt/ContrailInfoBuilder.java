package com.googlecode.contraildb.apt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;


import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.apt.core.env.EclipseAnnotationProcessorEnvironment;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import com.googlecode.meteorframework.core.annotation.MeteorAnnotationUtils;
import com.googlecode.meteorframework.core.annotation.ModelElement;
import com.sun.mirror.declaration.InterfaceDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.PackageDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;

public class ContrailInfoBuilder {
	
	EclipseAnnotationProcessorEnvironment _env;
	PackageDeclaration _declaration;
	StringWriter _writer= new StringWriter();
	PrintWriter _printWriter= new PrintWriter(_writer);
	ArrayList<TypeDeclaration> _classDeclarations= new ArrayList<TypeDeclaration>();
	String _bundleName;

	public ContrailInfoBuilder(EclipseAnnotationProcessorEnvironment env, PackageDeclaration packageDeclaration) {
		_env= env;
		_declaration= packageDeclaration;
		_classDeclarations.addAll(_declaration.getInterfaces());
		_classDeclarations.addAll(_declaration.getClasses());
		IProject project= _env.getJavaProject().getProject();
		IPluginModelBase modelBase= PluginRegistry.findModel(project);
		if (modelBase == null)
			throw new RuntimeException("Projects that use Meteor must be Eclipse PDE projects");
		BundleDescription bundleDescription= modelBase.getBundleDescription();
		if (bundleDescription == null) {
			_bundleName= project.getName();
		}
		else
			_bundleName= bundleDescription.getSymbolicName();
	}
	
	public String getMeteorInfo() {
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		String simpleName= _declaration.getSimpleName();
		String packageName= _declaration.getQualifiedName();
		String infoClassName= simpleName.substring(0,1).toUpperCase()+simpleName.substring(1)+"NS";
		
		writer.println("package "+packageName+";\n");
		if (!"com.googlecode.meteorframework.core".equals(packageName)) {
			writer.println("import com.googlecode.meteorframework.core.JavaMeteorMetadataProvider;");
			writer.println("import com.googlecode.meteorframework.core.Scope;");
		}
		writer.println("import com.googlecode.meteorframework.core.annotation.Generated;\n");
		//writer.println("import java.net.URL;\n");

		writer.println("@Generated public class "+infoClassName+" extends JavaMeteorMetadataProvider {\n");

		writer.println("\tpublic static final String NAMESPACE= \"meteor:"+packageName+"\";\n");
		writer.println("\tpublic static final String BUNDLE= \""+_bundleName+"\";\n");

		int classCount= 0;
		while (!_classDeclarations.isEmpty()) {
			TypeDeclaration classDeclaration= _classDeclarations.remove(0);
			
			if (classDeclaration.getAnnotation(ModelElement.class) == null)
				continue;
			
//			boolean isResource= false;
//			ClassDeclaration superDeclaration= classDeclaration;
//			while (superDeclaration != null && !isResource) {
//				if (superDeclaration.getQualifiedName().equals(Resource.class.getName())) 
//					isResource= true;
//				ClassType classType= superDeclaration.getSuperclass();
//				superDeclaration= (classType != null) ? classType.getDeclaration() : null;
//			}
//			if (!isResource)
//				continue;
			
			writeClassNames(writer, classDeclaration, "\t");
			classCount++;
		}
		
		if (classCount <= 0)
			return null;

		
		/*
		 * Create method for contributing metadata.
		 */ 
		writer.println("\n");
		writer.println("\tpublic void contributeMetaData(com.googlecode.meteorframework.core.Scope scope) {");
		writer.println("\t	super.contributeMetaData(scope, BUNDLE);");
		writer.println("\t}\n");
		
		writer.println("}");
		writer.flush();
		return stringWriter.toString();
	}

	private void writeClassNames(PrintWriter writer, TypeDeclaration classDeclaration, String prefix) 
	{
		writer.println(prefix+"public static interface "+classDeclaration.getSimpleName()+" {");
		
		String qualifiedClassName= getQualifiedName(classDeclaration);
		writer.println(prefix+"\tpublic static final String TYPE = \"meteor:"+qualifiedClassName+"\";\n");
		
		boolean writeComment= false;
		HashSet<String> properties= new HashSet<String>(); 
		for (MethodDeclaration methodDeclaration : classDeclaration.getMethods())
		{
			if (!MeteorAnnotationUtils.isMeteorProperty(methodDeclaration))
				continue;

			String methodName= methodDeclaration.getSimpleName();
			if (!writeComment) {
				writer.println(prefix+"\t// properties");
				writeComment= true;
			}
			String propertyName= methodName.startsWith("is") ? 
					methodName.substring(2,3).toLowerCase()+methodName.substring(3) :
					methodName.substring(3,4).toLowerCase()+methodName.substring(4);
					
			if (!properties.contains(propertyName))
			{
				properties.add(propertyName);
				
				writer.println(prefix+"\tpublic static final String "+propertyName+" = \"meteor:"+qualifiedClassName+"."+propertyName+"\";");
			}
		}
		if (writeComment)
			writer.println();
		
		writeComment= false;
		HashSet<String> methods= new HashSet<String>(); 
		for (MethodDeclaration methodDeclaration : classDeclaration.getMethods())
		{
			if (!methodDeclaration.getModifiers().contains(Modifier.PUBLIC))
				continue;
			if (methodDeclaration.getModifiers().contains(Modifier.STATIC))
				continue;

//			if (methodDeclaration.getAnnotation(Model.class) == null)
//				continue;

			if (MeteorAnnotationUtils.isMeteorProperty(methodDeclaration))
				continue;

			String methodName= methodDeclaration.getSimpleName();
			if (!methods.contains(methodName))
			{
				methods.add(methodName);
				if (!writeComment) {
					writer.println(prefix+"\t// methods");
					writeComment= true;
				}
				writer.println(prefix+"\tpublic static final String "+methodName+" = \"meteor:"+qualifiedClassName+"."+methodName+"\";");
			}
		}
		if (writeComment)
			writer.println();
		
		// write any nested classes
		for (TypeDeclaration declaration : classDeclaration.getNestedTypes()) {
			if (declaration.getAnnotation(ModelElement.class) == null)
				continue;
			if (declaration instanceof InterfaceDeclaration) {
				writeClassNames(writer, (InterfaceDeclaration)declaration, prefix+"\t");
			}
		}
		
		writer.println(prefix+"}\n");		
	}

	private String getQualifiedName(TypeDeclaration classDeclaration) {
		String name= classDeclaration.getQualifiedName();
		TypeDeclaration declaringType= classDeclaration.getDeclaringType();
		if (declaringType != null) 
			name= getQualifiedName(declaringType)+"$"+classDeclaration.getSimpleName();
		return name;
	}

}

