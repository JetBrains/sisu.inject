/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.space;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Qualifier;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Java 6 Annotation {@link Processor} that generates a qualified class index for the current build.
 */
public final class SisuIndexAPT6
    extends AbstractSisuIndex
    implements Processor
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final String QUALIFIERS = "qualifiers";

    private static final String ALL = "all";

    private static final String NONE = "none";

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private ProcessingEnvironment environment;

    private String qualifiers;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public void init( final ProcessingEnvironment _environment )
    {
        environment = _environment;
        qualifiers = _environment.getOptions().get( QUALIFIERS );
    }

    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment round )
    {
        final Elements elementUtils = environment.getElementUtils();
        for ( final TypeElement anno : annotations )
        {
            if ( null != anno.getAnnotation( Qualifier.class ) )
            {
                for ( final Element elem : round.getElementsAnnotatedWith( anno ) )
                {
                    if ( elem.getKind().isClass() )
                    {
                        addClassToIndex( SisuIndex.NAMED, elementUtils.getBinaryName( (TypeElement) elem ) );
                    }
                }
            }
        }

        if ( round.processingOver() )
        {
            flushIndex();
        }

        return false;
    }

    public Iterable<? extends Completion> getCompletions( final Element element, final AnnotationMirror annotation,
                                                          final ExecutableElement member, final String userText )
    {
        return Collections.emptySet();
    }

    public Set<String> getSupportedAnnotationTypes()
    {
        if ( ALL.equalsIgnoreCase( qualifiers ) )
        {
            return Collections.singleton( "*" );
        }
        if ( NONE.equalsIgnoreCase( qualifiers ) )
        {
            return Collections.emptySet();
        }
        return Collections.singleton( SisuIndex.NAMED );
    }

    public Set<String> getSupportedOptions()
    {
        return Collections.singleton( QUALIFIERS );
    }

    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.RELEASE_6;
    }

    // ----------------------------------------------------------------------
    // Customized methods
    // ----------------------------------------------------------------------

    @Override
    protected void info( final String msg )
    {
        environment.getMessager().printMessage( Diagnostic.Kind.NOTE, msg );
    }

    @Override
    protected void warn( final String msg )
    {
        environment.getMessager().printMessage( Diagnostic.Kind.WARNING, msg );
    }

    @Override
    protected Reader getReader( final String path )
        throws IOException
    {
        final FileObject file = environment.getFiler().getResource( StandardLocation.CLASS_OUTPUT, "", path );
        return new InputStreamReader( file.openInputStream(), "UTF-8" );
    }

    @Override
    protected Writer getWriter( final String path )
        throws IOException
    {
        return environment.getFiler().createResource( StandardLocation.CLASS_OUTPUT, "", path ).openWriter();
    }
}