package com.anotherservice.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by jrichard on 24/04/2015.
 */
public class AnyTest {

    @Test
    public void testSimpleString() {
        String simple = "SimpleString";
        Any simpleString = Any.wrap(simple);
        assertThat(simpleString).isNotNull();
        assertThat(simpleString.isNull()).isFalse();
        assertThat(simple).isEqualTo(simple);
    }

    @Test
    public void testSimple() {
        Any nullWrap = Any.wrap(null);
        assertThat(nullWrap.isNull()).isTrue();
    }
}
