# NVBan Mod README

## Overview
NVBan is a Minecraft mod that prevents players from switching accounts to bypass bans by binding player accounts to hardware identifiers.

## Commands

### `/naban unbind <player>`
- **Permission Level**: 3
- **Function**: Unbinds a player's account from their hardware ID, allowing them to log in from a different device
- **Usage**: `/naban unbind Steve` - Removes the device binding for player "Steve"

### `/naban list`
- **Permission Level**: 3
- **Function**: Lists all current player-to-hardware ID bindings
- **Usage**: `/naban list` - Shows all bound player-hardware relationships

### `/naban info <player>`
- **Permission Level**: 3
- **Function**: Displays the hardware ID that is bound to a specific player
- **Usage**: `/naban info Steve` - Shows the hardware ID bound to player "Steve"

### `/naban ban <hardwareId>`
- **Permission Level**: 3
- **Function**: Bans a specific hardware ID, preventing any account bound to it from logging in
- **Usage**: `/naban ban AABBCCDDEEFF` - Bans the specified hardware ID
- **Note**: You can get hardware IDs using `/naban list` or `/naban info <player>`

## How It Works
1. When a player logs in for the first time, their account is bound to their device's hardware ID
2. If they try to log in from a different device, they will be disconnected
3. If another account tries to log in from a device that's already bound to a different player, they will be disconnected
4. Admins can manage bindings and ban specific hardware IDs using the provided commands

## Permission Requirements
All commands require permission level 3 (administrator level) to execute.
