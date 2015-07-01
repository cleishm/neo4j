/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.extension;

import org.junit.Test;

import org.neo4j.embedded.CommunityTestGraphDatabase;
import org.neo4j.embedded.GraphDatabase;
import org.neo4j.embedded.TestGraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.Service;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.test.TargetDirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Base class for testing a {@link org.neo4j.kernel.extension.KernelExtensionFactory}. The base test cases in this
 * class verifies that a extension upholds the {@link org.neo4j.kernel.extension.KernelExtensionFactory} contract.
 */
public abstract class KernelExtensionFactoryContractTest
{
    protected final Class<? extends KernelExtensionFactory<?>> extClass;
    private final String key;
    private final TargetDirectory target;

    public KernelExtensionFactoryContractTest( String key, Class<? extends KernelExtensionFactory<?>> extClass )
    {
        this.target = TargetDirectory.forTest( getClass() );
        this.extClass = extClass;
        this.key = key;
    }

    public TestGraphDatabase graphdb( int instance )
    {
        TestGraphDatabase.EphemeralBuilder builder = CommunityTestGraphDatabase.buildEphemeral();
        configure( builder, true, instance );
        return builder.open();
    }

    /**
     * Override to create default configuration for the {@link org.neo4j.kernel.extension.KernelExtensionFactory}
     * under test.
     *
     * @param builder    the builder to configure
     * @param shouldLoad <code>true</code> if configuration that makes the
     *                   extension load should be created, <code>false</code> if
     *                   configuration that makes the extension not load should be
     *                   created.
     * @param instance   used for differentiating multiple instances that will run
     *                   simultaneously.
     */
    protected void configure( TestGraphDatabase.EphemeralBuilder builder, boolean shouldLoad, int instance )
    {
    }

    static KernelExtensions getExtensions( GraphDatabaseService graphdb )
    {
        return ((GraphDatabaseAPI) graphdb).getDependencyResolver().resolveDependency( KernelExtensions.class );
    }

    @Test
    public void extensionShouldHavePublicNoArgConstructor() throws Exception
    {
        KernelExtensionFactory<?> instance = null;
        try
        {
            instance = newInstance();
        }
        catch ( IllegalArgumentException failure )
        {
            failure.printStackTrace();
            fail( "Contract violation: extension class must have public no-arg constructor (Exception in stderr)" );
        }
        assertNotNull( instance );
    }

    @Test
    public void shouldBeAbleToLoadExtensionAsAServiceProvider() throws Exception
    {
        KernelExtensionFactory<?> instance = null;
        try
        {
            instance = loadInstance();
        }
        catch ( ClassCastException failure )
        {
            failure.printStackTrace();
            fail( "Loaded instance does not match the extension class (Exception in stderr)" );
        }

        assertNotNull( "Could not load the kernel extension with the provided key", instance );
        assertTrue( "Class of the loaded instance is a subclass of the extension class",
                instance.getClass() == extClass );
    }

    @Test
    public void differentInstancesShouldHaveEqualHashCodesAndBeEqual() throws Exception
    {
        KernelExtensionFactory<?> one = newInstance();
        KernelExtensionFactory<?> two = newInstance();
        assertEquals( "new instances have different hash codes", one.hashCode(), two.hashCode() );
        assertEquals( "new instances are not equals", one, two );

        one = loadInstance();
        two = loadInstance();
        assertEquals( "loaded instances have different hash codes", one.hashCode(), two.hashCode() );
        assertEquals( "loaded instances are not equals", one, two );

        one = loadInstance();
        two = newInstance();
        assertEquals( "loaded instance and new instance have different hash codes", one.hashCode(), two.hashCode() );
        assertEquals( "loaded instance and new instance are not equals", one, two );
    }

    @Test
    public void canLoadKernelExtension() throws Exception
    {
        GraphDatabase graphdb = graphdb( 0 );
        try
        {
            assertTrue( "Failed to load extension", getExtensions( graphdb ).isRegistered( extClass ) );
        }
        finally
        {
            graphdb.shutdown();
        }
    }

    private final KernelExtensionFactory<?> newInstance()
    {
        try
        {
            return extClass.newInstance();
        }
        catch ( Exception cause )
        {
            throw new IllegalArgumentException( "Could not instantiate extension class", cause );
        }
    }

    protected final KernelExtensionFactory<?> loadInstance()
    {
        return extClass.cast( Service.load( KernelExtensionFactory.class, key ) );
    }
}
