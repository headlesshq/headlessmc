# HeadlessMc-Web

Using [CheerpJ](https://cheerpj.com/),
a Java runtime written in web assembly,
it is now possible to run HeadlessMc inside the browser.
A demo has been deployed at https://headlesshq.github.io/headlessmc/.
There are some restrictions,
a browser plugin to disable 
[CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
is required and CheerpJ is missing some native functions,
as well as full asm support, so we cannot launch the game, yet.
But we are quite close already,
If we developed a small instrumentation server
that works around the asm issues and could proxy CORS this would be no problem.
We can already launch smaller jars, like the forge and fabric installers.

![Screenshot](browser.png)

To run HeadlessMc inside the browser only minor modifications were needed.
HeadlessMc-Web is launched through the headlessmc-launcher-wrapper, which
calls a custom main function in the [CheerpJ-Plugin](plugin) for HeadlessMc.
This main function creates a JFrame and redirects output and input to it,
so that we have a console. 
It then proceeds to launch the HeadlessMc-Launcher normally.
