A BeepBeep toolbox to manipulate sensor events
==============================================

This repository contains an extension to the [BeepBeep](https://liflab.github.io/beepbeep-3) event stream processing engine for the manipulation of sensor events produced by smart home
platforms such as [NEARS](https://domus.recherche.usherbrooke.ca/nears/)
and [CASAS](https://casas.wsu.edu/datasets/).

To learn more about BeepBeep, a [presentation](https://www.slideshare.net/sylvainhalle/event-stream-processing-with-beepbeep-3-258079731) is available, as well as a complete (and free) [textbook](https://www.puq.ca/catalogue/livres/event-stream-processing-with-beep-beep-3663.html).

![An example of a BeepBeep pipeline](https://github.com/sylvainhalle/beepbeep-sensor-events/blob/main/Source/src/doc-files/ContactLifecycle.png?raw=true)

Using the extension
-------------------

You can use the toolbox by simply downloading the latest precompiled [release](https://github.com/sylvainhalle/beepbeep-sensor-events/releases). Otherwise you can build the extension directly from the sources (instructions at the end).

The library is stand-alone: in addition to the new processors and functions it defines, it bundles BeepBeep and all the [palettes](https://github.com/liflab/beepbeep-3-palettes) it needs to operate. No other JAR file is needed.

### In Java

To use the toolbox in a Java project, simply add the library to the Java classpath or as a dependency in your IDE.

### In Groovy

To use the toolbox in [Groovy](https://groovy-lang.org), you also need to add the library to the classpath. This can be done in two ways:

1. By specifying the classpath directly when running the script. For example, to run the script `myscript.gvy`, one would write:

```bash
groovy -cp xxx.jar myscript.gvy
```

where `xxx.jar` should be replaced by the name and location of the toolbox JAR file.

2. By setting the `CLASSPATH` environment variable and specifying the location of the toolbox JAR file, e.g. in Bash:

```bash
export CLASSPATH=/path/to/xxx.jar
```

Once done the `-cp` option is no longer necessary when running the scripts.

A few examples of Groovy scripts using the toolbox are available in the project's root folder.

Groovy shortcuts
----------------

A few "shortcuts" are available when using the toolbox in Groovy scripts. Importing the `sensors.Shortcuts` package at the start of a script avoids importing many BeepBeep packages individually (see the Javadoc for details). More information can be found in the BeepBeep [slideshow](https://www.slideshare.net/sylvainhalle/event-stream-processing-with-beepbeep-3-258079731) (slide 347 onwards).

Code examples
-------------

The library comes with code examples that showcase the operations that can be done on sensor logs. Explanations and illustrations of the corresponding BeepBeep pipelines can be accessed in the Javadoc, by looking for the `sensors.examples` package.

Building the extension
----------------------

The repository is structured as an [AntRun](https://github.com/sylvainhalle/AntRun) Java project, which comes with a command line build script that can automatically download dependencies, compile, test and bundle the stand-alone library as a JAR file.

To compile the extension, make sure you have the following:

- The Java Development Kit (JDK) to compile. The palette complies
  with Java version 8; it is probably safe to use any later version.
- [Ant](http://ant.apache.org) to automate the compilation and build process

At the command line, in the project's root folder, simply typing:

    ant

should take care of downloading all dependencies and compiling the project. The result is a set of three JAR files:

- `beepbeep-sensor-toolbox-x.x.jar`: the library itself
- `beepbeep-sensor-toolbox-x.x-sources.jar`: the source code
- `beepbeep-sensor-toolbox-x.x-javadoc.jar`: the Javadoc documentation; unzip in a folder and open `index.html` to view the document (yes, there is documentation for all the classes in the library)

<!-- :wrap=soft:maxLineLen=76: -->
