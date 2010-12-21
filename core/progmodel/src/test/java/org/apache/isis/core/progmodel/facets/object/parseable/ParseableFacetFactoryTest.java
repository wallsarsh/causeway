/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.progmodel.facets.object.parseable;

import java.util.List;

import org.apache.isis.applib.adapters.Parser;
import org.apache.isis.applib.annotation.Parseable;
import org.apache.isis.core.metamodel.config.internal.PropertiesConfiguration;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.spec.feature.ObjectFeatureType;
import org.apache.isis.core.progmodel.facets.AbstractFacetFactoryTest;

public class ParseableFacetFactoryTest extends AbstractFacetFactoryTest {

    private ParseableFacetFactory facetFactory;
    private PropertiesConfiguration propertiesConfiguration;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        facetFactory = new ParseableFacetFactory();
        propertiesConfiguration = new PropertiesConfiguration();
        facetFactory.setIsisConfiguration(propertiesConfiguration);
    }

    @Override
    protected void tearDown() throws Exception {
        facetFactory = null;
        super.tearDown();
    }

    @Override
    public void testFeatureTypes() {
        final List<ObjectFeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertTrue(contains(featureTypes, ObjectFeatureType.OBJECT));
        assertFalse(contains(featureTypes, ObjectFeatureType.PROPERTY));
        assertFalse(contains(featureTypes, ObjectFeatureType.COLLECTION));
        assertFalse(contains(featureTypes, ObjectFeatureType.ACTION));
        assertFalse(contains(featureTypes, ObjectFeatureType.ACTION_PARAMETER));
    }

    public void testFacetPickedUp() {

        facetFactory.process(MyParseableUsingParserName.class, methodRemover, facetHolder);

        final ParseableFacet facet = facetHolder.getFacet(ParseableFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof ParseableFacetAbstract);
    }

    public void testFacetFacetHolderStored() {

        facetFactory.process(MyParseableUsingParserName.class, methodRemover, facetHolder);

        final ParseableFacetAbstract parseableFacet =
            (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertEquals(facetHolder, parseableFacet.getFacetHolder());
    }

    public void testNoMethodsRemoved() {

        facetFactory.process(MyParseableUsingParserName.class, methodRemover, facetHolder);

        assertNoMethodsRemoved();
    }

    @Parseable(parserName = "org.apache.isis.core.progmodel.facets.object.parseable.ParseableFacetFactoryTest$MyParseableUsingParserName")
    public static class MyParseableUsingParserName extends ParserNoop<MyParseableUsingParserName> {

        /**
         * Required since is a Parser.
         */
        public MyParseableUsingParserName() {
        }
    }

    public void testParseableUsingParserName() {

        facetFactory.process(MyParseableUsingParserName.class, methodRemover, facetHolder);

        final ParseableFacetAbstract parseableFacet =
            (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertEquals(MyParseableUsingParserName.class, parseableFacet.getParserClass());
    }

    public static class ParserNoop<T> implements Parser<T> {
        @Override
        public T parseTextEntry(final Object context, final String entry) {
            return null;
        }

        @Override
        public int typicalLength() {
            return 0;
        }

        @Override
        public String displayTitleOf(final T object) {
            return null;
        }

        @Override
        public String displayTitleOf(T object, String usingMask) {
            return null;
        }

        @Override
        public String parseableTitleOf(final T existing) {
            return null;
        }
    }

    @Parseable(parserClass = MyParseableUsingParserClass.class)
    public static class MyParseableUsingParserClass extends ParserNoop<MyParseableUsingParserClass> {
        /**
         * Required since is a Parser.
         */
        public MyParseableUsingParserClass() {
        }
    }

    public void testParseableUsingParserClass() {

        facetFactory.process(MyParseableUsingParserClass.class, methodRemover, facetHolder);

        final ParseableFacetAbstract parseableFacet =
            (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertEquals(MyParseableUsingParserClass.class, parseableFacet.getParserClass());
    }

    public void testParseableMustBeAParser() {
        // no test, because compiler prevents us from nominating a class that doesn't
        // implement Parser
    }

    @Parseable(parserClass = MyParseableWithoutNoArgConstructor.class)
    public static class MyParseableWithoutNoArgConstructor extends ParserNoop<MyParseableWithoutNoArgConstructor> {

        // no no-arg constructor

        public MyParseableWithoutNoArgConstructor(final int value) {
        }
    }

    public void testParseableHaveANoArgConstructor() {
        facetFactory.process(MyParseableWithoutNoArgConstructor.class, methodRemover, facetHolder);

        final ParseableFacetAbstract parseableFacet =
            (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertNull(parseableFacet);
    }

    @Parseable(parserClass = MyParseableWithoutPublicNoArgConstructor.class)
    public static class MyParseableWithoutPublicNoArgConstructor extends
        ParserNoop<MyParseableWithoutPublicNoArgConstructor> {

        // no public no-arg constructor
        MyParseableWithoutPublicNoArgConstructor() {
            this(0);
        }

        public MyParseableWithoutPublicNoArgConstructor(final int value) {
        }
    }

    public void testParseableHaveAPublicNoArgConstructor() {
        facetFactory.process(MyParseableWithoutPublicNoArgConstructor.class, methodRemover, facetHolder);

        final ParseableFacetAbstract parseableFacet =
            (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertNull(parseableFacet);
    }

    @Parseable()
    public static class MyParseableWithParserSpecifiedUsingConfiguration extends
        ParserNoop<MyParseableWithParserSpecifiedUsingConfiguration> {

        /**
         * Required since is a Parser.
         */
        public MyParseableWithParserSpecifiedUsingConfiguration() {
        }
    }

    public void testParserNameCanBePickedUpFromConfiguration() {
        final String className =
            "org.apache.isis.core.progmodel.facets.object.parseable.ParseableFacetFactoryTest$MyParseableWithParserSpecifiedUsingConfiguration";
        propertiesConfiguration.add(ParserUtil.PARSER_NAME_KEY_PREFIX + canonical(className)
            + ParserUtil.PARSER_NAME_KEY_SUFFIX, className);
        facetFactory.process(MyParseableWithParserSpecifiedUsingConfiguration.class, methodRemover, facetHolder);
        final ParseableFacetAbstract facet = (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertNotNull(facet);
        assertEquals(MyParseableWithParserSpecifiedUsingConfiguration.class, facet.getParserClass());
    }

    public static class NonAnnotatedParseableParserSpecifiedUsingConfiguration extends
        ParserNoop<NonAnnotatedParseableParserSpecifiedUsingConfiguration> {

        /**
         * Required since is a Parser.
         */
        public NonAnnotatedParseableParserSpecifiedUsingConfiguration() {
        }
    }

    public void testNonAnnotatedParseableCanPickUpParserFromConfiguration() {
        final String className =
            "org.apache.isis.core.progmodel.facets.object.parseable.ParseableFacetFactoryTest$NonAnnotatedParseableParserSpecifiedUsingConfiguration";
        propertiesConfiguration.add(ParserUtil.PARSER_NAME_KEY_PREFIX + canonical(className)
            + ParserUtil.PARSER_NAME_KEY_SUFFIX, className);
        facetFactory.process(NonAnnotatedParseableParserSpecifiedUsingConfiguration.class, methodRemover, facetHolder);
        final ParseableFacetAbstract facet = (ParseableFacetAbstract) facetHolder.getFacet(ParseableFacet.class);
        assertNotNull(facet);
        assertEquals(NonAnnotatedParseableParserSpecifiedUsingConfiguration.class, facet.getParserClass());
    }

    private String canonical(final String className) {
        return className.replace('$', '.');
    }

}
