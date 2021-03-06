/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class NameBindingTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, FooResource.class, BarResource.class, FooBarResource.class, FooFilter.class,
                BarFilter.class, FooBarFilter.class);
    }

    @Path("resource")
    public static class Resource {
        @GET
        public String noBinding() {
            return "noBinding";
        }

        @GET
        @FooBinding
        @Path("foo")
        public String foo() {
            return "foo";
        }

        @GET
        @BarBinding
        @Path("bar")
        public String bar() {
            return "bar";
        }

        @GET
        @FooBinding
        @BarBinding
        @Path("foobar")
        public String foobar() {
            return "foobar";
        }

    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface FooBinding {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface BarBinding {
    }


    @FooBinding
    public static class FooFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add(this.getClass().getSimpleName(), "called");
        }
    }

    @BarBinding
    public static class BarFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add(this.getClass().getSimpleName(), "called");
        }
    }

    @FooBinding
    @BarBinding
    public static class FooBarFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add(this.getClass().getSimpleName(), "called");
        }
    }


    private static final Set<Class<?>> FILTERS = initialize();

    private static Set<Class<?>> initialize() {
        final Set<Class<?>> set = Sets.newHashSet();
        set.add(FooFilter.class);
        set.add(BarFilter.class);
        set.add(FooBarFilter.class);
        return set;
    }


    private void checkCalled(Response response, Class<?>... filtersThatShouldBeCalled) {
        Set<Class<?>> positiveFilters = Sets.newHashSet(filtersThatShouldBeCalled);
        for (Class<?> filter : FILTERS) {
            if (positiveFilters.contains(filter)) {
                assertEquals("Filter '" + filter.getSimpleName() + "' should be called.",
                        "called", response.getHeaders().getFirst(filter.getSimpleName()));
            } else {
                assertNull("Filter '" + filter.getSimpleName() + "' should not be called.",
                        response.getHeaders().get(filter.getSimpleName()));
            }
        }
    }


    private Response _getResponse(String path) {
        final Response response = target().path(path).request().get();
        assertEquals(200, response.getStatus());
        return response;
    }

    @Test
    public void testResourceNoBinding() {
        checkCalled(_getResponse("resource"));
    }

    @Test
    public void testResourceFooBinding() {
        checkCalled(_getResponse("resource/foo"), FooFilter.class);
    }

    @Test
    public void testResourceBarBinding() {
        checkCalled(_getResponse("resource/bar"), BarFilter.class);
    }

    @Test
    public void testResourceFooBarBinding() {
        checkCalled(_getResponse("resource/foobar"), FooFilter.class, BarFilter.class, FooBarFilter.class);
    }


    @Path("foo-resource")
    @FooBinding
    public static class FooResource extends Resource {
    }

    @Test
    public void testFooResourceNoBinding() {
        checkCalled(_getResponse("foo-resource"), FooFilter.class);
    }

    @Test
    public void testFooResourceFooBinding() {
        checkCalled(_getResponse("foo-resource/foo"), FooFilter.class);
    }

    @Test
    public void testFooResourceBarBinding() {
        checkCalled(_getResponse("foo-resource/bar"), FooFilter.class, BarFilter.class, FooBarFilter.class);
    }

    @Test
    public void testFooResourceFooBarBinding() {
        checkCalled(_getResponse("foo-resource/foobar"), FooFilter.class, BarFilter.class, FooBarFilter.class);
    }

    @Path("bar-resource")
    @BarBinding
    public static class BarResource extends Resource {
    }

    @Test
    public void testBarResourceNoBinding() {
        checkCalled(_getResponse("bar-resource"), BarFilter.class);
    }

    @Test
    public void testBarResourceFooBinding() {
        checkCalled(_getResponse("bar-resource/foo"), BarFilter.class, FooFilter.class, FooBarFilter.class);
    }

    @Test
    public void testBarResourceBarBinding() {
        checkCalled(_getResponse("bar-resource/bar"), BarFilter.class);
    }

    @Test
    public void testBarResourceFooBarBinding() {
        checkCalled(_getResponse("bar-resource/foobar"), BarFilter.class, FooFilter.class, FooBarFilter.class);
    }


    @Path("foobar-resource")
    @BarBinding
    @FooBinding
    public static class FooBarResource extends Resource {
    }

    @Test
    public void testFooBarResourceNoBinding() {
        checkCalled(_getResponse("foobar-resource"), BarFilter.class, FooFilter.class, FooBarFilter.class);
    }

    @Test
    public void testFooBarResourceFooBinding() {
        checkCalled(_getResponse("foobar-resource/foo"), BarFilter.class, FooFilter.class, FooBarFilter.class);
    }

    @Test
    public void testFooBarResourceBarBinding() {
        checkCalled(_getResponse("foobar-resource/bar"), BarFilter.class, FooFilter.class, FooBarFilter.class);
    }

    @Test
    public void testFooBarResourceFooBarBinding() {
        checkCalled(_getResponse("foobar-resource/foobar"), BarFilter.class, FooFilter.class, FooBarFilter.class);
    }


}
