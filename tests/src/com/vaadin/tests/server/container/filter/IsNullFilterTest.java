package com.vaadin.tests.server.container.filter;

import junit.framework.Assert;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.IsNull;

public class IsNullFilterTest extends AbstractFilterTest {

    public void testIsNull() {
        Item item1 = new PropertysetItem();
        item1.addItemProperty("a", new ObjectProperty<String>(null,
                String.class));
        item1.addItemProperty("b",
                new ObjectProperty<String>("b", String.class));
        Item item2 = new PropertysetItem();
        item2.addItemProperty("a",
                new ObjectProperty<String>("a", String.class));
        item2.addItemProperty("b", new ObjectProperty<String>(null,
                String.class));

        Filter filter1 = new IsNull("a");
        Filter filter2 = new IsNull("b");

        Assert.assertTrue(filter1.passesFilter(null, item1));
        Assert.assertFalse(filter1.passesFilter(null, item2));
        Assert.assertFalse(filter2.passesFilter(null, item1));
        Assert.assertTrue(filter2.passesFilter(null, item2));
    }

    public void testIsNullEqualsHashCode() {
        Filter filter1 = new IsNull("a");
        Filter filter1b = new IsNull("a");
        Filter filter2 = new IsNull("b");

        // equals()
        Assert.assertEquals(filter1, filter1b);
        Assert.assertFalse(filter1.equals(filter2));
        Assert.assertFalse(filter1.equals(new And()));

        // hashCode()
        Assert.assertEquals(filter1.hashCode(), filter1b.hashCode());
    }

}