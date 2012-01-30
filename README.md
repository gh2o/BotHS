BotHS
=====
BotHS is an embedded web server tailored towards FRC robotics teams.

Features
--------
* Basic HTML templating system.
* MTV (model-template-view) paradigm.
* WebSocket support: stream data straight to the browser!

To Get Started
--------------
After downloading the sources via git or the ZIP file above,

* Copy everything in the `src` folder into the `src` folder in your project
* Copy everything in the `resources` folder into the `resources` folder in your project

Add this somewhere in your code (preferably the constructor of your main class):

	Server server = new Server ();
	server.addRoute ("/", new TemplateView ("/index.html"));
	server.start ();

Add these lines to the import statements of that file:

	import org.team4159.boths.Server;
	import org.team4159.boths.views.TemplateView;

In your `resources` folder, create a file named `index.html`:

	<!DOCTYPE html>
	<html>
		<head>
			<title>Hello from BotHS!</title>
		</head>
		<body>
			<h1>BotHS!!!</h1>
		</body>
	</html>

Build, deploy, and go to the address of your cRIO on port 8080
(http://10.56.78.2:8080/ if you are team 5678) and if all went
well, you should see your page! It was really that simple.

This was made with the intention of allowing the cRIO device to
access the rich potential of the web browser. With BotHS, you can access and
control your robot with any web-capable device during testing.
The possibilities are endless!
