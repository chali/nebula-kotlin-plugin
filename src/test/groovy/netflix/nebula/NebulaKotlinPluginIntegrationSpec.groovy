package netflix.nebula


import nebula.test.IntegrationSpec

class NebulaKotlinPluginIntegrationSpec extends IntegrationSpec {
    String kotlinVersion

    def setup() {
        kotlinVersion = NebulaKotlinPlugin.loadKotlinVersion()
        buildFile << """\
        apply plugin: 'nebula.kotlin'

        repositories {
            mavenCentral()
            maven { url 'https://dl.bintray.com/kotlin/kotlin-eap' }
        }
        """.stripIndent()
    }

    def 'plugin applies'() {
        given:
        buildFile.delete()
        buildFile << """
        apply plugin: 'nebula.kotlin'
        """

        when:
        runTasksSuccessfully('help')

        then:
        noExceptionThrown()
    }

    def 'default standard library is added'() {
        given:
        buildFile << """
        sourceCompatibility = JavaVersion.VERSION_1_6
        """

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compileClasspath')

        then:
        result.standardOutput.contains("\\--- org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion\n")
    }

    def 'jdk7 standard library is added when source compatibility is VERSION_1_7'() {
        given:
        buildFile << """
        sourceCompatibility = JavaVersion.VERSION_1_7
        """

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compileClasspath')

        then:
        result.standardOutput.contains("\\--- org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion\n")
    }

    def 'jdk8 standard library is added when source compatibility is VERSION_1_8'() {
        given:
        buildFile << """
        sourceCompatibility = JavaVersion.VERSION_1_8
        """

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compileClasspath')

        then:
        result.standardOutput.contains("\\--- org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion\n")
    }

    def 'kotlin library versions are set if omitted'() {
        given:
        buildFile << """
        dependencies {
            implementation "org.jetbrains.kotlin:kotlin-reflect"
        }
        """

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compileClasspath')

        then:
        result.standardOutput.contains("+--- org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    }

    def 'plugin applied without conflicts with kotlin 1.2.70'() {
        given:
        buildFile.delete()
        buildFile <<  """
        allprojects {
            apply plugin: 'nebula.kotlin'

            repositories {
                mavenCentral()
                maven { url 'https://dl.bintray.com/kotlin/kotlin-eap' }
            }
        }

        """

        addSubproject("sub2", """
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation group: 'com.google.guava', name: 'guava', version: '26.0-jre'
            }
        """)

        addSubproject("sub1", """
            dependencies {
                implementation project(":sub2")
            }

            task resolve() {
                doFirst {
                    configurations.apiDependenciesMetadata.files()
                }
            }
        """)

        when:
        runTasksSuccessfully(':sub1:resolve')

        then:
        noExceptionThrown()
    }

    def 'plugin applies latest kotlin without conflicts while doing dependency lock'() {
        given:
        buildFile.delete()
        buildFile << """
        buildscript {
            repositories {
                mavenCentral()
                maven {
                    url "https://plugins.gradle.org/m2/"
                }
            }
            dependencies {
                classpath "com.netflix.nebula:gradle-dependency-lock-plugin:11.1.2"
            }
        }

        repositories {
            mavenCentral()
            maven { url 'https://dl.bintray.com/kotlin/kotlin-eap' }
        }
        
        allprojects {
            apply plugin: 'nebula.dependency-lock'
        }
        
        subprojects {
            repositories {
                mavenCentral()
                maven { url 'https://dl.bintray.com/kotlin/kotlin-eap' }
            }
            
            apply plugin: 'nebula.kotlin'
            
            kotlin {
                experimental {
                    coroutines 'enable'
                }
            }
        }

        """

        addSubproject("sub2", """
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation group: 'com.google.guava', name: 'guava', version: '26.0-jre'
            }
        """)

        addSubproject("sub1", """
            apply plugin: 'groovy'
            dependencies {
                  implementation project(":sub2")
            }
        """)


        when:
        runTasksSuccessfully('generateLock', 'saveLock')

        then:
        noExceptionThrown()

        and:
        File dependencyLockFile = new File(getProjectDir(), '/sub2/dependencies.lock')
        dependencyLockFile.text.contains("com.google.guava:guava")
        dependencyLockFile.text.contains("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    def 'jdk8 standard library is added to custom configuration'() {
        given:
        buildFile << """
        sourceCompatibility = JavaVersion.VERSION_1_8
        
        configurations {
            myConfig
        }
        
        nebulaKotlin {
            stdlibConfigurations = ["myConfig"]
        }
        """

        when:
        def resultCompileClasspath = runTasksSuccessfully('dependencies', '--configuration', 'compileClasspath')

        then:
        !resultCompileClasspath.standardOutput.contains("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion\n")

        when:
        def resultMyConfig = runTasksSuccessfully('dependencies', '--configuration', 'myConfig')

        then:
        resultMyConfig.standardOutput.contains("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion\n")
    }

    def 'jdk8 standard library is added to custom configuration - deprecated extension property'() {
        given:
        buildFile << """
        sourceCompatibility = JavaVersion.VERSION_1_8
        
        configurations {
            myConfig
        }
        
        nebulaKotlin {
            stdlibConfiguration = "myConfig"
        }
        """

        when:
        def resultCompileClasspath = runTasksSuccessfully('dependencies', '--configuration', 'compileClasspath')

        then:
        !resultCompileClasspath.standardOutput.contains("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion\n")

        when:
        def resultMyConfig = runTasksSuccessfully('dependencies', '--configuration', 'myConfig')

        then:
        resultMyConfig.standardOutput.contains("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion\n")
    }
}
