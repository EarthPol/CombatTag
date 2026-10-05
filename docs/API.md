# API

Import `com.earthpol.combattag.api.CombatTagApi`. See [CombatTagApi.java](../src/main/java/com/earthpol/combattag/api/CombatTagApi.java) for every method.

After [building CombatTag](../README.md#build), add this dependency:

```xml
<dependency>
  <groupId>com.earthpol</groupId>
  <artifactId>CombatTag</artifactId>
  <version>2.3.0</version>
  <scope>provided</scope>
</dependency>
```

Add `depend: [CombatTag]` to `plugin.yml`, then get the API where you need it:

```java
CombatTagApi api = Bukkit.getServicesManager().load(CombatTagApi.class);
if (api == null) return;

// Example calls
boolean tagged = api.isTagged(player);
long remaining = api.getRemainingMillis(player);
api.applyTag(player);
api.removeTag(player);
```

UUID overloads `isTagged` and `getRemainingMillis` are safe from any thread. Player overloads and mutations run on the owning player thread: the Paper main thread or the player's thread on Folia.
