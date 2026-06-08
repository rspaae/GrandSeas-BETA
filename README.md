<div align="center">
  <h1>🏝️ GrandSeas</h1>
  <p><i>The ultimate acid ocean survival plugin for PaperMC! Build islands, manage teams, and dominate the toxic seas.</i></p>
  
  [![GitHub License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
  [![PaperMC](https://img.shields.io/badge/Platform-PaperMC-black?style=flat&logo=paper)](https://papermc.io/)
</div>

<br>

**GrandSeas** is a modern, feature-rich SkyBlock and AcidIsland alternative. It brings a completely fresh experience to island survival servers by replacing the void with a toxic, acidic ocean. 

Players must expand their island, manage resources, form teams, and deposit valuable blocks into a centralized Point Storage system to rank up and upgrade their island.

---

## ✨ Features

- ☠️ **Acidic Oceans:** The water surrounding the islands is toxic! Players and mobs take damage when touching the water or being out in the acid rain (fully configurable).
- 📦 **Point Storage System:** Instead of placing valuable blocks manually around the island to gain levels, players can deposit blocks into a centralized Point Storage GUI.
- 📈 **Dynamic Island Upgrades:** Players can spend their Island Bank balance to upgrade their Island Border Size and Ore Generator rates.
- 👥 **Advanced Team & Trust Management:** Complete control over island access. Add members, promote leaders, or give specific players a `Trusted` status to build without joining the team.
- 💰 **Island Economy:** Built-in island bank system. Deposit, withdraw, and manage island funds directly through the GUI.
- 🛡️ **Robust Protection:** Extensive island protection preventing visitors from griefing, opening chests, or interacting with the environment.
- 🎨 **Modern GUI Menus:** Almost every feature is accessible through beautiful, interactive menus—no more remembering complex commands!

---

## 📸 Screenshots & Menus

- **Main Menu (`/is`)** - Your central hub for island management.
- **Control Panel (`/is settings`)** - Toggle settings like Visitor Access, PvP, and Island Chat.
- **Upgrades Menu (`/is upgrades`)** - Upgrade your border size and generator using your Island Bank balance.
- **Point Storage (`/is level`)** - Deposit blocks like Diamonds, Emeralds, and Spawners to increase your island's level!
- **Leaderboard (`/is top`)** - Compete with other islands to have the highest level!

---

## 📜 Commands

### Player Commands
All player commands use the base command `/is` or `/island`.

| Command | Description |
| :--- | :--- |
| `/is` | Open the Main Menu or teleport to your island. |
| `/is create` | Create a new island. |
| `/is settings` | Open the Island Control Panel. |
| `/is upgrades` | Open the Upgrades Menu. |
| `/is balance` | View your island's bank balance. |
| `/is level` | View your island's points & open Point Storage. |
| `/is top` | View the top islands leaderboard. |
| `/is visit <player>` | Visit another player's island. |
| `/is trust <player>` | Grant build access to a non-team member. |
| `/is ban <player>` | Ban a player from your island. |
| `/is team invite <player>` | Invite a player to join your island team. |
| **And much more...** | Use `/is help` in-game for the full list! |

### Admin Commands
Admin commands require the `grandseas.admin` permission.

| Command | Description |
| :--- | :--- |
| `/isadmin tp <player>` | Force teleport to any player's island. |
| `/isadmin info <player>` | View detailed stats of a player's island. |
| `/isadmin delete <player>` | Forcibly delete a player's island. |
| `/isadmin setpoints <player> <amount>` | Manually set an island's point level. |
| `/isadmin eco <give/take/set> <p> <amount>` | Manage an island's bank balance. |
| `/isadmin reload` | Reload the plugin configuration. |

---

## ⚙️ Configuration
GrandSeas is highly customizable. Upon first load, the plugin will generate a `config.yml` and `messages.yml` file. 

You can configure:
- Acid damage rates for players, mobs, and items.
- Points values for every block type in the Point Storage.
- Upgrade costs for borders and generators.
- Starter chest items.
- Maximum team sizes and trusted limits.

---

## 💻 Compilation & Installation

1. Download the latest `.jar` file from the [Releases](#) tab (or compile it yourself using `mvn clean package`).
2. Drop the `GrandSeas-BETA.jar` into your server's `plugins/` folder.
3. Restart your server.
4. Enjoy the acidic seas!

---

<div align="center">
  <i>Developed with ❤️ for the PaperMC community.</i>
</div>
