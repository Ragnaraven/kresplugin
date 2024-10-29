# KRes - Kotlin Resource Generator Plugin

KRes is a Gradle plugin for Kotlin Multiplatform projects that generates type-safe resource accessors for strings and files. It helps manage localized strings and file resources in a clean, type-safe way. 

## Features

- Type-safe string resource generation with support for:
  - Multiple languages/locales with fallback support
  - String interpolation with ${variable} syntax
  - Nested string hierarchies
- Type-safe file resource path generation
- Automatic resource cleanup and regeneration during builds
- Configurable output location and package name
- Seamless integration with Kotlin Multiplatform projects

## Installation

Add to your project's buildscript:

```kotlin
plugins {
    id("io.kresplugin") version "1.0.0"
}
```

## Usage

### Basic Setup

1. Configure the plugin in your `build.gradle.kts`:

```kotlin
kres {
    kresPackage = "com.example.resources"  // Required: Package name for generated code
    krName = "KR"                          // Optional: Name of generated object (default: KR)
    stringsRoot = "strings"                // Optional: Root name for strings file (default: strings)
    disableFileResourceGeneration = false  // Optional: Disable file resource generation
}
```

2. Create your strings resource file at `src/commonMain/resources/strings.json`:

```json
{
    "greeting": {
        "en": "Hello, ${name}!",
        "es": "¡Hola, ${name}!"
    },
    "app": {
        "title": {
            "en": "My App",
            "es": "Mi Aplicación"
        },
        "description": {
            "en": "A great application",
            "es": "Una gran aplicación"
        }
    }
}
```

3. Place any resource files you want to reference in `src/commonMain/resources/`

### Generated Code Usage

```kotlin
// Access string resources with interpolation
val greeting = KR.strings.greeting("John")

// Access nested strings
val appTitle = KR.strings.app.title()

// Access file paths (if file resource generation is enabled)
val imagePath = KR.images.logo_png
```

### Language Selection

The generated code includes a mutable `currentLang` property that defaults to "en":

```kotlin
KR.strings.currentLang = "es" // Switch to Spanish
val greeting = KR.strings.greeting("Juan") // ¡Hola, Juan!
```

## Configuration Options

| Option | Type | Description | Default |
|--------|------|-------------|---------|
| kresPackage | String | Package name for generated code | Required |
| krName | String | Name of the generated object | "KR" |
| stringsRoot | String | Name of the strings resource file | "strings" |
| generatedFileDir | String | Directory for generated files | "generated/kres" |
| disableFileResourceGeneration | Boolean | Disable file resource generation | false |
| resourcesRoot | File? | Custom resources root directory | null |

## Gradle Tasks

- `kresgenerate` - Generates all resource files
- `kresclean` - Cleans generated resource files

The plugin automatically runs during the build process, ensuring resources are always up-to-date.