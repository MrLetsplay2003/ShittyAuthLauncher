# ShittyAuthLauncher
A Minecraft launcher that uses a shitty authentication server

For the authentication server, go [here](https://github.com/MrLetsplay2003/ShittyAuthServer)

ShittyAuthLauncher is a Minecraft launcher that's not tied to any specific authentication server. It lets you fully configure which auth/session/api server you want to use, and will automatically patch the Mojang-provided authlib to use the configured auth server.

# Installing
Just download the latest launcher JAR file from the [releases](https://github.com/MrLetsplay2003/ShittyAuthLauncher/releases) section. You can then run it using any Java 11+ VM on Linux or Windows.

# Features
- Automatically patches Mojang's authlib as well as the Minecraft JAR
- Configurable auth/session/api/services servers
- Supports custom skin hosts
- Supports authentication and skins in modern versions of Minecraft as well as older ones (as low as beta 1.8 when using [ShittyAuthServer](https://github.com/MrLetsplay2003/ShittyAuthServer))
- Works completely independently of the normal Minecraft launcher

# Compiling the launcher yourself
The launcher uses Maven for building.

To compile the launcher, use:
```
$ mvn package
```
which will generate a `ShittyAuthLauncher-VERSION.jar` in the `target` folder
