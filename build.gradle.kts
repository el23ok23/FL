// Los plugins se aplican en cada módulo (ver app/ y core/).
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
