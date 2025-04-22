# AngelsSMP Plugin

AngelsSMP is a Minecraft plugin that introduces Angel Books, each with unique abilities that players can use during gameplay. The plugin also includes features like cooldowns, inventory management, and custom GUI interactions.

## Features

- **Angel Books**: Six unique Angel Books (`Zoe`, `Kore`, `Rhea`, `Lilana`, `Vasia`, `Charikleia`) with special abilities.
- **Custom GUI**: Players can select their Angel Book from a custom inventory GUI.
- **Cooldown System**: Abilities have a cooldown period to prevent spamming.
- **Persistent Data**: Player data is stored to ensure Angel Books persist across sessions.
- **Event Handling**: Includes custom behavior for player interactions, inventory clicks, and villager trades.

## Commands

- `/book`: Retrieve your assigned Angel Book.
- `/myangel`: Check which Angel Book is currently assigned to you.
- `/angelgui`: Open the Angel Book selection GUI.
- `/givebook <player> <book>`: (Admin only) Assign a specific Angel Book to a player.

## Permissions
'
- `angelsmp.command.book`: Allows players to use the `/book` command.
- Admin commands like `/givebook` require the player to have operator (`op`) privileges.

## Abilities

- **Zoe**: Grants the player a powerful regeneration and resistance boost for a short duration. Nearby players within 8 blocks are stunned with slowness.
- **Kore**: Launches a fireball in the direction the player is looking. The player also gains a brief regeneration effect, and lava particles are spawned around them.
- **Rhea**: Pulls nearby players (within 15 blocks) toward the user, applying slowness and blindness effects. Totem particles are displayed around the user.
- **Lilana**: Launches the player into the air. After a short delay, nearby players (within 5 blocks) are damaged and knocked back, with an explosion particle effect.
- **Vasia**: Propels the player upward. After a delay, nearby players (within 6 blocks) are slowed significantly, and soul fire flame particles are displayed.
- **Charikleia**: Damages nearby players (within 4 blocks) and propels the user forward based on the number of hits. Electric spark particles are displayed around the user.

## Support

For support, please open an issue on the GitHub repository or contact the plugin developer directly.
Or join the Discord server for community support and discussions.
# https://dsc.gg/smpstore
