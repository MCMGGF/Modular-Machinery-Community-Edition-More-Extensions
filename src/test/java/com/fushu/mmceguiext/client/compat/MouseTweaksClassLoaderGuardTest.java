package com.fushu.mmceguiext.client.compat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MouseTweaksClassLoaderGuardTest {
    @Test
    public void officialMouseTweaksClassesCanBePreloadedWithoutAMixin() {
        List<String> failures = MouseTweaksClassLoaderGuard.preloadClasses(
            getClass().getClassLoader(),
            MouseTweaksClassLoaderGuard.PRELOAD_CLASSES
        );

        assertTrue(failures.toString(), failures.isEmpty());
    }

    @Test
    public void cleanroomInvalidAndNegativeEntriesAreClearedBeforePreload() {
        RecoverableClassLoader loader = new RecoverableClassLoader(getClass().getClassLoader());
        loader.invalidClasses.add("java.lang.String");
        loader.negativeEntries.add("java.lang.String");

        List<String> failures = MouseTweaksClassLoaderGuard.preloadClasses(
            loader,
            Collections.singletonList("java.lang.String")
        );

        assertTrue(failures.isEmpty());
        assertFalse(loader.invalidClasses.contains("java.lang.String"));
        assertFalse(loader.negativeEntries.contains("java.lang.String"));
    }

    @Test
    public void genuinelyMissingClassesAreReportedWithoutThrowing() {
        List<String> failures = MouseTweaksClassLoaderGuard.preloadClasses(
            getClass().getClassLoader(),
            Arrays.asList("", "missing.mmceguiext.MouseTweaksClass")
        );

        assertEquals(
            Collections.singletonList("missing.mmceguiext.MouseTweaksClass"),
            failures
        );
    }

    private static final class RecoverableClassLoader extends ClassLoader {
        private final Set<String> invalidClasses = new HashSet<String>();
        private final Set<String> negativeEntries = new HashSet<String>();

        private RecoverableClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Set<String> getInvalidClasses() {
            return this.invalidClasses;
        }

        public void clearNegativeEntries(Set<String> entries) {
            this.negativeEntries.removeAll(entries);
        }
    }
}
