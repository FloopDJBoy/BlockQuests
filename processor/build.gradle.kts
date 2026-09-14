plugins {
    id("java-library")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies{
    implementation("com.google.auto.service:auto-service:1.1.1")
}
