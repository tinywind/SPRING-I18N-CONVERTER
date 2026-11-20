# Security Policy

## Supported Versions

Security fixes are applied on `main` and released on the current `7.0.x` release line across all modules published from this repository (`spring-i18n-converter`, `spring-i18n-converter-maven`, and the Gradle plugin `org.tinywind.spring-i18n-converter`). Older releases do not receive security backports.

| Version | Supported |
| ------- | --------- |
| `7.0.x` (latest release / `main`) | Yes |
| `< 7.0.0` (legacy `0.1.x` releases) | No |

Supported runtime and build environments are Java 17+, Maven 3.10+, and Gradle 9.x.

## Reporting a Vulnerability

Please do **not** report undisclosed security vulnerabilities through public GitHub issues, pull requests, or discussions.

Use GitHub's [private vulnerability reporting](https://github.com/tinywind/SPRING-I18N-CONVERTER/security/advisories/new) for this repository to submit a report confidentially. If you cannot use GitHub Security Advisories, contact the maintainer directly at `tinywind0@gmail.com`.

When reporting an issue, please include as much of the following as possible:

- Affected module (`spring-i18n-converter`, `spring-i18n-converter-maven`, or `org.tinywind.spring-i18n-converter`) and version
- Java, Maven, or Gradle version used
- Affected component (for example: XML configuration parsing and JAXB unmarshalling, `JavascriptConverter`, `ExcelConverter`, `apply-converted-js/i18n.js`, or output file path handling)
- A minimal reproduction or proof of concept (without real secrets or confidential message bundles)
- Description of the potential impact and any suggested mitigation

## Response Process

- We aim to acknowledge receipt of a vulnerability report within 7 days.
- Validated issues are tracked privately through a GitHub Security Advisory while a fix is prepared and verified.
- Once a patched release is available, we will publish the advisory and credit reporters who wish to be acknowledged.

## Scope and Security Model Notes

`SPRING-I18N-CONVERTER` runs at build time via its CLI (`Launcher`), Maven plugin, or Gradle plugin to convert Spring `.properties` message bundles to and from JavaScript and Excel (`.xlsx`/`.xls`).

### In scope

- Unsafe XML or XSD configuration parsing in `Launcher.load` or the Maven and Gradle entry points
- Output escaping or JavaScript injection issues when encoding `.properties` bundles into JavaScript files, or unsafe behavior in `apply-converted-js/i18n.js`
- Unexpected file writes or path traversal derived from locale names, workbook cells, or output directory handling
- Vulnerabilities in Excel workbook parsing or generation (`ExcelConverter`, `SinglePageExcel`) or in bundled and transitive dependencies affecting published `7.0.x` artifacts

### Out of scope

- Unsupported legacy versions (`< 7.0.0`)
- Arbitrary code execution from untrusted build configuration (`<converter>` class names) or from decoding untrusted `.js` files with `JavascriptConverter` (`toMessageProperties=true`), which evaluates input JavaScript through Nashorn by design; treat configuration files and `sourceDir` inputs as trusted build artifacts
- Vulnerabilities purely in upstream third-party libraries or build tools without a demonstrated impact in `SPRING-I18N-CONVERTER`
