# TaleName ServerLink

Link your Hytale server to [TaleName.net](https://talename.net).

## Installation

1. Download `TaleName-ServerLink.jar` from releases
2. Place in your server's `plugins` folder
3. Restart your server

## Linking Your Server

1. Go to [talename.net/settings](https://talename.net/settings) -> Servers -> Add Server
2. Get your 6-character link code
3. Run `/talename link <code>` on your server

## Commands

All commands require **operator permission**.

| Command | Description |
|---------|-------------|
| `/talename link <code>` | Link server to TaleName |
| `/talename unlink` | Unlink server |
| `/talename status` | Check link status |

## Building

```bash
./gradlew build
```

## License

MIT License
