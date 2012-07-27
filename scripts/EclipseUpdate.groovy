/*
 * Copyright 2010-2012 the original author or authors.
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
        classpathentry(kind: 'output', path: 'bin')

        def normalizeFilePath = { file ->
            String path = file.canonicalPath
            String originalPath = path
            path = path.replaceFirst(~/$griffonHomeRegex/, 'GRIFFON_HOME')
            path = path.replaceFirst(~/$userHomeRegex/, 'USER_HOME')
            path = normalizePath(path)
            boolean var = path.startsWith('USER_HOME') || path.startsWith('GRIFFON_HOME')
            originalPath = path
            path = path.replaceFirst(~/${baseDirPath}(\\|\/)/, '')
            var = path == originalPath && !path.startsWith(File.separator)
            [kind: var? 'var' : 'lib', path: path]
        }
        def visitDependencies = { List dependencies ->
            dependencies.each { File f ->
                if(visitedDependencies.contains(f)) return
                visitedDependencies << f
                Map pathEntry = normalizeFilePath(f)
                classpathentry(kind: pathEntry.kind, path: pathEntry.path)
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
