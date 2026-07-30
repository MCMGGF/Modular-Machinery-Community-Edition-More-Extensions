package com.fushu.mmceguiext.common.integration.crafttweaker;

import crafttweaker.util.IEventHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MMCEGEEventsTest {

    @Before
    public void setUp() {
        MMCEGEEvents.clearButtonClickHandlers();
    }

    @After
    public void tearDown() {
        MMCEGEEvents.clearButtonClickHandlers();
    }

    @Test
    public void shortMachineNameDispatchesHandlerOnce() {
        IEventHandler<ControllerButtonClickEvent> handler = handler();
        MMCEGEEvents.onControllerButtonClick("blood_altar", handler);

        List<IEventHandler<ControllerButtonClickEvent>> handlers =
            MMCEGEEvents.collectHandlersForMachineKeys("modularmachinery:blood_altar", "blood_altar");

        assertEquals(1, handlers.size());
        assertSame(handler, handlers.get(0));
    }

    @Test
    public void fullMachineNameDispatchesHandlerOnce() {
        IEventHandler<ControllerButtonClickEvent> handler = handler();
        MMCEGEEvents.onControllerButtonClick("modularmachinery:blood_altar", handler);

        List<IEventHandler<ControllerButtonClickEvent>> handlers =
            MMCEGEEvents.collectHandlersForMachineKeys("modularmachinery:blood_altar", "blood_altar");

        assertEquals(1, handlers.size());
        assertSame(handler, handlers.get(0));
    }

    @Test
    public void sameHandlerRegisteredWithBothAliasesDispatchesOnce() {
        IEventHandler<ControllerButtonClickEvent> handler = handler();
        MMCEGEEvents.onControllerButtonClick("blood_altar", handler);
        MMCEGEEvents.onControllerButtonClick("modularmachinery:blood_altar", handler);

        List<IEventHandler<ControllerButtonClickEvent>> handlers =
            MMCEGEEvents.collectHandlersForMachineKeys("modularmachinery:blood_altar", "blood_altar");

        assertEquals(1, handlers.size());
        assertSame(handler, handlers.get(0));
    }

    @Test
    public void differentAliasHandlersEachDispatchOnce() {
        IEventHandler<ControllerButtonClickEvent> shortHandler = handler();
        IEventHandler<ControllerButtonClickEvent> fullHandler = handler();
        MMCEGEEvents.onControllerButtonClick("blood_altar", shortHandler);
        MMCEGEEvents.onControllerButtonClick("modularmachinery:blood_altar", fullHandler);

        List<IEventHandler<ControllerButtonClickEvent>> handlers =
            MMCEGEEvents.collectHandlersForMachineKeys("modularmachinery:blood_altar", "blood_altar");

        assertEquals(2, handlers.size());
        assertTrue(handlers.contains(shortHandler));
        assertTrue(handlers.contains(fullHandler));
    }

    @Test
    public void duplicateRegistrationOnSameKeyDispatchesOnce() {
        IEventHandler<ControllerButtonClickEvent> handler = handler();
        MMCEGEEvents.onControllerButtonClick("blood_altar", handler);
        MMCEGEEvents.onControllerButtonClick("blood_altar", handler);

        List<IEventHandler<ControllerButtonClickEvent>> handlers =
            MMCEGEEvents.collectHandlersForMachineKeys("modularmachinery:blood_altar", "blood_altar");

        assertEquals(1, handlers.size());
    }

    private static IEventHandler<ControllerButtonClickEvent> handler() {
        return new IEventHandler<ControllerButtonClickEvent>() {
            @Override
            public void handle(ControllerButtonClickEvent event) {
            }
        };
    }
}
