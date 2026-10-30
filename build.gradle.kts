plugins {
    base
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
    layout.buildDirectory = rootProject.layout.projectDirectory.dir(".tmp/gradle-build/${project.name}")
}
