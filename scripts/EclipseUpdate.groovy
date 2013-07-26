/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */

import org.codehaus.griffon.artifacts.model.Plugin
import static griffon.util.GriffonApplicationUtils.is64Bit
import static griffon.util.GriffonApplicationUtils.isWindows

import groovy.xml.MarkupBuilder

target(updateEclipseClasspath: "Update the application's Eclipse classpath file") {
    updateEclipseClasspathFile()
}
setDefaultTarget('updateEclipseClasspath')

normalizePath = { path ->
    if (isWindows) {
        path = path.replace('\\\\', '\\')
        path = path.replace('\\', '\\\\')
    }
    path
}

updateEclipseClasspathFile = { newPlugin = null ->
    println "Updating Eclipse classpath file..."

    def visitedDependencies = []

    String userHomeRegex    = normalizePath(userHome.toString())
    String griffonHomeRegex = normalizePath(griffonHome.toString())
    String baseDirPath      = normalizePath(griffonSettings.baseDir.path)

    String indent = '    '
    def writer = new PrintWriter(new FileWriter("${baseDirPath}/.classpath"))
    def xml = new MarkupBuilder(new IndentPrinter(writer, indent))
    xml.setDoubleQuotes(true)
    xml.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')
    xml.mkp.comment("Auto generated on ${new Date()}")
    xml.mkp.yieldUnescaped '\n'
    xml.classpath {
        mkp.yieldUnescaped("\n${indent}<!-- source paths -->")
        ['griffon-app', 'src', 'test'].each { base ->
            new File(baseDirPath, base).eachDir { dir ->
                if (! (dir.name =~ /^\..+/) && dir.name != 'templates') {
                    classpathentry(kind: 'src', path: "${base}/${dir.name}")
                }
            }
        }
        buildConfig.eclipse?.classpath?.include?.each { dir ->
            File target = new File(baseDirPath, dir)
            dir = normalizePath(dir)
            if(target.exists()) classpathentry(kind: 'src', path: dir)
        }

        mkp.yieldUnescaped("\n${indent}<!-- output paths -->")
        classpathentry(kind: 'con', path: 'org.eclipse.jdt.launching.JRE_CONTAINER')
        classpathentry(exported: true, kind: 'con', path: 'GROOVY_DSL_SUPPORT')
        classpathentry(kind: 'output', path: "USER_HOME${classesDirPath.substring(userHomeRegex.size())}")

        def normalizeFilePath = { file ->
            String path = file.canonicalPath
            String originalPath = path
            if (path.startsWith(griffonHomeRegex)) path = 'GRIFFON_HOME' + path.substring(griffonHomeRegex.size())
            if (path.startsWith(userHomeRegex)) path = 'USER_HOME' + path.substring(userHomeRegex.size())
            path = normalizePath(path)
            boolean var = path.startsWith('USER_HOME') || path.startsWith('GRIFFON_HOME')
            originalPath = path
            if (path.startsWith(baseDirPath + '/')) path = path.substring(baseDirPath.size() + 1)
            if (isWindows && path.startsWith(baseDirPath + '\\')) path = path.substring((baseDirPath + '\\').size())
            var = path == originalPath && !path.startsWith(File.separator)
            [kind: var? 'var' : 'lib', path: path]
        }

        def extractPluginName = { str, qualifier ->
            def m = str =~ /griffon-(.+)-${qualifier}-.*/
            m[0][1]
        }

        def visitDependencies = { List dependencies ->
            dependencies.each { File f ->
                if(visitedDependencies.contains(f)) return
                visitedDependencies << f
                Map pathEntry = normalizeFilePath(f)
                if (pathEntry.path.contains('griffon-rt')) {
                    classpathentry(kind: pathEntry.kind, path: pathEntry.path,
                                   sourcepath: 'GRIFFON_HOME/doc/' + f.name.replace('.jar', '-sources.jar')) {
                        attributes {
                            attribute(name: 'javadoc_location', value: 'http://griffon.codehaus.org/guide/latest/api/')
                        }
                    }
                } else if (pathEntry.path.contains('griffon-cli')) {
                    classpathentry(kind: pathEntry.kind, path: pathEntry.path,
                                   sourcepath: 'GRIFFON_HOME/doc/' + f.name.replace('.jar', '-sources.jar'))
                } else if (f.name.startsWith('griffon-')) {
                    if (f.name.contains('-runtime-') || f.name.contains('-compile-')) {
                       String qualifier = f.name.contains('runtime') ? 'runtime' : 'compile'
                       String pluginName = extractPluginName(f.name, qualifier)
                       def pluginEntry = pluginSettings.projectPlugins.find { it.key == pluginName }
                       if (pluginEntry) {
                            File sourceFile = new File("${pluginEntry.value.directory.file}/docs/${f.name.replace('.jar', '-sources.jar')}")
                            if (sourceFile.exists()) {
                                classpathentry(kind: pathEntry.kind, path: pathEntry.path,
                                              sourcepath: normalizeFilePath(sourceFile).path)
                            } else {
                                classpathentry(kind: pathEntry.kind, path: pathEntry.path)
                            }
                        } else {
                            classpathentry(kind: pathEntry.kind, path: pathEntry.path)
                        }
                    } else {
                        classpathentry(kind: pathEntry.kind, path: pathEntry.path)
                    }
                } else {
                    classpathentry(kind: pathEntry.kind, path: pathEntry.path)
                }
            }
        }

        mkp.yieldUnescaped("\n${indent}<!-- runtime -->")
        visitDependencies(griffonSettings.runtimeDependencies)
        mkp.yieldUnescaped("\n${indent}<!-- test -->")
        visitDependencies(griffonSettings.testDependencies)
        mkp.yieldUnescaped("\n${indent}<!-- compile -->")
        visitDependencies(griffonSettings.compileDependencies)
        mkp.yieldUnescaped("\n${indent}<!-- build -->")
        visitDependencies(griffonSettings.buildDependencies)

        def visitPlatformDir = { libdir ->
            def nativeLibDir = new File("${libdir}/${platform}")
            if(nativeLibDir.exists()) {
                nativeLibDir.eachFileMatch(~/.*\.jar/) { file ->
                    Map pathEntry = normalizeFilePath(file)
                    classpathentry(kind: pathEntry.kind, path: pathEntry.path)
                }
            }
            nativeLibDir = new File("${libdir}/${platform[0..-3]}")
            if(is64Bit && nativeLibDir.exists()) {
                nativeLibDir.eachFileMatch(~/.*\.jar/) { file ->
                    Map pathEntry = normalizeFilePath(file)
                    classpathentry(kind: pathEntry.kind, path: pathEntry.path)
                }
            }
        }

        mkp.yieldUnescaped("\n${indent}<!-- platform specific -->")
        visitPlatformDir(new File("${baseDirPath}/lib"))

        pluginSettings.doWithProjectPlugins { pluginName, pluginVersion, pluginDir ->
            if("${pluginName}-${pluginVersion}" == newPlugin) return
            def libDir = new File(pluginDir, 'lib')
            visitPlatformDir(libDir)
        }
        pluginSettings.doWithFrameworkPlugins { pluginName, pluginVersion, pluginDir ->
            if("${pluginName}-${pluginVersion}" == newPlugin) return
            def libDir = new File(pluginDir, 'lib')
            visitPlatformDir(libDir)
        }
        if(newPlugin) {
            def libDir = new File([artifactSettings.artifactBase(Plugin.TYPE), newPlugin, 'lib'].join(File.separator))
            visitPlatformDir(libDir)
        }
    }
}
