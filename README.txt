FF14 MIL Raiding BOT a.k.a. HildiBot
licensed under the Server Side Public License (SSPL)
2021-2024 Mitsuhide Sanada@Phoenix
---

A bot for chat services to plan and manage FF14 raids.
Screenshot: https://puu.sh/JYink/c22471582f.png

---

Discord:
- Public instance:
NOTE: this instance is not (and will not) be verified!

https://discord.com/api/oauth2/authorize?client_id=867707955112968202&permissions=120259151904&scope=bot+applications.commands

- Permissions:

Slash Commands: Required, allows the bot to function.
Write Messages: Required, allows the bot to write messages.
View Message History: required, allows the bot to edit raid listings.

Manage Server: optional, allows to add a numeric notification how many raids will start in the next 24 hours on the lobby icon
Create Public Threads: optional, allows creation of a thread per raid to which the members get automatically added.
Create Private Threads: optional, see above
Manage Threads: optional, allows to automatically un-archive threads if the raid has not happened yet

- Self-hosting:
No privileged intents are required.
Run via: java -jar MILBot.jar
Launch once to generate config file, then edit discord.token and settings.owner.



Command line args:

--config <path>: Use a different config file.
--dbwebui: Force the web API to be enabled.
--debug: Enables debug logs.
--initclasses: Reloads the classes.csv file.
--no-connect: Do not connect to chat service (and just provide the API if enabled).

- API Specific (discord):

--reinitialize: Reloads emoji, image cache and commands for all lobbies.