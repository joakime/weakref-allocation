WeakReference Allocation Tool
=============================

When you have excessive garbage collection times due to large 
weak reference counts, it is often hard to determine where these
weak references come from in your environment.

This is a modification of the java.lang.ref.WeakReference implementation
that will track and report what it finds via a JMX MBean.

How to use:
-----------

Enable JMX in your application.

Then start your java application with a bootclasspath that points to this implementation.

eg:

    $ java -Xbootclasspath/p:./weakref-allocation-1.0.0.jar -jar myapp.jar

Now check your JMX console at java.lang.ref for the information being collected.


How to use with Jetty 9:
------------------------

    # Use a ${jetty.base}
    [user]$ cd mybase

    # Copy the weakref jar into place
    [mybase]$ mkdir -p lib/boot
    [mybase]$ cp /path/to/weakref-allocation-1.0.0.jar lib/boot/
    
    # Enable JMX
    [mybase]$ echo "--module=jmx" >> start.ini

    # Force a forked exec with bootclasspath use of this jar
    [mybase]$ echo "--exec" >> start.ini
    [mybase]$ echo "-Xbootclasspath/p:lib/boot/weakref-allocation-1.0.0.jar" >> start.ini

