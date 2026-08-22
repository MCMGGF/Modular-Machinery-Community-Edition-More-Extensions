plugins {
    id("java-library")
    id("maven-publish")
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.9"
}

group = "com.fushu.mmce"
version = "1.4.1"

java {
    withSourcesJar()
}

minecraft {
    mcVersion.set("1.12.2")
    username.set("Developer")
    injectedTags.put("VERSION", project.version.toString())

    val args = mutableListOf("-ea:${project.group}")
    args.add("-Dfml.coreMods.load=com.fushu.mmceguiext.core.MMCEGuiExtEarlyMixinLoader")
    args.add("-Dmixin.hotSwap=true")
    args.add("-Dmixin.checks.interfaces=true")
    args.add("-Dmixin.debug.export=true")
    extraRunJvmArguments.addAll(args)
}

val curseMavenMirrorPath = providers.environmentVariable("MMCEGE_CURSEMAVEN_MIRROR_PATH").orNull

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.spongepowered.org/maven")
    }
    maven {
        name = "CleanroomMC"
        url = uri("https://maven.cleanroommc.com")
    }
    maven {
        name = "BlameJared Maven"
        url = uri("https://maven.blamejared.com/")
    }
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    }
}

dependencies {
    val deobfCurseDependency: (String) -> Any = { notation ->
        if (!curseMavenMirrorPath.isNullOrBlank()) {
            val parts = notation.split(':')
            require(parts.size == 3 && parts[0] == "curse.maven") {
                "Invalid CurseMaven dependency notation: $notation"
            }
            val artifact = parts[1]
            val version = parts[2]
            val jar = file(curseMavenMirrorPath)
                .resolve("curse/maven/$artifact/$version/$artifact-$version.jar")
            check(jar.isFile) {
                "Locked CurseMaven dependency is missing: $jar"
            }
            rfg.deobf(files(jar))
        } else {
            rfg.deobf(notation)
        }
    }

    patchedMinecraft("me.eigenraven.java8unsupported:java-8-unsupported-shim:1.0.0")

    val mixin = modUtils.enableMixins("zone.rong:mixinbooter:8.9", "mixins.mmceguiext.refmap.json").toString()
    val localMixinBooter = fileTree("${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1/zone.rong/mixinbooter/8.9") {
        include("**/mixinbooter-8.9.jar")
    }.files.firstOrNull()
    api(mixin) {
        isTransitive = false
    }
    annotationProcessor("org.ow2.asm:asm-debug-all:5.2")
    annotationProcessor("com.google.guava:guava:30.0-jre")
    annotationProcessor("com.google.code.gson:gson:2.8.9")
    if (localMixinBooter != null) {
        annotationProcessor(files(localMixinBooter))
    } else {
        annotationProcessor(mixin) {
            isTransitive = false
        }
    }

    implementation(deobfCurseDependency("curse.maven:modular-machinery-community-edition-817377:7372953"))
    implementation("com.google.code.gson:gson:2.8.9")

    compileOnly("CraftTweaker2:CraftTweaker2-API:4.1.20.715")
    compileOnly("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.1.20.715")
    testCompileOnly("CraftTweaker2:CraftTweaker2-API:4.1.20.715")
    testCompileOnly("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.1.20.715")
    testRuntimeOnly("CraftTweaker2:CraftTweaker2-API:4.1.20.715")
    testRuntimeOnly("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.1.20.715")
    val craftTweakerModsDir = System.getenv("MMCEGE_CRAFTTWEAKER_MODS_DIR")
    val localCraftTweakerJars = if (!craftTweakerModsDir.isNullOrBlank()) {
        fileTree(craftTweakerModsDir) {
            include("*CraftTweaker2*.jar")
            include("*CraftTweaker*.jar")
            include("*ZenScript*.jar")
        }.files
    } else emptySet<File>()
    if (localCraftTweakerJars.isNotEmpty()) {
        compileOnly(files(localCraftTweakerJars))
    }
    compileOnly(deobfCurseDependency("curse.maven:the-one-probe-245211:2667280"))
    testCompileOnly(deobfCurseDependency("curse.maven:the-one-probe-245211:2667280"))
    testRuntimeOnly(deobfCurseDependency("curse.maven:the-one-probe-245211:2667280"))
    compileOnly(deobfCurseDependency("curse.maven:ae2-extended-life-570458:6302098"))
    compileOnly(deobfCurseDependency("curse.maven:ae2-fluid-crafting-rework-623955:5504001"))
    testImplementation(deobfCurseDependency("curse.maven:ae2-extended-life-570458:6302098"))
    compileOnly(deobfCurseDependency("curse.maven:mouse-tweaks-unofficial-461660:5876158"))
    testCompileOnly(deobfCurseDependency("curse.maven:mouse-tweaks-unofficial-461660:5876158"))
    testRuntimeOnly(deobfCurseDependency("curse.maven:mouse-tweaks-unofficial-461660:5876158"))
    compileOnly(deobfCurseDependency("curse.maven:Mekanism-268560:2835175"))
    testCompileOnly(deobfCurseDependency("curse.maven:Mekanism-268560:2835175"))
    testRuntimeOnly(deobfCurseDependency("curse.maven:Mekanism-268560:2835175"))
    compileOnly(deobfCurseDependency("curse.maven:mekanism-energistics-1027681:5408319"))
    compileOnly(deobfCurseDependency("curse.maven:had-enough-items-557549:4810661"))
    val localGregTech = if (curseMavenMirrorPath.isNullOrBlank()) {
        fileTree("${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1/curse.maven/gregtech-ce-unofficial-557242/5322654") {
            include("**/gregtech-ce-unofficial-557242-5322654.jar")
        }.files.firstOrNull()
    } else null
    if (localGregTech != null) {
        compileOnly(files(localGregTech))
        testCompileOnly(files(localGregTech))
        testRuntimeOnly(files(localGregTech))
    } else {
        compileOnly(deobfCurseDependency("curse.maven:gregtech-ce-unofficial-557242:5322654"))
        testCompileOnly(deobfCurseDependency("curse.maven:gregtech-ce-unofficial-557242:5322654"))
        testRuntimeOnly(deobfCurseDependency("curse.maven:gregtech-ce-unofficial-557242:5322654"))
    }
    // Optional Draconic Evolution / BrandonsCore integration. The 1.12.2 jars on this branch
    // expose int RF/FE only; TileCustomHatch probes newer OP long APIs reflectively if present.
    compileOnly(deobfCurseDependency("curse.maven:brandonscore-231382:3051539"))
    compileOnly(deobfCurseDependency("curse.maven:draconicevolution-223565:3051542"))
    testCompileOnly(deobfCurseDependency("curse.maven:brandonscore-231382:3051539"))
    testCompileOnly(deobfCurseDependency("curse.maven:draconicevolution-223565:3051542"))
    testRuntimeOnly(deobfCurseDependency("curse.maven:brandonscore-231382:3051539"))
    testRuntimeOnly(deobfCurseDependency("curse.maven:draconicevolution-223565:3051542"))
    compileOnly("software.bernie.geckolib:geckolib-forge-1.12.2:3.0.31")
    testImplementation("software.bernie.geckolib:geckolib-forge-1.12.2:3.0.31")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.ow2.asm:asm:5.2")
}

tasks.processResources.configure {
    inputs.property("version", project.version)
    inputs.property("mcversion", minecraft.mcVersion.get())

    filesMatching("mcmod.info") {
        expand(
            mapOf(
                "version" to project.version,
                "mcversion" to minecraft.mcVersion.get()
            )
        )
    }
}

tasks.compileJava.configure {
    sourceCompatibility = "1.8"
    options.encoding = "UTF-8"
    targetCompatibility = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.jar.configure {
    manifest {
        val attributes = manifest.attributes
        attributes["FMLCorePlugin"] = "com.fushu.mmceguiext.core.MMCEGuiExtEarlyMixinLoader"
        attributes["FMLCorePluginContainsFMLMod"] = true
    }
}

val verifyJarDoesNotBundleMouseTweaks by tasks.registering {
    group = "verification"
    description = "Fails when the release jar bundles or patches Mouse Tweaks classes."
    dependsOn(tasks.jar)

    doLast {
        val outputJar = tasks.jar.get().archiveFile.get().asFile
        val bundledEntries = zipTree(outputJar).matching {
            include("yalter/mousetweaks/**")
        }.files
        check(bundledEntries.isEmpty()) {
            "MMCEME must not bundle Mouse Tweaks classes: " +
                bundledEntries.joinToString { it.name }
        }

        val obsoleteMixinEntries = zipTree(outputJar).matching {
            include("mixins.mmceguiext.mousetweaks.json")
            include("com/fushu/mmceguiext/mixin/MixinMouseTweaksMain.class")
        }.files
        check(obsoleteMixinEntries.isEmpty()) {
            "MMCEME must not transform Mouse Tweaks classes: " +
                obsoleteMixinEntries.joinToString { it.name }
        }
    }
}

tasks.check.configure {
    dependsOn(verifyJarDoesNotBundleMouseTweaks)
}
