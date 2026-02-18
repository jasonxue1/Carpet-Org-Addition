import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val mcVersion = (project.extra["mcVersion"] as Number).toInt()
val unobfuscated = mcVersion >= 26_00_00

apply(plugin = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")
apply(plugin = "com.replaymod.preprocess")

val minecraftVersion: String by project
val parchmentVersion: String by project
val loaderVersion: String by project
val fabricApiVersion: String by project
val carpetVersion: String by project
val mixinextrasVersion: String by project
val modVersion: String by project
val modId: String by project
val modName: String by project
val modDescription: String by project
val modSource: String by project
val minecraftDependency: String by project
val fabricloaderDependency: String by project
val mavenGroup: String by project
val archivesBaseName: String by project
val issueTrackerUrl: String = "$modSource/issues"

repositories {
    maven {
        url = uri("https://maven.parchmentmc.org")
        content { includeGroup("org.parchmentmc.data") }
    }
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github")
            includeGroupByRegex("com\\.github\\..+")
        }
    }
    maven {
        url = uri("https://maven.fallenbreath.me/releases")
        content { includeGroup("me.fallenbreath") }
    }
    maven {
        url = uri("https://masa.dy.fi/maven")
        content { includeGroup("carpet") }
    }
}

extensions.getByType(SourceSetContainer::class).named("main") {
    java.srcDirs(rootProject.file("src/main/java"))
    resources.srcDirs(rootProject.file("src/main/resources"))
}

val loomExtension = extensions.getByType(LoomGradleExtensionAPI::class)
val fabricApiExtension = extensions.getByType(FabricApiExtension::class)

dependencies {
    fun processDependency(dep: Dependency?): Dependency? {
        // https://github.com/FabricMC/fabric-loader/issues/783
        if (dep is ModuleDependency && !(dep.group == "net.fabricmc" && dep.name == "fabric-loader")) {
            dep.exclude(mapOf("group" to "net.fabricmc", "module" to "fabric-loader"))
        }
        return dep
    }

    fun autoImplementation(dep: Any): Dependency? = processDependency(add(if (unobfuscated) "implementation" else "modImplementation", dep))

    fun autoRuntimeOnly(dep: Any): Dependency? = processDependency(add(if (unobfuscated) "runtimeOnly" else "modRuntimeOnly", dep))

    fun autoCompileOnly(dep: Any): Dependency? = processDependency(add(if (unobfuscated) "compileOnly" else "modCompileOnly", dep))

    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    if (!unobfuscated) {
        @Suppress("UnstableApiUsage")
        add(
            "mappings",
            loomExtension.layered {
                officialMojangMappings()
                if (parchmentVersion.isNotEmpty()) {
                    parchment("org.parchmentmc.data:parchment-$minecraftVersion:$parchmentVersion@zip")
                }
            },
        )
    }

    autoImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    autoImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    autoImplementation("carpet:fabric-carpet:$carpetVersion")

    val mixinExtrasDep = autoImplementation("io.github.llamalad7:mixinextras-fabric:$mixinextrasVersion")
    if (mixinExtrasDep != null) {
        add("include", mixinExtrasDep)
    }
    add("annotationProcessor", "io.github.llamalad7:mixinextras-fabric:$mixinextrasVersion")

    autoCompileOnly("org.jspecify:jspecify:1.0.0")
    autoRuntimeOnly("me.fallenbreath:mixin-auditor:0.2.0-${if (unobfuscated) "u" else "o"}")
}

val mixinConfigPath = "carpet-org-addition.mixins.json"
val javaCompatibility =
    when {
        mcVersion >= 26_00_00 -> JavaVersion.VERSION_25
        mcVersion >= 12005 -> JavaVersion.VERSION_21
        mcVersion >= 11800 -> JavaVersion.VERSION_17
        mcVersion >= 11700 -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }
val mixinCompatibilityLevel = javaCompatibility

val commonVmArgs = listOf("-Dmixin.debug.export=true", "-Dmixin.debug.countInjections=true")
loomExtension.runConfigs.configureEach {
    // to make sure it generates all "Minecraft Client (:subproject_name)" applications
    isIdeConfigGenerated = true
    runDir = "../../run"
    vmArgs(commonVmArgs)
}
loomExtension.runs {
    named("client") {
        programArgs("--username", "Player789")
        vmArgs(
            "-XX:+EnableDynamicAgentLoading",
            "--enable-native-access=ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
            "-DMC_DEBUG_ENABLED=true",
            "-DMC_DEBUG_VERBOSE_COMMAND_ERRORS=true",
            "-Dmixin.debug.export=true",
        )
    }
    val auditVmArgs = commonVmArgs + "-DmixinAuditor.audit=true"
    register("serverMixinAudit") {
        server()
        vmArgs.addAll(auditVmArgs)
        isIdeConfigGenerated = false
    }
    register("clientMixinAudit") {
        client()
        vmArgs.addAll(auditVmArgs)
        isIdeConfigGenerated = false
    }
}
loomExtension.accessWidenerPath.set(file("carpet-org-addition.accesswidener"))
fabricApiExtension.configureDataGeneration {
    client.set(true)
}

var modVersionSuffix = ""
if (System.getenv("BUILD_RELEASE") != "true") {
    val buildNumber = System.getenv("BUILD_ID")
    modVersionSuffix += if (buildNumber != null) "+build.$buildNumber" else "-SNAPSHOT"
}
val fullModVersion = modVersion + modVersionSuffix
val timeStamp = DateTimeFormatter.ofPattern("yyMMddHHmm").format(LocalDateTime.now())

group = mavenGroup
val baseExtension = extensions.getByType(BasePluginExtension::class)
baseExtension.archivesName.set(archivesBaseName)
version = "v$modVersion-mc$minecraftVersion$modVersionSuffix"

val modProperties =
    mapOf(
        "id" to modId,
        "name" to modName,
        "version" to fullModVersion,
        "description" to modDescription,
        "source" to modSource,
        "issues" to issueTrackerUrl,
        "minecraft_dependency" to minecraftDependency,
        "fabricloader_dependency" to fabricloaderDependency,
    )

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    inputs.properties(modProperties)
    inputs.property("buildTimestamp", timeStamp)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(modProperties + mapOf("buildTimestamp" to timeStamp))
    }

    filesMatching(mixinConfigPath) {
        filter { line: String ->
            line.replace("{{COMPATIBILITY_LEVEL}}", "JAVA_${mixinCompatibilityLevel.ordinal + 1}")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    options.release = javaCompatibility.majorVersion.toInt()
    if (javaCompatibility <= JavaVersion.VERSION_1_8) {
        options.compilerArgs.add("-Xlint:-options")
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test>().configureEach {
    enabled = false
}

extensions.getByType(JavaPluginExtension::class).apply {
    sourceCompatibility = javaCompatibility
    targetCompatibility = javaCompatibility

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task.
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    inputs.property("archives_base_name", archivesBaseName)
    from(rootProject.file("LICENSE")) {
        rename { name -> "${name}_${inputs.properties["archives_base_name"]}" }
    }
}
