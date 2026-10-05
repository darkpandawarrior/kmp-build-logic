/**
 * The Android SDK levels every module in the family compiles and ships against.
 *
 * Shared rather than repeated per convention plugin: an app built against a different `compileSdk`
 * than the libraries it links is how "works in the app module, fails in a library" bugs start.
 * PascalCase per the family's `naming.TopLevelPropertyNaming` standard (Compose API guidelines).
 */
internal const val CompileSdk = 37

/** Lowest Android version the family supports. Raising it drops devices — a product decision. */
internal const val MinSdk = 24

/** JDK level every KMP module compiles with (class file 69). Matches the daemon JVM and CI. */
internal const val JavaToolchain = 25
