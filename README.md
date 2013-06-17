
Keeps Eclipse files up to date
------------------------------

Plugin page: [http://artifacts.griffon-framework.org/plugin/eclipse-support](http://artifacts.griffon-framework.org/plugin/eclipse-support)


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

