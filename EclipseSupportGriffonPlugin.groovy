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
class EclipseSupportGriffonPlugin {
    // the plugin version
    String version = '0.5'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '0.9.5 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [:]
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-eclipse-support-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Keeps Eclipse files up to date'
    String description = '''
Provides better tooling integration with Eclipse. At the moment there is a single
script that keeps Eclipse's classpath file up to date.

Usage
-----
This plugin provides a single script which is automatically called whenever a plugin
is installed or uninstalled. If you add/remove a library from $appHome/lib then you
must manually run the script.

Configuration
-------------
Make sure to define the following classpath variables in your Eclipse environment

 * **USER_HOME** pointing to `$USER_HOME` (your user's home directory)
 * **GRIFFON_HOME** pointing to `$GRIFFON_HOME` (the location where Griffon is installed)

otherwise Eclipse will complain that the jars cannot be located.

All source directories available under `griffon-app` and `src` will be automatically
configured by the plugin. However, should you require additional source directories
to be included you can do so by specifying a list of directories relative to the
application's location. Add this settings to either `BuildConfig.groovy` or `settings.groovy`

        eclipse.classpath.include = ['gen-src', '../app2/src']

Scripts
-------
 * **eclipse-update** - re-generates the contents of the .classpath file.
'''
}
