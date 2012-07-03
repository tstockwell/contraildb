package com.googlecode.contraildb.apt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.apt.core.env.EclipseAnnotationProcessorEnvironment;
import org.eclipse.jdt.core.IJavaProject;

import com.googlecode.meteorframework.core.annotation.ModelElement;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.PackageDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;

public class ContrailAnnotationProcessor 
implements AnnotationProcessor
{

	private static final String METEOR_MANIFEST_HEADER = "# Meteor Content Providers";
	private EclipseAnnotationProcessorEnvironment	_env;
	private AnnotationTypeDeclaration _modelDeclaration;
	
	public ContrailAnnotationProcessor(EclipseAnnotationProcessorEnvironment env)
	{
		_env = env;
		_modelDeclaration= (AnnotationTypeDeclaration)
			env.getTypeDeclaration(ModelElement.class.getName());
	}

	public void process()
	{		
		Collection<Declaration> declarations= _env.getDeclarationsAnnotatedWith(_modelDeclaration);
		HashSet<String> completed= new HashSet<String>();
		for (Declaration declaration : declarations) {
			
			if ((declaration instanceof TypeDeclaration) == false) {
				continue;
			}
			
			TypeDeclaration classDeclaration= (TypeDeclaration)declaration;
			PackageDeclaration packageDeclaration= classDeclaration.getPackage();
			String packageName= packageDeclaration.getQualifiedName();
			if (!completed.contains(packageName)) {
				try {
					String className= packageDeclaration.getSimpleName();
					className= className.substring(0, 1).toUpperCase()+className.substring(1);
					className+= "NS";
					String serviceName= packageDeclaration.getQualifiedName()+"."+className;
					
					boolean serviceExistsInPackage= createMeteorInfoForPackage(packageDeclaration, serviceName) != null;
					recordDefaultDiscoveryInfo(packageDeclaration, serviceName, serviceExistsInPackage);
				}
				catch (Throwable t) {
					t.printStackTrace();
					_env.getMessager().printError("Error generating Meteor metadata for "+packageDeclaration.getQualifiedName()+":"+t.getMessage());
				}
				finally {
					completed.add(packageName);
				}
			}
		}
		
	}	
	
	

	/**
	 * Write the given class name into the META-INF/services/com.googlecode.meteorframework.MeteorMetadataProvider file.
	 * This is the 'standard' Java way of creating discovery information for Java service providers.
	 * See http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider.
	 *
	 * Meteor will look for this information at startup and use it to initally 
	 * populate its metadata repository. 
	 * @throws IOException 
	 * @throws CoreException 
	 */
	private void recordDefaultDiscoveryInfo(PackageDeclaration packageDeclaration, String serviceName, boolean serviceExistsInPackage) 
	throws IOException, CoreException 
	{
		
		HashSet<String> providerList= new HashSet<String>();
		IJavaProject javaProject= _env.getJavaProject();
		IProject project= javaProject.getProject();
		IFolder folder= project.getFolder("META-INF");
		if (folder.exists() == false) {
			folder.create(true, true, new NullProgressMonitor());
		}
		folder= folder.getFolder("services");
		if (folder.exists() == false) {
			folder.create(true, true, new NullProgressMonitor());
		}
		IFile file= folder.getFile("com.googlecode.meteorframework.core.MeteorMetadataProvider");
		if (file.exists() == false) {
			InputStream inputStream= new ByteArrayInputStream(METEOR_MANIFEST_HEADER.getBytes("UTF-8"));
			file.create(inputStream, true, new NullProgressMonitor());
		}
		BufferedReader reader= new BufferedReader(new InputStreamReader(file.getContents()));
		try {
			String line= null;
			while ((line= reader.readLine()) != null) {
				if (line.startsWith("#") == false) {
					providerList.add(line);
				}
			}
		}
		finally {
			try { reader.close(); } catch (Throwable t) { }
		}
		
		if (serviceExistsInPackage) {
			providerList.add(serviceName);
		}
		else
			providerList.remove(serviceName);
		
		
		/*
		 * The file should contain a newline-separated list of unique concrete
		 * provider-class names. Space and tab characters, as well as blank lines,
		 * are ignored. The comment character is '#' (0x23); on each line all
		 * characters following the first comment character are ignored. The file
		 * must be encoded in UTF-8.
		 */
		ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
		byte[] bs= (METEOR_MANIFEST_HEADER+"\n").getBytes("UTF-8");
		outputStream.write(bs, 0, bs.length);
		for (String line: providerList) {
			bs= (line+"\n").getBytes("UTF-8");
			outputStream.write(bs, 0, bs.length);
		}
		outputStream.flush();
		ByteArrayInputStream inputStream= new ByteArrayInputStream(outputStream.toByteArray());
		file.setContents(inputStream, IFile.FORCE, new NullProgressMonitor());
	}

	protected String createMeteorInfoForPackage(PackageDeclaration packageDeclaration, String meteorInfoClassName) 
    throws IOException 
    {
		
		String fileContents= new MeteorInfoBuilder(_env, packageDeclaration).getMeteorInfo();
		if (fileContents == null || fileContents.length() <= 0)
			return null;
		
		Filer filer= _env.getFiler();
		PrintWriter writer= filer.createSourceFile(meteorInfoClassName);
		writer.write(fileContents);
		writer.flush();
		writer.close();

//		try {
//			_env.getJavaProject().getProject().build(
//					IncrementalProjectBuilder.INCREMENTAL_BUILD, 
//					new NullProgressMonitor());
//		}
//		catch (Throwable t) {
//			t.printStackTrace();
//		}
		
		return meteorInfoClassName;
	}

	public AnnotationProcessorEnvironment getEnvironment()
	{
		return _env;
	}

}
