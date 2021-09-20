# Purpose of module

To allow modules scattered throughout the classpath to provide bindings to stuff placed in their own code base and have
these modules merged together in a guice injector environment.

# Key classes

## AutoBindModule

The interface to implement if you want to configure your own binding

## AutoBindModules

A Guice module that searches the classpath for AutoBindModules and merges them together. Also enables custom class
scanners to be implemented by extending AbstractClassScanner to find classes matching your own criteria.

## BindInjectAnnotatedImplementations

An example of a module implementing the AutoBindModule interface and making use of the scanner functionality by
searching for 

