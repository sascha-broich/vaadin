/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.tests.components.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.CheckBoxElement;
import com.vaadin.testbench.elements.TextFieldElement;
import com.vaadin.testbench.elements.WindowElement;
import com.vaadin.tests.tb3.MultiBrowserTest;

/**
 * Tests dialogs with WAI-ARIA.
 * 
 * @since
 * @author Vaadin Ltd
 */
public class ExtraWindowShownWaiAriaTest extends MultiBrowserTest {

    @Test
    public void testDialogs() throws InterruptedException {
        openTestURL();

        List<ButtonElement> buttons = $(ButtonElement.class).all();
        assertFalse(buttons.isEmpty());

        // open alert dialog
        buttons.get(0).click();

        // ensure dialog opened
        List<WindowElement> windows = $(WindowElement.class).all();
        assertFalse(windows.isEmpty());

        // ensure correct attributes
        assertEquals("alertdialog", windows.get(0).getAttribute("role"));

        WebElement header = windows.get(0).findElement(
                By.className("v-window-header"));
        assertEquals(header.getAttribute("id"),
                windows.get(0).getAttribute("aria-labelledby"));

        WebElement label = windows.get(0).findElement(By.className("v-label"));
        assertEquals(label.getAttribute("id"),
                windows.get(0).getAttribute("aria-describedby"));

        List<WebElement> wButtons = windows.get(0).findElements(
                By.className("v-button"));
        assertEquals("button", wButtons.get(0).getAttribute("role"));
        assertEquals("button", wButtons.get(1).getAttribute("role"));

        // close dialog
        wButtons.get(0).click();

        // ensure dialog closed
        windows = $(WindowElement.class).all();
        assertTrue(windows.isEmpty());

        // check additional description (second checkbox on the page)
        List<CheckBoxElement> checkBoxes = $(CheckBoxElement.class).all();
        WebElement input = checkBoxes.get(1).findElement(By.tagName("input"));
        // ensure that not checked yet
        assertEquals(null, input.getAttribute("checked"));
        input.click();
        // ensure that checked now
        assertEquals("true", input.getAttribute("checked"));

        // open alert dialog
        buttons = $(ButtonElement.class).all();
        buttons.get(0).click();

        // ensure correct attributes
        windows = $(WindowElement.class).all();
        List<WebElement> labels = windows.get(0).findElements(
                By.className("v-label"));
        assertEquals(labels.get(0).getAttribute("id") + " "
                + labels.get(1).getAttribute("id"), windows.get(0)
                .getAttribute("aria-describedby"));

        // close dialog
        wButtons = windows.get(0).findElements(By.className("v-button"));
        wButtons.get(0).click();

        // ensure dialog closed
        windows = $(WindowElement.class).all();
        assertTrue(windows.isEmpty());

        // add prefix and postfix
        List<TextFieldElement> textFields = $(TextFieldElement.class).all();
        textFields.get(0).sendKeys("Important");
        textFields.get(1).sendKeys(" - do ASAP");

        // open alert dialog
        buttons = $(ButtonElement.class).all();
        buttons.get(0).click();

        // ensure the assistive spans have been added to the header
        windows = $(WindowElement.class).all();
        header = windows.get(0).findElement(By.className("v-window-header"));
        List<WebElement> assistiveElements = header.findElements(By
                .className("v-assistive-device-only"));
        assertEquals("Important",
                assistiveElements.get(0).getAttribute("innerHTML"));
        assertEquals(" - do ASAP",
                assistiveElements.get(1).getAttribute("innerHTML"));
    }
}
