# SPRING-I18N-CONVERTER

Convert Spring `.properties` message bundles to and from JavaScript and Excel with Maven or Gradle.

Version `7.0.10` follows Spring Framework `7.0.10` and is tested with its `ReloadableResourceBundleMessageSource`. Spring is a test dependency; the converter does not require Spring at runtime. Java 17 or newer is required.

Supported conversions:

- `messages.properties` and `messages_<locale>.properties` to `default.js` and `<locale>.js`
- JavaScript locale files back to message bundles
- Message bundles to Excel `.xlsx` files
- Excel `.xlsx` or legacy `.xls` files back to message bundles

The JavaScript integration example is in [apply-converted-js](apply-converted-js).

## Maven

The core and Maven plugin are published on Maven Central:

- `org.tinywind:spring-i18n-converter:7.0.10`
- `org.tinywind:spring-i18n-converter-maven:7.0.10`

Add the plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.tinywind</groupId>
            <artifactId>spring-i18n-converter-maven</artifactId>
            <version>7.0.10</version>
            <executions>
                <execution>
                    <phase>generate-resources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <sources>
                    <source>
                        <converter>org.tinywind.springi18nconverter.converter.JavascriptConverter</converter>
                        <sourceDir>${project.basedir}/src/main/resources/i18n</sourceDir>
                        <targetDir>${project.basedir}/target/i18n/js</targetDir>
                        <describeByNative>true</describeByNative>
                    </source>
                </sources>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Run `mvn generate-resources`, or invoke the goal directly:

```sh
mvn org.tinywind:spring-i18n-converter-maven:7.0.10:generate
```

Set `<skip>true</skip>` in the plugin configuration to skip conversion.

## Gradle

The Gradle plugin is published on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.tinywind.spring-i18n-converter). Apply it in `build.gradle.kts`:

```kotlin
plugins {
    id("org.tinywind.spring-i18n-converter") version "7.0.10"
}

springI18n {
    configurationFile.set(layout.projectDirectory.file("configuration.xml"))
}
```

The default configuration file is `configuration.xml` in the project directory. The plugin reuses the converter's XML schema and resolves relative source and target directories against the Gradle project directory.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns="https://raw.githubusercontent.com/tinywind/SPRING-I18N-CONVERTER/master/spring-i18n-converter/src/main/resources/xsd/spring-i18n-converter-0.1.xsd">
    <sources>
        <source>
            <converter>org.tinywind.springi18nconverter.converter.JavascriptConverter</converter>
            <sourceDir>src/main/resources/i18n</sourceDir>
            <targetDir>build/i18n/js</targetDir>
            <describeByNative>true</describeByNative>
        </source>
    </sources>
</configuration>
```

Run the task:

```sh
./gradlew convertI18n
```

Set `springI18n.skip.set(true)` to skip conversion. The task runs when requested and does not declare incremental outputs because configuration files may select arbitrary source and target directories.

## Conversion options

Each `<source>` supports the following fields in Maven configuration or the standalone XML file:

| Field | Default | Purpose |
| --- | --- | --- |
| `converter` | `org.tinywind.springi18nconverter.converter.JavascriptConverter` | Use `JavascriptConverter` or `ExcelConverter` in that package. |
| `sourceDir` | Required | Directory containing the input files. |
| `targetDir` | Empty | Output directory; an empty value uses the working directory (the project directory for Gradle). |
| `toMessageProperties` | `false` | Set `true` to convert JavaScript or Excel back to `.properties`. |
| `sourceEncoding` | `UTF-8` | Text input encoding. |
| `targetEncoding` | `UTF-8` | Text output encoding. |
| `overwrite` | `true` | Set `false` to preserve existing output files. Missing outputs are still created. |
| `describeByNative` | `false` | Preserve native Unicode characters in text output instead of Unicode escapes. |
| `excludes` | Empty | Reserved configuration field; currently unused. |

To export Excel, use `org.tinywind.springi18nconverter.converter.ExcelConverter` and a separate `targetDir`. To import JavaScript or Excel, set `toMessageProperties` to `true` and point `sourceDir` at the exported files. Conversion failures fail the Maven goal or Gradle task.

## Build and test

The Maven reactor builds the core and Maven plugin. Use Maven 3.10.0 or newer:

```sh
mvn clean verify
```

The Gradle wrapper builds and tests the Gradle adapter. The core version must already be available on Maven Central, or supplied through an initialization script for a local candidate:

```sh
./gradlew --project-cache-dir .tmp/gradle-project-cache :spring-i18n-converter-gradle:check :spring-i18n-converter-gradle:assemble
```

Tests cover both conversion directions, Unicode and escaping, XML validation, overwrite behavior, plugin failure propagation, and Spring Framework message loading. CI installs the current core into an isolated Maven repository before running Gradle TestKit against it.

The Maven `release` profile attaches source and Javadoc archives, signs artifacts with GPG, and publishes through the Maven Central Portal. Gradle publication uses `:spring-i18n-converter-gradle:publishPlugins` and Gradle Portal publishing credentials. Credentials belong in environment variables or local credential storage.

## License

[MIT](LICENSE)
